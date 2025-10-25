/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes;

import lombok.Getter;
import modelengine.fit.waterflow.domain.stream.nodes.From;
import modelengine.fit.waterflow.domain.stream.reactive.Publisher;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextMessenger;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextRepo;
import modelengine.fit.waterflow.domain.context.repo.flowlock.FlowLocks;
import modelengine.fitframework.log.Logger;

import java.util.Optional;

/**
 * 流程定义开始节点核心类
 * 流程实例流转时需要构建该对象
 *
 * @author 高诗意
 * @since 2023/08/14
 */
@Getter
public class FlowStartNode extends FlowNode {
    private static final Logger log = Logger.get(FlowStartNode.class);

    private Publisher<FlowData> publisher;

    @Override
    public Publisher<FlowData> getPublisher(String streamId, FlowContextRepo repo,
            FlowContextMessenger messenger, FlowLocks locks) {
        if (!Optional.ofNullable(this.publisher).isPresent()) {
            this.publisher = new From<>(streamId, this.metaId, repo, messenger, locks);
        }
        return this.publisher;
    }
}
