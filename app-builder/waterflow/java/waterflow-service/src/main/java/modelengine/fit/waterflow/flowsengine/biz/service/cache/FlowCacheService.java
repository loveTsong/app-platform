/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.biz.service.cache;

import static java.lang.System.identityHashCode;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.AllArgsConstructor;
import modelengine.fit.waterflow.domain.stream.reactive.Publisher;
import modelengine.fit.waterflow.domain.stream.reactive.Subscriber;
import modelengine.fit.waterflow.domain.stream.reactive.Subscription;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextMessenger;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextRepo;
import modelengine.fit.waterflow.domain.context.repo.flowlock.FlowLocks;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.FlowDefinition;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.repo.FlowDefinitionRepo;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.log.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 提供流程运行时的缓存服务
 *
 * @author yangxiangyu
 * @since 2025/1/24
 */
@Component
@AllArgsConstructor
public class FlowCacheService {
    private static final Logger LOG = Logger.get(FlowCacheService.class);
    private static final Cache<String, FlowCache> STREAM_ID_FLOW_CACHE =
            Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).maximumSize(1000).build();
    private static final Cache<String, FlowDefinition> DEFINITION_ID_FLOW_CACHE =
            Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).maximumSize(1000).build();

    private final FlowContextRepo contextRepo;
    private final FlowContextMessenger contextMessenger;
    private final FlowLocks locks;
    private final FlowDefinitionRepo definitionRepo;

    /**
     * 根据streamID获取缓存中的流程定义
     *
     * @param streamId stream id
     * @return 流程定义
     */
    public FlowDefinition getDefinitionByStreamId(String streamId) {
        FlowCache flowCache = Optional.ofNullable(getFlowCacheByStreamId(streamId)).orElse(new FlowCache());
        return flowCache.getDefinition();
    }

    /**
     * 根据流程定义id获取缓存中的流程定义
     *
     * @param id 流程定义id
     * @return 流程定义
     */
    public FlowDefinition getDefinitionById(String id) {
        return DEFINITION_ID_FLOW_CACHE.get(id, var -> definitionRepo.find(id));
    }

    /**
     * 根据流程版本获取water flow
     *
     * @param streamId 流程版本
     * @return {@link Publisher} water flow
     */
    public Publisher<FlowData> getPublisher(String streamId) {
        FlowCache flowCache = Optional.ofNullable(getFlowCacheByStreamId(streamId)).orElse(new FlowCache());
        return flowCache.getPublisher();
    }

    private FlowCache getFlowCacheByStreamId(String streamId) {
        return STREAM_ID_FLOW_CACHE.get(streamId, var -> {
            FlowDefinition definition = definitionRepo.findByStreamId(streamId);
            if (definition == null) {
                return null;
            }
            Publisher<FlowData> publisher = definition.convertToFlow(contextRepo, contextMessenger, locks);
            Set<String> toIds = new HashSet<>();
            buildNodeInfoMap(toIds, publisher, 1);
            LOG.info("[WaterFlows::putPublisher] put new publisher only if not exists, "
                            + "streamId: {}, publisher: {}, exits: {}",
                    streamId, identityHashCode(publisher), identityHashCode(publisher));
            return new FlowCache(definition, publisher);
        });
    }

    private void buildNodeInfoMap(Set<String> toIds, Publisher publisher, int order) {
        toIds.add(publisher.getId());
        List<Subscription> subscriptions = publisher.getSubscriptions();
        subscriptions.forEach(subscription -> {
            Subscriber to = subscription.getTo();
            LOG.info("buildNodeInfo, nodeId: {}, order:{}", to.getId(), order);
            if (toIds.contains(to.getId())) {
                return;
            }
            to.setOrder(order);
            if (to instanceof Publisher) {
                buildNodeInfoMap(toIds, (Publisher) to, order + 1);
            }
        });
    }

    /**
     * 清空缓存
     */
    public static void clear() {
        STREAM_ID_FLOW_CACHE.invalidateAll();
        DEFINITION_ID_FLOW_CACHE.invalidateAll();
    }
}
