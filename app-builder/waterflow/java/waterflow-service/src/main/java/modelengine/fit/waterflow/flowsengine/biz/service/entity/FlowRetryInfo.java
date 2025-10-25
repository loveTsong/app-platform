/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.biz.service.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.domain.stream.nodes.To;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowRetry;

import java.util.List;

/**
 * 重试数据相关信息
 *
 * @author yangxiangyu
 * @since 2024/5/17
 */
@Setter
@Getter
@Builder
public class FlowRetryInfo {
    private FlowRetry flowRetry;

    private To<FlowData, Object> to;

    private List<FlowContext<String>> flowContexts;
}
