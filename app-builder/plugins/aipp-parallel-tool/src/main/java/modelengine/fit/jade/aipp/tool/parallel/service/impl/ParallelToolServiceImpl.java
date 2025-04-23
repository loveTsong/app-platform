/*
 * Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 * This file is a part of the ModelEngine Project.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package modelengine.fit.jade.aipp.tool.parallel.service.impl;

import modelengine.fit.jade.aipp.tool.parallel.entities.Config;
import modelengine.fit.jade.aipp.tool.parallel.entities.ToolCall;
import modelengine.fit.jade.aipp.tool.parallel.service.ParallelToolService;
import modelengine.fit.jade.tool.SyncToolCall;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Fit;
import modelengine.jade.carver.tool.annotation.Group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 并行工具调用节点服务接口的实现。
 *
 * @author 宋永坦
 * @since 2025-04-23
 */
@Component
@Group(name = "ParallelToolImpl")
public class ParallelToolServiceImpl implements ParallelToolService {
    private final SyncToolCall syncToolCall;

    public ParallelToolServiceImpl(@Fit SyncToolCall syncToolCall) {
        this.syncToolCall = syncToolCall;
    }

    @Override
    public Map<String, Object> call(List<ToolCall> toolCalls, Config config, Map<String, Object> context) {
        return new HashMap<>();
    }
}
