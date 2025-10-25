/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.filters;

import modelengine.fit.waterflow.domain.stream.operators.Operators;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 最小Size的过滤器
 *
 * @author 高诗意
 * @since 2023/09/25
 */
public class FlowBatchSizeFilter extends FlowFilter {
    private static final String THRESHOLD = "threshold";

    @Override
    public Operators.Filter<FlowData> filter() {
        int threshold = Integer.parseInt(this.getProperties().get(THRESHOLD));
        return (inputs) -> {
            inputs.forEach(input -> {
            });
            if (inputs.size() < threshold) {
                return new ArrayList<>();
            }
            return inputs.stream().limit(threshold).collect(Collectors.toList());
        };
    }
}
