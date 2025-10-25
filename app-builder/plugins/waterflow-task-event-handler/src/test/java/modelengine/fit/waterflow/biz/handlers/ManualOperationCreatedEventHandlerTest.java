/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.biz.handlers;

import static modelengine.fit.waterflow.biz.operation.OperatorFactory.getOperator;
import static modelengine.fit.waterflow.domain.enums.FlowNodeStatus.ARCHIVED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson.JSON;

import modelengine.fit.jober.FlowSmartFormService;
import modelengine.fit.jober.common.ErrorCodes;
import modelengine.fit.jober.common.exceptions.JobberException;
import modelengine.fit.waterflow.biz.operation.OperatorFactory;
import modelengine.fit.waterflow.biz.operation.operator.SmartFormOperator;
import modelengine.fit.waterflow.common.Constant;
import modelengine.fit.waterflow.flowsengine.biz.service.TraceOwnerService;
import modelengine.fit.waterflow.flowsengine.biz.service.cache.FlowDefinitionQueryService;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowcontext.FlowContextPersistRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowretry.FlowRetryRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowtrace.FlowTraceRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.FlowDefinition;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.FlowNode;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.FlowStateNode;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.converter.MappingFlowDataConverter;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.converter.MappingFromType;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.converter.MappingNode;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.converter.MappingNodeType;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.tasks.FlowTask;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.repo.FlowDefinitionRepo;
import modelengine.fit.waterflow.domain.enums.FlowNodeType;
import modelengine.fit.waterflow.flowsengine.domain.flows.enums.FlowTaskType;
import modelengine.fit.waterflow.flowsengine.domain.flows.events.FlowTaskCreatedEvent;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowContextMapper;
import modelengine.fit.waterflow.flowsengine.persist.po.FlowContextPO;
import modelengine.fitframework.broker.client.BrokerClient;
import modelengine.fitframework.broker.client.Invoker;
import modelengine.fitframework.broker.client.Router;
import modelengine.fitframework.util.ObjectUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link FlowTaskCreatedEventHandler} 测试类
 *
 * @author 晏钰坤
 * @since 2023/9/18
 */
class ManualOperationCreatedEventHandlerTest {
    private static final String HANDLE_SMART_FORM_GENERICABLE = "htctmizg0mydwnt2ttbbp8jlgo2e9e0w";

    private FlowDefinition generateFlowDefinition(FlowTaskType taskType) {
        Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("title", "管理员审批");
        propertiesMap.put("created_by", "{{creator}}");
        propertiesMap.put("owner", "{{owner1}}");
        List<MappingNode> inputMappingConfig = new ArrayList<>(Arrays.asList(
                new MappingNode("taskKey1", MappingNodeType.STRING, MappingFromType.REFERENCE, Arrays.asList("statue"),
                        ""), new MappingNode("int", MappingNodeType.INTEGER, MappingFromType.INPUT, 666, "")));
        MappingFlowDataConverter flowDataConverter = new MappingFlowDataConverter(inputMappingConfig, null);

        FlowTask task = new FlowTask();
        task.setConverter(flowDataConverter);
        task.setTaskId("taskId");
        task.setTaskType(taskType);
        task.setExceptionFitables(Collections.singleton("exceptionFitable"));
        task.setProperties(propertiesMap);
        task.setNodeId("nodeId");
        FlowNode flowNode = new FlowStateNode();
        flowNode.setTask(task);
        flowNode.setMetaId("metaId");
        flowNode.setType(FlowNodeType.STATE);
        Map<String, FlowNode> nodeMap = new HashMap<>();
        nodeMap.put("nodeId", flowNode);
        return FlowDefinition.builder().nodeMap(nodeMap).build();
    }

    private List<FlowContextPO> generatePO() {
        HashMap<String, Object> businessData = new HashMap<>();
        businessData.put("description", "描述信息");
        businessData.put("third_party_data", "第三方系统数据");
        businessData.put("creator", "yyk");
        businessData.put("owner1", "yyk_owner");
        businessData.put("priority", "urgent");
        businessData.put("statue", "approved");
        businessData.put("application", "framework");
        FlowData flowData = FlowData.builder()
                .operator("yyk")
                .startTime(LocalDateTime.now())
                .businessData(businessData)
                .contextData(new HashMap<>())
                .build();
        FlowContextPO contextPO = FlowContextPO.builder()
                .contextId("flowContextId")
                .flowData(JSON.toJSONString(flowData))
                .traceId("traceId")
                .status(String.valueOf(ARCHIVED))
                .positionId("eventId")
                .build();
        return Collections.singletonList(contextPO);
    }

    @Nested
    @DisplayName("测试手动执行任务事件的处理")
    class TestManualOperationEventHandler {
        private FlowTaskCreatedEventHandler handler;

        private FlowContextMapper flowContextMapper;

        private FlowTraceRepo flowTraceRepo;

        private FlowRetryRepo flowRetryRepo;

        private FlowDefinitionRepo flowDefinitionRepo;

        private BrokerClient brokerClient;

        private TraceOwnerService traceOwnerService;

        private FlowDefinitionQueryService definitionQueryService;

        @BeforeEach
        void setUp() {
            flowContextMapper = Mockito.mock(FlowContextMapper.class);
            flowTraceRepo = Mockito.mock(FlowTraceRepo.class);
            flowRetryRepo = Mockito.mock(FlowRetryRepo.class);
            traceOwnerService = Mockito.mock(TraceOwnerService.class);
            Integer defaultLimitation = 5;
            boolean useLimit = false;
            FlowContextPersistRepo flowContextPersistRepo = new FlowContextPersistRepo(flowContextMapper, flowTraceRepo,
                    flowRetryRepo, traceOwnerService, defaultLimitation, useLimit, 1);
            flowDefinitionRepo = Mockito.mock(FlowDefinitionRepo.class);
            definitionQueryService = Mockito.mock(FlowDefinitionQueryService.class);
            brokerClient = Mockito.mock(BrokerClient.class);
            handler = new FlowTaskCreatedEventHandler(flowContextPersistRepo, definitionQueryService, brokerClient);
        }

