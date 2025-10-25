/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.biz.service.scheduletasks;

import modelengine.fit.waterflow.DatabaseBaseTest;
import modelengine.fit.waterflow.FlowsDataBaseTest;
import modelengine.fit.waterflow.MethodNameLoggerExtension;
import modelengine.fit.waterflow.flowsengine.biz.service.DefaultTraceOwnerService;
import modelengine.fit.waterflow.flowsengine.biz.service.TraceOwnerService;
import modelengine.fit.waterflow.flowsengine.biz.service.cache.FlowDefinitionQueryService;
import modelengine.fit.waterflow.flowsengine.biz.service.cache.FlowQueryService;
import modelengine.fit.waterflow.flowsengine.domain.flows.FlowsTestUtil;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowcontext.FlowContextPersistMessenger;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowcontext.FlowContextPersistRepo;
import modelengine.fit.waterflow.domain.context.repo.flowlock.FlowLocks;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowlock.FlowLocksMemo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowretry.DefaultFlowRetryRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowretry.FlowRetryRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowtrace.DefaultFlowTraceRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowtrace.FlowTraceRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.FlowDefinition;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.repo.DefaultFlowDefinitionRepo;
import modelengine.fit.waterflow.domain.enums.FlowNodeStatus;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.FlowParser;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.Parser;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FitableUsageMapper;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowContextMapper;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowDefinitionMapper;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowRetryMapper;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowTraceMapper;

import modelengine.fit.waterflow.spi.lock.InvalidDistributedLockNotify;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.List;

/**
 * RestartContextSchedule对应测试类
 *
 * @author 杨祥宇
 * @since 2024/4/24
 */
@Disabled("目前发现测试用的H2内存数据库出现insert或update成功但是查不到的情况，后面某个用例给查出来了")
@ExtendWith(MethodNameLoggerExtension.class)
class RestartContextScheduleTest extends DatabaseBaseTest {
    private RestartContextSchedule restartContextSchedule;

    private FlowTraceRepo traceRepo;

    private FlowContextPersistRepo contextPersistRepo;

    private DefaultFlowDefinitionRepo flowDefinitionRepo;

    private FlowLocks locks = new FlowLocksMemo();

    private FlowContextPersistMessenger messenger = new FlowContextPersistMessenger(null);

    private TraceOwnerService traceOwnerService;

    private FlowTraceMapper traceMapper;

    private FlowContextMapper contextMapper;

    private FlowDefinitionMapper flowDefinitionMapper;

    private FitableUsageMapper fitableUsageMapper;

    private FlowRetryRepo retryRepo;

    private FlowDefinitionQueryService definitionQueryService;

    private FlowQueryService flowQueryService;

    private final Parser parser = new FlowParser(null);

    @BeforeEach
    void setUp() {
        traceMapper = sqlSessionManager.getMapper(FlowTraceMapper.class);
        traceRepo = new DefaultFlowTraceRepo(traceMapper);
        contextMapper = sqlSessionManager.getMapper(FlowContextMapper.class);
        InvalidDistributedLockNotify notify = Mockito.mock(InvalidDistributedLockNotify.class);
        traceOwnerService = new DefaultTraceOwnerService(locks, notify);
        FlowRetryMapper retryMapper = sqlSessionManager.getMapper(FlowRetryMapper.class);
        retryRepo = new DefaultFlowRetryRepo(retryMapper);
        contextPersistRepo = new FlowContextPersistRepo(contextMapper, traceRepo, retryRepo, traceOwnerService, 10,
                false, 1);
        flowDefinitionMapper = sqlSessionManager.getMapper(FlowDefinitionMapper.class);
        fitableUsageMapper = sqlSessionManager.getMapper(FitableUsageMapper.class);
        flowDefinitionRepo = new DefaultFlowDefinitionRepo(flowDefinitionMapper, fitableUsageMapper, parser);
        restartContextSchedule = new RestartContextSchedule(traceRepo, contextPersistRepo, flowDefinitionRepo, locks,
                messenger, traceOwnerService, definitionQueryService, flowQueryService);
    }

    @Nested
    @DisplayName("重启后context重试测试")
    class RestartContextTest extends FlowsDataBaseTest {
        private final String sql = "handler/flowRestart/saveData.sql";

        private final String deleteSql = "handler/flowRestart/cleanData.sql";

        @Override
        protected String getFilePathPrefix() {
            return "flows/executors/";
        }

        @BeforeEach
        void setUp() {
            executeSqlInFile(sql);
        }

        @AfterEach
        void tearDown() {
            executeSqlInFile(deleteSql);
        }
        @Test
        @DisplayName("测试状态为new的context重试成功")
        void restartNewContextSuccess() {
            String jsonData = getJsonData(getFilePath("flows_auto_echo_state_node_1_to_1.json"));
            FlowDefinition definition = parser.parse(jsonData);
            definition.setTenant("yxy");
            definition.setCreatedBy("yxy");
            flowDefinitionRepo.save(definition, jsonData);

            String traceId = "restartTraceNew";

            restartContextSchedule.restartInterruptContext();

            String contextId = "restartContextNew";
            List<FlowContext<FlowData>> contexts = FlowsTestUtil.waitSingle(
                    contextSupplier(contextPersistRepo, definition.getStreamId(), traceId, "state2", FlowNodeStatus.ARCHIVED));
            Assertions.assertEquals(FlowNodeStatus.ARCHIVED, contexts.get(0).getStatus());
            Assertions.assertEquals("state2", contexts.get(0).getPosition());
            Assertions.assertEquals(contextId, contexts.get(0).getId());
        }

        @Test
        @DisplayName("测试状态为pending的context重试成功")
        void restartPendingContextSuccess() {
            String jsonData = getJsonData(getFilePath("flows_auto_echo_state_node_1_to_1.json"));
            FlowDefinition definition = parser.parse(jsonData);
            definition.setTenant("yxy");
            definition.setCreatedBy("yxy");
            flowDefinitionRepo.save(definition, jsonData);
            String traceId = "restartTracePending";

            restartContextSchedule.restartInterruptContext();

            String contextId = "restartContextPending";
            List<FlowContext<FlowData>> contexts = FlowsTestUtil.waitSingle(
                    contextSupplier(contextPersistRepo, definition.getStreamId(), traceId, "state2", FlowNodeStatus.ARCHIVED));
            Assertions.assertEquals(FlowNodeStatus.ARCHIVED, contexts.get(0).getStatus());
            Assertions.assertEquals("state2", contexts.get(0).getPosition());
            Assertions.assertEquals(contextId, contexts.get(0).getId());
        }

        @Test
        @DisplayName("测试状态为ready的context重试成功")
        void restartReadyContextSuccess() {
            String jsonData = getJsonData(getFilePath("flows_auto_echo_state_node_1_to_1.json"));
            FlowDefinition definition = parser.parse(jsonData);
            definition.setTenant("yxy");
            definition.setCreatedBy("yxy");
            flowDefinitionRepo.save(definition, jsonData);
            String traceId = "restartTraceReady";

            restartContextSchedule.restartInterruptContext();

            String contextId = "restartContextReady";
            List<FlowContext<FlowData>> contexts = FlowsTestUtil.waitSingle(
                    contextSupplier(contextPersistRepo, definition.getStreamId(), traceId, "state1", FlowNodeStatus.RETRYABLE));
            Assertions.assertEquals(FlowNodeStatus.RETRYABLE, contexts.get(0).getStatus());
            Assertions.assertEquals("state1", contexts.get(0).getPosition());
            Assertions.assertEquals(contextId, contexts.get(0).getId());
        }
    }
}