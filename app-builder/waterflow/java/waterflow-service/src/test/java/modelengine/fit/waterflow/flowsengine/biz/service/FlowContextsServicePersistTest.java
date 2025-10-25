/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.biz.service;

import static modelengine.fit.waterflow.flowsengine.domain.flows.FlowsTestUtil.waitSingle;
import static modelengine.fit.waterflow.domain.enums.FlowNodeStatus.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import modelengine.fit.waterflow.DatabaseBaseTest;
import modelengine.fit.waterflow.FlowsDataBaseTest;
import modelengine.fit.waterflow.MethodNameLoggerExtension;
import modelengine.fit.waterflow.common.utils.SleepUtil;
import modelengine.fit.waterflow.flowsengine.biz.service.cache.FlowDefinitionQueryService;
import modelengine.fit.waterflow.flowsengine.biz.service.cache.FlowQueryService;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowRetry;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowTrace;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextMessenger;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowcontext.FlowContextPersistMessenger;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowcontext.FlowContextPersistRepo;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowcontext.QueryFlowContextPersistRepo;
import modelengine.fit.waterflow.domain.context.repo.flowlock.FlowLocks;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowlock.FlowLocksMemo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowretry.DefaultFlowRetryRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowretry.FlowRetryRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowtrace.DefaultFlowTraceRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowtrace.FlowTraceRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.FlowDefinition;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.repo.DefaultFlowDefinitionRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.repo.FlowDefinitionRepo;
import modelengine.fit.waterflow.domain.enums.FlowNodeStatus;
import modelengine.fit.waterflow.domain.enums.FlowTraceStatus;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.FlowParser;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.Parser;
import modelengine.fit.waterflow.flowsengine.domain.flows.streams.From;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FitableUsageMapper;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowContextMapper;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowDefinitionMapper;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowRetryMapper;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowTraceMapper;
import modelengine.fit.waterflow.flowsengine.persist.po.FlowRetryPO;
import modelengine.fit.waterflow.service.FlowRuntimeService;
import modelengine.fitframework.broker.client.BrokerClient;
import modelengine.fitframework.broker.client.Invoker;
import modelengine.fitframework.broker.client.Router;
import modelengine.fitframework.plugin.Plugin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * {@link FlowContextsService} 对应测试类（数据库环境）
 *
 * @author 李哲峰
 * @since 2024/2/7
 */
@ExtendWith(MethodNameLoggerExtension.class)
public class FlowContextsServicePersistTest extends DatabaseBaseTest {
    private static final BrokerClient BROKER_CLIENT = Mockito.mock(BrokerClient.class);

    private static final Plugin PLUGIN = Mockito.mock(Plugin.class);

    private static final Parser PARSER = new FlowParser(BROKER_CLIENT);

    private static final FlowContextMapper FLOW_CONTEXT_MAPPER;

    private static final FlowTraceMapper FLOW_TRACE_MAPPER;

    private static final FlowTraceRepo TRACE_REPO;

    private static final FlowRetryMapper FLOW_RETRY_MAPPER;

    private static final FlowRetryRepo FLOW_RETRY_REPO;

    private static final FlowDefinitionMapper FLOW_DEFINITION_MAPPER;

    private static final FlowContextRepo<FlowData> REPO;

    private static final QueryFlowContextPersistRepo QUERY_REPO;

    private static final FlowDefinitionRepo DEFINITION_REPO;

    private static final FlowContextMessenger MESSENGER = new FlowContextPersistMessenger(PLUGIN);

    private static final FlowLocks LOCKS = new FlowLocksMemo();

    private static final FlowRuntimeService FLOW_CONTEXTS_SERVICE;

    private static final FlowRetryService FLOW_RETRY_SERVICE;

    private static final TraceOwnerService traceOwnerService;

    private static FlowDefinitionQueryService definitionQueryService;

    private static FlowQueryService flowQueryService;

    static {
        sqlSessionManager.startManagedSession(true);
        FLOW_CONTEXT_MAPPER = sqlSessionManager.getMapper(FlowContextMapper.class);
        FLOW_TRACE_MAPPER = sqlSessionManager.getMapper(FlowTraceMapper.class);
        TRACE_REPO = new DefaultFlowTraceRepo(FLOW_TRACE_MAPPER);
        FLOW_RETRY_MAPPER = sqlSessionManager.getMapper(FlowRetryMapper.class);
        FLOW_RETRY_REPO = new DefaultFlowRetryRepo(FLOW_RETRY_MAPPER);
        FLOW_DEFINITION_MAPPER = sqlSessionManager.getMapper(FlowDefinitionMapper.class);
        Integer defaultLimitation = 5;
        traceOwnerService = Mockito.mock(TraceOwnerService.class);
        boolean useLimit = false;
        REPO = new FlowContextPersistRepo(FLOW_CONTEXT_MAPPER, TRACE_REPO, FLOW_RETRY_REPO, traceOwnerService,
                defaultLimitation, useLimit, 1);
        QUERY_REPO = new QueryFlowContextPersistRepo(FLOW_CONTEXT_MAPPER);
        FitableUsageMapper fitableUsageMapper = sqlSessionManager.getMapper(FitableUsageMapper.class);
        DEFINITION_REPO = new DefaultFlowDefinitionRepo(FLOW_DEFINITION_MAPPER, fitableUsageMapper, PARSER);
        FLOW_CONTEXTS_SERVICE = new FlowRuntimeServiceImpl(DEFINITION_REPO, REPO, MESSENGER, QUERY_REPO, TRACE_REPO,
                FLOW_RETRY_REPO, LOCKS, traceOwnerService, null, false, BROKER_CLIENT,
                definitionQueryService, flowQueryService);
        FLOW_RETRY_SERVICE = new FlowRetryService(REPO, FLOW_RETRY_REPO, LOCKS, traceOwnerService,
                definitionQueryService, flowQueryService);
    }