        @Test
        @DisplayName("获取智能表单任务事件创建任务实例成功")
        void givenSmartFormManualOperationEventThenHandleSuccessfully() throws Throwable {
            FlowTaskCreatedEvent event = new FlowTaskCreatedEvent(Collections.singletonList("flowContextId"),
                    "metaId-version", "nodeId", "publisher");
            FlowDefinition definition = generateFlowDefinition(FlowTaskType.AIPP_SMART_FORM);
            when(flowContextMapper.findByContextIdList(any())).thenReturn(generatePO());
            when(flowDefinitionRepo.findByStreamId(anyString())).thenReturn(definition);
            when(definitionQueryService.findByStreamId(anyString())).thenReturn(definition);

            Invoker smartFormInvoker = Mockito.mock(Invoker.class);
            setupRouterMock(brokerClient, FlowSmartFormService.class, HANDLE_SMART_FORM_GENERICABLE, smartFormInvoker,
                    null);

            handler.handleEvent(event);
            verify(smartFormInvoker, times(1)).invoke(anyList(), anyString());
        }

        @Test
        @DisplayName("获取智能表单任务事件创建任务实例处理异常成功")
        void givenSmartFormManualOperationEventThenHandleExceptionSuccessfully() throws Throwable {
            FlowTaskCreatedEvent event = new FlowTaskCreatedEvent(Collections.singletonList("flowContextId"),
                    "metaId-version", "nodeId", "publisher");
            FlowDefinition definition = generateFlowDefinition(FlowTaskType.AIPP_SMART_FORM);
            when(flowContextMapper.findByContextIdList(any())).thenReturn(generatePO());
            when(flowDefinitionRepo.findByStreamId(anyString())).thenReturn(definition);
            when(definitionQueryService.findByStreamId(anyString())).thenReturn(definition);

            Invoker smartFormInvoker = Mockito.mock(Invoker.class);
            setupRouterMockWithException(brokerClient, smartFormInvoker,
                    new JobberException(ErrorCodes.FLOW_HANDLE_SMART_FORM_FAILED));

            handler.handleEvent(event);
            verify(smartFormInvoker, times(1)).invoke(anyList(), anyString());
            verify(smartFormInvoker, times(1)).invoke(anyString(), anyList(), anyString());
        }

        @Test
        @DisplayName("测试处理人工任务时能够拿到下一个节点的id信息")
        void testGetNodeMetaIdSuccessWhenTaskHandle() throws Throwable {
            FlowTaskCreatedEvent event = new FlowTaskCreatedEvent(Collections.singletonList("flowContextId"),
                    "metaId-version", "nodeId", "publisher");
            FlowDefinition definition = generateFlowDefinition(FlowTaskType.AIPP_SMART_FORM);
            when(flowContextMapper.findByContextIdList(any())).thenReturn(generatePO());
            when(flowDefinitionRepo.findByStreamId(anyString())).thenReturn(definition);
            when(definitionQueryService.findByStreamId(anyString())).thenReturn(definition);

            SmartFormOperator operator = Mockito.mock(SmartFormOperator.class);
            MockedStatic<OperatorFactory> operatorFactory = Mockito.mockStatic(OperatorFactory.class);
            operatorFactory.when(() -> getOperator("SMART_FORM", brokerClient)).thenReturn(operator);
            Invoker smartFormInvoker = Mockito.mock(Invoker.class);
            setupRouterMock(brokerClient, FlowSmartFormService.class, HANDLE_SMART_FORM_GENERICABLE, smartFormInvoker,
                    null);

            handler.handleEvent(event);
            operatorFactory.close();

            ArgumentCaptor<List<FlowContext<FlowData>>> contextCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<FlowTask> taskCaptor = ArgumentCaptor.forClass(FlowTask.class);
            verify(operator).operate(contextCaptor.capture(), taskCaptor.capture());
            List<FlowContext<FlowData>> capturedContexts = contextCaptor.getValue();
            FlowTask taskCaptorValue = taskCaptor.getValue();

            Assertions.assertEquals("nodeId", taskCaptorValue.getNodeId());
            Assertions.assertEquals(1, capturedContexts.size());
            Map<String, Object> contextData = capturedContexts.get(0).getData().getContextData();
            Assertions.assertEquals("eventId", ObjectUtils.cast(contextData.get(Constant.NODE_META_ID)));
            Assertions.assertEquals("STATE", ObjectUtils.cast(contextData.get(Constant.NODE_TYPE)));
        }

        private void setupRouterMock(BrokerClient brokerClient, Class<?> serviceClass, String genericableId,
                                     Invoker invoker, Object result) throws Throwable {
            Router router = Mockito.mock(Router.class);
            when(brokerClient.getRouter(serviceClass, genericableId)).thenReturn(router);
            when(router.route(any())).thenReturn(invoker);
            when(invoker.invoke(any())).thenReturn(result);
        }

        private void setupRouterMockWithException(BrokerClient brokerClient, Invoker invoker, Throwable throwable)
                throws Throwable {
            Router router = Mockito.mock(Router.class);
            when(brokerClient.getRouter(any(), anyString())).thenReturn(router);
            when(router.route(any())).thenReturn(invoker);
            when(invoker.invoke(anyList(), anyString())).thenThrow(throwable);
            when(invoker.invoke(anyString(), anyList(), anyString())).thenReturn(null);
        }
    }
}