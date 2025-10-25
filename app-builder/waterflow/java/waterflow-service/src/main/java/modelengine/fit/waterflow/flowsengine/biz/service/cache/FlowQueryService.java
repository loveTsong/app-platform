/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.biz.service.cache;

import modelengine.fit.waterflow.domain.stream.reactive.Publisher;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fitframework.annotation.Component;

import lombok.AllArgsConstructor;

/**
 * 提供流程运行前查询运行的Flow结构服务
 *
 * @author yangxiangyu
 * @since 2025/1/24
 */
@Component
@AllArgsConstructor
public class FlowQueryService {
    private final FlowCacheService cacheService;

    /**
     * 根据stream id获取water flow
     *
     * @param streamId stream id
     * @return water flow启动入口
     */
    public Publisher<FlowData> getPublisher(String streamId) {
        return cacheService.getPublisher(streamId);
    }
}
