/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.biz.operation.operator;

import static modelengine.fit.waterflow.common.Constant.BUSINESS_DATA_KEY;
import static modelengine.fit.waterflow.common.Constant.FLOW_CONTEXT_ID_KEY;
import static modelengine.fit.waterflow.common.Constant.NODE_ID_KEY;
import static modelengine.fit.waterflow.common.Constant.NODE_TYPE;
import static modelengine.fit.waterflow.common.Constant.TRACE_ID_KEY;

import modelengine.fit.jober.FlowSmartFormService;
import modelengine.fit.jober.common.ErrorCodes;
import modelengine.fit.jober.common.exceptions.JobberException;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.tasks.FlowTask;
import modelengine.fitframework.broker.client.BrokerClient;
import modelengine.fitframework.broker.client.filter.route.FitableIdFilter;
import modelengine.fitframework.exception.FitException;
import modelengine.fitframework.log.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能表单Operator
 *
 * @author 李哲峰
 * @since 2023/12/14
 */
public class SmartFormOperator implements Operator {
    private static final Logger log = Logger.get(SmartFormOperator.class);

    private static final String HANDLE_SMART_FORM_GENERICABLE = "htctmizg0mydwnt2ttbbp8jlgo2e9e0w";

    private static final String HANDLE_SMART_FORM_FITABLE = "qz90ufu144m607hfud1ecbk0dnq3xavd";

    private final BrokerClient brokerClient;

    public SmartFormOperator(BrokerClient brokerClient) {
        this.brokerClient = brokerClient;
    }

    @Override
    public void operate(List<FlowContext<FlowData>> contexts, FlowTask task) {
        List<Map<String, Object>> inputs = contexts.stream().map(cxt -> new HashMap<String, Object>() {
            {
                put(FLOW_CONTEXT_ID_KEY, cxt.getId());
                put(NODE_ID_KEY, task.getNodeId());
                put(NODE_TYPE, cxt.getData().getContextData().get(NODE_TYPE));
                put(TRACE_ID_KEY, cxt.getTraceId());
                put(BUSINESS_DATA_KEY, cxt.getData().getBusinessData());
            }
        }).collect(Collectors.toList());

        try {
            brokerClient.getRouter(FlowSmartFormService.class, HANDLE_SMART_FORM_GENERICABLE)
                    .route(new FitableIdFilter(HANDLE_SMART_FORM_FITABLE))
                    .invoke(inputs, task.getTaskId());
        } catch (FitException e) {
            log.error("Caught a throwable during a remote invocation, fitableId is {}. Caused by {}",
                    HANDLE_SMART_FORM_FITABLE, e.getMessage());
            throw new JobberException(ErrorCodes.FLOW_HANDLE_SMART_FORM_FAILED);
        }
    }

    public BrokerClient getBrokerClient() {
        return brokerClient;
    }
}
