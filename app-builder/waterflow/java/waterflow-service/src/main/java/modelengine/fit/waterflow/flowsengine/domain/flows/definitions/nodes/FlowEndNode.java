/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes;

import lombok.Getter;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.domain.stream.nodes.To;
import modelengine.fit.waterflow.domain.stream.reactive.Subscriber;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextMessenger;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextRepo;
import modelengine.fit.waterflow.domain.context.repo.flowlock.FlowLocks;
import modelengine.fitframework.log.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 流程定义结束节点核心类
 * 流程实例流转时需要构建该对象
 *
 * @author 高诗意
 * @since 2023/08/14
 */
@Getter
public class FlowEndNode extends FlowNode {
    private static final Logger log = Logger.get(FlowEndNode.class);

    private Subscriber<FlowData, FlowData> subscriber;

    @Override
    public Subscriber<FlowData, FlowData> getSubscriber(String streamId, FlowContextRepo repo,
            FlowContextMessenger messenger, FlowLocks locks) {
        if (!Optional.ofNullable(this.subscriber).isPresent()) {
            this.subscriber = new To<>(streamId, this.metaId, this::endProduce, repo, messenger, locks, this.type);
            setCallback(this.subscriber, messenger);
            setGlobalTrace(this.subscriber, messenger);
        }
        return this.subscriber;
    }

    private List<FlowData> endProduce(List<FlowContext<FlowData>> input) {
        return input.stream().map(flowContext -> {
            addContextData(flowContext);
            return flowContext.getData();
        }).collect(Collectors.toList());
    }
}
