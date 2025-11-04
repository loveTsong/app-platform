/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.biz.handlers;

import static modelengine.fit.waterflow.common.Constant.BUSINESS_DATA_KEY;
import static modelengine.fit.waterflow.common.Constant.CONTEXT_DATA;
import static modelengine.fit.waterflow.common.Constant.FLOWS_EVENT_HANDLER_EXECUTOR;
import static modelengine.fit.waterflow.common.Constant.PASS_DATA;
import static modelengine.fit.waterflow.spi.FlowExceptionService.HANDLE_EXCEPTION_GENERICABLE;

import modelengine.fit.waterflow.biz.operation.OperatorFactory;
import modelengine.fit.waterflow.biz.operation.operator.Operator;
import modelengine.fit.waterflow.common.Constant;
import modelengine.fit.waterflow.flowsengine.biz.service.cache.FlowDefinitionQueryService;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowcontext.FlowContextPersistRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.FlowDefinition;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.FlowNode;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.converter.FlowDataConverter;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.tasks.FlowTask;
import modelengine.fit.waterflow.flowsengine.domain.flows.events.FlowTaskCreatedEvent;
import modelengine.fit.waterflow.flowsengine.domain.flows.utils.FlowExecuteInfoUtil;
import modelengine.fit.waterflow.flowsengine.utils.FlowUtil;
import modelengine.fit.waterflow.spi.FlowExceptionService;
import modelengine.fitframework.annotation.Asynchronous;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.broker.client.BrokerClient;
import modelengine.fitframework.broker.client.filter.route.FitableIdFilter;
import modelengine.fitframework.event.EventHandler;
import modelengine.fitframework.exception.FitException;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 手动任务事件处理类
 *
 * @author 晏钰坤
 * @since 2023/9/14
 */
@Component
public class FlowTaskCreatedEventHandler implements EventHandler<FlowTaskCreatedEvent> {
    private static final Logger log = Logger.get(FlowTaskCreatedEventHandler.class);
    private static final String TASK_EXECUTE_INFO_TYPE = "task";

    private final FlowContextPersistRepo flowContextPersistRepo;
    private final FlowDefinitionQueryService definitionQueryService;
    private final BrokerClient brokerClient;

    public FlowTaskCreatedEventHandler(FlowContextPersistRepo flowContextPersistRepo,
                                       FlowDefinitionQueryService definitionQueryService, BrokerClient brokerClient) {
        this.flowContextPersistRepo = flowContextPersistRepo;
        this.definitionQueryService = definitionQueryService;
        this.brokerClient = brokerClient;
    }

    /**
     * 处理流程引擎发布的事件，创建人工任务
     * 流程引擎发布的事件中，包含一个批次的contexts ID，该批次是用户配置自定义过滤器或者默认过滤器产生的
     * 这一批次的contexts只生成一个人工任务，任务人工必须能够承载这批次contexts中的业务数据
     * 这一批次的contexts产生的单个人工任务，每个context相当于任务中的一行数据，这行数据要与context ID做1:1关联
     * 流程恢复执行时，由业务发送一批更新过的context ID以及对应的属性变化值触发流程恢复执行
     *
     * @param eventData 流程引擎发布的事件数据
     */
    @Override
    @Asynchronous(executor = FLOWS_EVENT_HANDLER_EXECUTOR)
    public void handleEvent(FlowTaskCreatedEvent eventData) {
        log.info("[FlowTaskCreatedEventHandler]: start to handle event.");
        List<String> contextIds = eventData.getFlowContextId();
        List<FlowContext<FlowData>> contexts = flowContextPersistRepo.getByIds(contextIds);

        String streamId = eventData.getStreamId();
        FlowDefinition flowDefinition = definitionQueryService.findByStreamId(streamId);
        FlowNode flowNode = flowDefinition.getFlowNode(eventData.getNodeId());
        FlowTask task = flowNode.getTask();
        String type = task.getTaskType().getSource();
        List<FlowContext<FlowData>> convertedContexts = null;
        try {
            convertedContexts = this.convertFlowContext(contexts, task, flowNode);
            flowContextPersistRepo.save(convertedContexts);
            Operator operator = OperatorFactory.getOperator(type, brokerClient);
            operator.operate(convertedContexts, task);
        } catch (FitException e) {
            for (String fitableId : task.getExceptionFitables()) {
                this.brokerClient.getRouter(FlowExceptionService.class, HANDLE_EXCEPTION_GENERICABLE)
                        .route(new FitableIdFilter(fitableId))
                        .invoke(eventData.getNodeId(), getFlowData(ObjectUtils.nullIf(convertedContexts, contexts)), e.getMessage());
            }
            log.error("Caught a throwable during the task handling. TaskId is {}. Caused by {}", task.getTaskId(),
                    e.getMessage());
        }
    }

    private List<FlowContext<FlowData>> convertFlowContext(List<FlowContext<FlowData>> contexts, FlowTask task,
                                                           FlowNode node) {
        FlowDataConverter taskConverter = task.getConverter();
        if (Objects.isNull(taskConverter)) {
            return contexts;
        }
        contexts.forEach(context -> {
            updateContextData(node, context);
            FlowData flowData = context.getData();
            Map<String, Object> newInputMap = taskConverter.convertInput(flowData.getBusinessData());
            Optional.of(flowData)
                    .map(FlowData::getContextData)
                    .map(contextData -> contextData.get(Constant.NODE_META_ID))
                    .map(Object::toString)
                    .ifPresent(nodeMetaId -> FlowExecuteInfoUtil.addInputMap2ExecuteInfoMap(flowData, newInputMap,
                            nodeMetaId, TASK_EXECUTE_INFO_TYPE));
            flowData.setBusinessData(FlowUtil.mergeMaps(flowData.getBusinessData(), newInputMap));
        });
        return contexts;
    }

    private void updateContextData(FlowNode node, FlowContext<FlowData> context) {
        Map<String, Object> contextMetaData = context.getData().getContextData();
        contextMetaData.put(Constant.NODE_META_ID, node.getMetaId());
        contextMetaData.put(Constant.NODE_TYPE, node.getType().getCode());
    }

    private List<Map<String, Object>> getFlowData(List<FlowContext<FlowData>> flowContexts) {
        return flowContexts.stream().map(cxt -> new HashMap<String, Object>() {
            {
                put(BUSINESS_DATA_KEY, cxt.getData().getBusinessData());
                put(CONTEXT_DATA, cxt.getData().getContextData());
                put(PASS_DATA, cxt.getData().getPassData());
            }
        }).collect(Collectors.toList());
    }
}
