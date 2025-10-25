/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.filters;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import modelengine.fit.waterflow.domain.stream.operators.Operators;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.enums.FlowFilterType;

import java.util.Map;

/**
 * 流程节点数据过滤器
 *
 * @author 高诗意
 * @since 2023/09/25
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class FlowFilter {
    private FlowFilterType filterType;

    private Map<String, String> properties;

    /**
     * filter
     *
     * @return Filter<FlowData>
     */
    public abstract Operators.Filter<FlowData> filter();
}
