/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.biz.operation.operator;

import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.tasks.FlowTask;

import java.util.List;

/**
 * 任务处理接口
 *
 * @author 晏钰坤
 * @since 2023/9/15
 */
public interface Operator {
    /**
     * 处理流程中的手动任务
     *
     * @param contexts 流程流转数据
     * @param task 流程定义中的手动任务
     */
    void operate(List<FlowContext<FlowData>> contexts, FlowTask task);
}
