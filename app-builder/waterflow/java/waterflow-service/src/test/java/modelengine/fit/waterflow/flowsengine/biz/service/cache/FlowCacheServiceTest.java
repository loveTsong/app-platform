/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.biz.service.cache;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import modelengine.fit.waterflow.domain.stream.reactive.Publisher;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextMessenger;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextRepo;
import modelengine.fit.waterflow.domain.context.repo.flowlock.FlowLocks;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.FlowDefinition;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.repo.FlowDefinitionRepo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * FlowCacheService对应测试类
 *
 * @author yangxiangyu
 * @since 2025/2/6
 */
class FlowCacheServiceTest {
    private FlowCacheService cacheService;
    private FlowContextRepo contextRepo;
    private FlowContextMessenger contextMessenger;
    private FlowLocks locks;
    private FlowDefinitionRepo definitionRepo;

    @BeforeEach
    void setUp() {
        contextRepo = Mockito.mock(FlowContextRepo.class);
        contextMessenger = Mockito.mock(FlowContextMessenger.class);
        locks = Mockito.mock(FlowLocks.class);
        definitionRepo = Mockito.mock(FlowDefinitionRepo.class);
        cacheService = new FlowCacheService(contextRepo, contextMessenger, locks, definitionRepo);
    }

    @AfterEach
    void tearDown() {
        FlowCacheService.clear();
    }

    @Test
    @DisplayName("测试根据streamId成功获取流程定义")
    void testGetDefinitionByStreamIdSuccess() {
        String streamId = "streamId";
        String definitionId = "definitionId";
        FlowDefinition definition = Mockito.mock(FlowDefinition.class);
        Publisher<FlowData> publisher = Mockito.mock(Publisher.class);
        when(definitionRepo.findByStreamId(anyString())).thenReturn(definition);
        when(definition.convertToFlow(contextRepo, contextMessenger, locks)).thenReturn(publisher);
        when(definition.getStreamId()).thenReturn(streamId);
        when(definition.getDefinitionId()).thenReturn(definitionId);

        FlowDefinition persistenceDefinition = cacheService.getDefinitionByStreamId(streamId);
        FlowDefinition cacheDefinition = cacheService.getDefinitionByStreamId(streamId);

        Assertions.assertEquals(definition, persistenceDefinition);
        Assertions.assertEquals(definition, cacheDefinition);
    }

    @Test
    @DisplayName("测试根据流程定义Id成功获取流程定义")
    void testGetDefinitionByIdSuccess() {
        String streamId = "streamId";
        String definitionId = "definitionId";
        FlowDefinition definition = Mockito.mock(FlowDefinition.class);
        Publisher<FlowData> publisher = Mockito.mock(Publisher.class);
        when(definitionRepo.find(anyString())).thenReturn(definition);
        when(definition.convertToFlow(contextRepo, contextMessenger, locks)).thenReturn(publisher);
        when(definition.getStreamId()).thenReturn(streamId);
        when(definition.getDefinitionId()).thenReturn(definitionId);

        FlowDefinition persistenceDefinition = cacheService.getDefinitionById(definitionId);
        FlowDefinition cacheDefinition = cacheService.getDefinitionById(definitionId);

        Assertions.assertEquals(definition, persistenceDefinition);
        Assertions.assertEquals(definition, cacheDefinition);
    }

    @Test
    @DisplayName("测试根据streamId成功获取publisher")
    void testGetPublisherByStreamIdSuccess() {
        String streamId = "streamId";
        String definitionId = "definitionId";
        FlowDefinition definition = Mockito.mock(FlowDefinition.class);
        Publisher<FlowData> publisher = Mockito.mock(Publisher.class);
        when(definitionRepo.findByStreamId(anyString())).thenReturn(definition);
        when(definition.convertToFlow(contextRepo, contextMessenger, locks)).thenReturn(publisher);
        when(definition.getStreamId()).thenReturn(streamId);
        when(definition.getDefinitionId()).thenReturn(definitionId);

        Publisher<FlowData> persistPublisher = cacheService.getPublisher(streamId);
        Publisher<FlowData> cachePublisher = cacheService.getPublisher(streamId);

        Assertions.assertEquals(publisher, persistPublisher);
        Assertions.assertEquals(publisher, cachePublisher);
    }
}