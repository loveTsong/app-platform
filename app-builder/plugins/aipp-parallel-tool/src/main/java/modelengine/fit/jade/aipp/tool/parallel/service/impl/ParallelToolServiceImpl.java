/*
 * Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 * This file is a part of the ModelEngine Project.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package modelengine.fit.jade.aipp.tool.parallel.service.impl;

import modelengine.fit.jade.aipp.tool.parallel.domain.BatchRequest;
import modelengine.fit.jade.aipp.tool.parallel.entities.Argument;
import modelengine.fit.jade.aipp.tool.parallel.entities.Config;
import modelengine.fit.jade.aipp.tool.parallel.entities.ToolCall;
import modelengine.fit.jade.aipp.tool.parallel.service.ParallelToolService;
import modelengine.fit.jade.aipp.tool.parallel.support.TaskExecutor;
import modelengine.fit.jade.tool.SyncToolCall;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Fit;
import modelengine.fitframework.annotation.Fitable;
import modelengine.fitframework.annotation.Property;
import modelengine.fitframework.annotation.Value;
import modelengine.fitframework.inspection.Validation;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.util.CollectionUtils;
import modelengine.fitframework.util.StringUtils;
import modelengine.jade.carver.tool.annotation.Attribute;
import modelengine.jade.carver.tool.annotation.Group;
import modelengine.jade.carver.tool.annotation.ToolMethod;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 并行工具调用节点服务接口的实现。
 *
 * @author 宋永坦
 * @since 2025-04-23
 */
@Component
@Group(name = "ParallelToolImpl")
public class ParallelToolServiceImpl implements ParallelToolService {
    private static final Logger LOG = Logger.get(ParallelToolServiceImpl.class);
    private static final int MIN_CONCURRENCY = 1;
    private static final int MAX_CONCURRENCY = 32;

    private final SyncToolCall syncToolCall;

    private final TaskExecutor taskExecutor;

    private final Config defaultConfig;

    public ParallelToolServiceImpl(@Fit SyncToolCall syncToolCall, TaskExecutor taskExecutor,
            @Value("${parallel-tool.concurrency:8}") int defaultConcurrency) {
        this.syncToolCall = syncToolCall;
        this.taskExecutor = taskExecutor;
        this.defaultConfig = Config.builder()
                .concurrency(Validation.between(defaultConcurrency,
                        MIN_CONCURRENCY,
                        MAX_CONCURRENCY,
                        StringUtils.format("The parallel tool concurrent should between {0} and {1}.",
                                MIN_CONCURRENCY,
                                MAX_CONCURRENCY)))
                .build();
    }

    @Override
    @Fitable("default")
    @ToolMethod(name = "parallelToolDefault", description = "用于循环执行工具", extensions = {
            @Attribute(key = "tags", value = "FIT"), @Attribute(key = "tags", value = "BASIC"),
            @Attribute(key = "tags", value = "PARALLELNODESTATE")
    })
    @Property(description = "循环执行工具的结果")
    public Map<String, Object> call(List<ToolCall> toolCalls, Config config, Map<String, Object> context) {
        if (!this.isValidToolCalls(toolCalls)) {
            throw new IllegalArgumentException("Invalid toolCalls param.");
        }
        BatchRequest batchRequest =
                new BatchRequest(toolCalls, this.getConfig(config), this.syncToolCall, this.taskExecutor);
        batchRequest.post();
        return batchRequest.await();
    }

    private boolean isValidToolCalls(List<ToolCall> toolCalls) {
        return !CollectionUtils.isEmpty(toolCalls) && this.isValidOutputName(toolCalls) && toolCalls.stream()
                .noneMatch(toolCall -> toolCall == null || !this.isValidArgs(toolCall.getArgs()));
    }

    private boolean isValidArgs(List<Argument> args) {
        return !(args == null || args.stream().anyMatch(arg -> arg == null || StringUtils.isEmpty(arg.getName())));
    }

    private boolean isValidOutputName(List<ToolCall> toolCalls) {
        Set<String> hitSet = new HashSet<>();
        return toolCalls.stream()
                .map(ToolCall::getOutputName)
                .allMatch(outputName -> StringUtils.isNotEmpty(outputName) && hitSet.add(outputName));
    }

    private Config getConfig(Config config) {
        if (config == null || config.getConcurrency() == null) {
            return this.defaultConfig;
        }
        if (config.getConcurrency() < MIN_CONCURRENCY || config.getConcurrency() > MAX_CONCURRENCY) {
            LOG.warn("The given concurrency is illegal, it should between 1 and 32, use default config instead. "
                    + "[concurrency={}]", config.getConcurrency());
            return this.defaultConfig;
        }
        return config;
    }
}
