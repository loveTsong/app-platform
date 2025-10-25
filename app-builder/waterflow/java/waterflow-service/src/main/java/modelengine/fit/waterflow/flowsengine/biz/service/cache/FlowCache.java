/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.biz.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import modelengine.fit.waterflow.domain.stream.reactive.Publisher;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.FlowDefinition;

/**
 * 流程中的缓存结构
 *
 * @author yangxiangyu
 * @since 2025/1/22
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowCache {
    @Getter
    @Setter
    private FlowDefinition definition;

    @Getter
    @Setter
    private Publisher<FlowData> publisher;
}
