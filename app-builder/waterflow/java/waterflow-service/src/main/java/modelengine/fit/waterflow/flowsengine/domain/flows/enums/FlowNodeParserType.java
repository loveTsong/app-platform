/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.domain.flows.enums;

import lombok.Getter;
import modelengine.fit.waterflow.domain.enums.FlowNodeType;
import modelengine.fit.waterflow.exceptions.WaterflowParamException;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.nodes.ConditionNodeParser;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.nodes.EndNodeParser;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.nodes.NodeParser;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.nodes.ParallelNodeParser;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.nodes.StartNodeParser;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.nodes.StateNodeParser;

import java.util.Arrays;

import static modelengine.fit.waterflow.ErrorCodes.ENUM_CONVERT_FAILED;

/**
 * The flow node parser types.
 *
 * @author 宋永坦
 * @since 2025-10-14
 */
@Getter
public enum FlowNodeParserType {
    START(FlowNodeType.START, new StartNodeParser()),
    STATE(FlowNodeType.STATE, new StateNodeParser()),
    CONDITION(FlowNodeType.CONDITION, new ConditionNodeParser()),
    PARALLEL(FlowNodeType.PARALLEL, new ParallelNodeParser()),
    FORK(FlowNodeType.FORK, null),
    JOIN(FlowNodeType.JOIN, null),
    EVENT(FlowNodeType.EVENT, null),
    END(FlowNodeType.END, new EndNodeParser());

    private final FlowNodeType type;
    private final NodeParser nodeParser;

    FlowNodeParserType(FlowNodeType type, NodeParser nodeParser) {
        this.type = type;
        this.nodeParser = nodeParser;
    }

    /**
     * Get the node parser.
     *
     * @param type node type.
     * @return NodeParser The node parser.
     */
    public static NodeParser getParser(FlowNodeType type) {
        return Arrays.stream(values())
                .filter(value -> value.getType().equals(type))
                .findFirst()
                .map(parserType -> parserType.nodeParser)
                .orElseThrow(() -> new WaterflowParamException(ENUM_CONVERT_FAILED, "FlowNodeParserType", type));
    }
}