    @Override
    protected void cleanTable() {
        executeSqlInFile("handler/flowDefinition/cleanData.sql");
        executeSqlInFile("handler/flowTrace/cleanData.sql");
        executeSqlInFile("handler/flowContext/cleanData.sql");
        executeSqlInFile("handler/flowRetry/cleanData.sql");
    }

    @Nested
    @DisplayName("测试重试任务合集")
    class RetryJoberTest extends FlowsDataBaseTest {
        private static final String FILE_PATH_PREFIX = "flows/executors/";

        private MockedStatic<SleepUtil> sleepUtilMockedStatic;

        @BeforeEach
        void setUp() {
            sleepUtilMockedStatic = mockStatic(SleepUtil.class);
        }
        @AfterEach
        void tearDown() {
            sleepUtilMockedStatic.close();
        }

        @Test
        @Disabled("目前发现测试用的H2内存数据库出现insert成功但是查不到的情况，后面某个用例给查出来了")
        @DisplayName("测试重试自动任务成功： 预设重试计划，然后重试自动任务遇到不可重试异常，流程终止")
        public void testRetryJoberSuccess() {
            String jsonData = getJsonData(getFilePath("flows_auto_general_jober_with_state_node_1_to_1.json"));
            FlowDefinition flowDefinition = PARSER.parse(jsonData);
            String streamId = flowDefinition.getStreamId();
            flowDefinition.setCreatedBy("lzf");
            flowDefinition.setTenant("framework");
            DEFINITION_REPO.save(flowDefinition, jsonData);
            From<FlowData> flow = (From<FlowData>) flowDefinition.convertToFlow(REPO, MESSENGER, LOCKS);
            LocalDateTime startTime = LocalDateTime.parse("2024-02-01T11:18:01.011");
            FlowTrace flowTrace = FlowTrace.builder()
                    .streamId(streamId)
                    .operator("lzf")
                    .application("aipp")
                    .startNode("start1")
                    .endNode("ender1")
                    .startTime(startTime)
                    .status(FlowTraceStatus.RUNNING)
                    .contextPool(new HashSet<>())
                    .build();
            TRACE_REPO.save(flowTrace);
            String traceId = flowTrace.getId();
            FlowContext<FlowData> context = generateFlowContext(streamId, traceId, "state1", "toBatchId",
                    FlowNodeStatus.RETRYABLE);
            REPO.save(Collections.singletonList(context));
            TRACE_REPO.updateContextPool(Collections.singletonList(traceId),
                    Collections.singletonList(context.getId()));
            FlowRetry flowRetry = new FlowRetry("toBatchId", "toBatch", LocalDateTime.now(), null, 0, 1);
            FLOW_RETRY_REPO.save(Collections.singletonList(flowRetry));
            when(traceOwnerService.isAnyOwn(anySet())).thenReturn(true);
            when(traceOwnerService.getTraces()).thenReturn(new ArrayList<>(context.getTraceId()));
            when(traceOwnerService.isOwn(traceId)).thenReturn(true);
            sleepUtilMockedStatic.when(() -> SleepUtil.sleep(anyInt())).thenAnswer((invocation -> null));

            Router mockRouter = Mockito.mock(Router.class);
            when(BROKER_CLIENT.getRouter(any(), anyString())).thenReturn(mockRouter);
            Invoker mockInvoker = Mockito.mock(Invoker.class);
            when(mockRouter.route(any())).thenReturn(mockInvoker);
            when(mockInvoker.communicationType(any())).thenReturn(mockInvoker);
            when(mockInvoker.invoke(any(), anyList(), anyString())).thenReturn(null);

            FLOW_RETRY_SERVICE.retryTask();

            FlowRetryPO flowRetryPO = FLOW_RETRY_MAPPER.find("toBatchId");
            if (flowRetryPO != null) {
                Assertions.assertTrue(flowRetryPO.getNextRetryTime().isAfter(flowRetryPO.getLastRetryTime()));
                Assertions.assertEquals(1, flowRetryPO.getRetryCount());
                Assertions.assertEquals(2, flowRetryPO.getVersion());
            }
            waitSingle(contextSupplier(REPO, streamId, traceId, "state1", ERROR));
            Assertions.assertNull(FLOW_RETRY_MAPPER.find("toBatchId"));
        }

        @Override
        protected String getFilePathPrefix() {
            return FILE_PATH_PREFIX;
        }

        private FlowContext<FlowData> generateFlowContext(String streamId, String traceId, String position,
                String toBatch, FlowNodeStatus status) {
            HashMap<String, Object> businessData = new HashMap<>();
            businessData.put("application", "aipp");
            FlowData flowData = FlowData.builder()
                    .operator("lzf")
                    .startTime(LocalDateTime.now())
                    .businessData(businessData)
                    .contextData(new HashMap<>())
                    .build();
            FlowContext<FlowData> context = new FlowContext(streamId, "rootId", flowData,
                    Collections.singleton(traceId), position);
            context.setStatus(status);
            context.toBatch(toBatch);
            return context;
        }
    }
}
