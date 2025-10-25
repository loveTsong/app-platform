/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.biz.service.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import modelengine.fit.waterflow.MethodNameLoggerExtension;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.callbacks.FlowCallback;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.callbacks.FlowGeneralCallback;
import modelengine.fit.waterflow.flowsengine.domain.flows.enums.FlowCallbackType;
import modelengine.fit.waterflow.flowsengine.domain.flows.events.FlowCallbackEvent;
import modelengine.fit.waterflow.spi.FlowCallbackService;
import modelengine.fitframework.broker.client.BrokerClient;
import modelengine.fitframework.broker.client.Invoker;
import modelengine.fitframework.broker.client.Router;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;

/**
 * {@link FlowCallbackEventHandler} 测试类
 *
 * @author 李哲峰
 * @since 2023/12/12
 */
@ExtendWith(MethodNameLoggerExtension.class)
public class FlowCallbackEventHandlerTest {
    private static final String GENERAL_CALLBACK_GENERICABLE = "w8onlgq9xsw13jce4wvbcz3kbmjv3tuw";

    private FlowCallbackEventHandler handler;

    private BrokerClient brokerClient;

    public FlowCallbackEventHandlerTest() {
    }

    private FlowCallback generateFlowCallback() {
        FlowCallback flowCallback = new FlowGeneralCallback();
        flowCallback.setName("通知回调");
        flowCallback.setType(FlowCallbackType.GENERAL_CALLBACK);
        flowCallback.setFilteredKeys(Collections.singleton("application"));
        flowCallback.setFitables(Collections.singleton("通知回调实现"));
        flowCallback.setBrokerClient(brokerClient);
        return flowCallback;
    }

    private FlowContext<FlowData> generateFlowContext() {
        HashMap<String, Object> businessData = new HashMap<>();
        businessData.put("description", "描述信息");
        businessData.put("third_party_data", "第三方系统数据");
        businessData.put("creator", "lzf");
        businessData.put("owner1", "lzf_owner");
        businessData.put("priority", "urgent");
        businessData.put("statue", "approved");
        businessData.put("application", "aipp");
        FlowData flowData = FlowData.builder()
                .operator("lzf")
                .startTime(LocalDateTime.now())
                .businessData(businessData)
                .contextData(new HashMap<>())
                .build();
        return new FlowContext("streamId", "rootId", flowData, Collections.singleton("traceId"), "nodeId");
    }

    @Nested
    @DisplayName("测试手动执行回调事件的处理")
    class TestFlowCallbackEventHandler {
        @BeforeEach
        void setUp() {
            brokerClient = Mockito.mock(BrokerClient.class);
            handler = new FlowCallbackEventHandler();
        }

        @Test
        @DisplayName("流程回调函数事件处理成功")
        void givenFlowCallbackCreatedEventThenHandleSuccessfully() throws Throwable {
            FlowCallbackEvent event = new FlowCallbackEvent(Collections.singletonList(generateFlowContext()),
                    generateFlowCallback(), "publisher");

            Invoker callbackInvoker = Mockito.mock(Invoker.class);
            setupRouterMock(brokerClient, FlowCallbackService.class, GENERAL_CALLBACK_GENERICABLE, callbackInvoker,
                    null);
            handler.handleEvent(event);
            verify(callbackInvoker, times(1)).invoke(anyList());
        }

        private void setupRouterMock(BrokerClient brokerClient, Class<?> serviceClass, String genericableId,
                Invoker invoker, Object result) throws Throwable {
            Router router = Mockito.mock(Router.class);
            when(brokerClient.getRouter(serviceClass, genericableId)).thenReturn(router);
            when(router.route(any())).thenReturn(invoker);
            when(invoker.invoke(any())).thenReturn(result);
        }
    }
}
