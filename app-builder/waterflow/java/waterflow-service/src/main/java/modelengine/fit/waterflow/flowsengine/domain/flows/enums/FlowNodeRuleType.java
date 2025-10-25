/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.domain.flows.enums;

import lombok.Getter;
import modelengine.fit.waterflow.domain.enums.FlowNodeType;
import modelengine.fit.waterflow.exceptions.WaterflowParamException;
import modelengine.fit.waterflow.flowsengine.domain.flows.validators.rules.nodes.ConditionNodeRule;
import modelengine.fit.waterflow.flowsengine.domain.flows.validators.rules.nodes.EndNodeRule;
import modelengine.fit.waterflow.flowsengine.domain.flows.validators.rules.nodes.ForkNodeRule;
import modelengine.fit.waterflow.flowsengine.domain.flows.validators.rules.nodes.NodeRule;
import modelengine.fit.waterflow.flowsengine.domain.flows.validators.rules.nodes.ParallelNodeRule;
import modelengine.fit.waterflow.flowsengine.domain.flows.validators.rules.nodes.StartNodeRule;
import modelengine.fit.waterflow.flowsengine.domain.flows.validators.rules.nodes.StateNodeRule;

import java.util.Arrays;

import static java.util.Locale.ROOT;
import static modelengine.fit.waterflow.ErrorCodes.ENUM_CONVERT_FAILED;

/**
 * The flow node rule types.
 *
 * @author 宋永坦
 * @since 2025-10-14
 */
@Getter
public enum FlowNodeRuleType {
    START(FlowNodeType.START, new StartNodeRule()),
    STATE(FlowNodeType.STATE, new StateNodeRule()),
    CONDITION(FlowNodeType.CONDITION, new ConditionNodeRule()),
    PARALLEL(FlowNodeType.PARALLEL, new ParallelNodeRule()),
    FORK(FlowNodeType.FORK, new ForkNodeRule()),
    JOIN(FlowNodeType.JOIN, null),
    EVENT(FlowNodeType.EVENT, null),
    END(FlowNodeType.END, new EndNodeRule());

    private final FlowNodeType type;
    private final NodeRule nodeRule;

    FlowNodeRuleType(FlowNodeType type, NodeRule nodeRule) {
        this.type = type;
        this.nodeRule = nodeRule;
    }

    /**
     * Get the node rule.
     *
     * @param type node type.
     * @return NodeRule The node rule.
     */
    public static NodeRule getRule(FlowNodeType type) {
        return Arrays.stream(values())
                .filter(value -> value.getType().equals(type))
                .findFirst()
                .map(parserType -> parserType.nodeRule)
                .orElseThrow(() -> new WaterflowParamException(ENUM_CONVERT_FAILED, "FlowNodeRuleType", type));
    }
}
