/*
 * Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 * This file is a part of the ModelEngine Project.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package modelengine.fit.jade.aipp.tool.parallel.support.impl;

import modelengine.fit.jade.aipp.tool.parallel.support.TaskExecutor;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Value;
import modelengine.fitframework.inspection.Validation;
import modelengine.fitframework.util.StringUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务执行器的实现。
 *
 * @author 宋永坦
 * @since 2025-04-24
 */
@Component
public class DefaultTaskExecutor implements TaskExecutor {
    private static final int MIN_THREAD_POOL_SIZE = 1;
    private static final int MAX_THREAD_POOL_SIZE = 128;
    private static final int MIN_QUEUE_SIZE = 100;
    private static final int MAX_QUEUE_SIZE = 10000;

    private final ExecutorService executorService;

    public DefaultTaskExecutor(@Value("${parallel-tool.thread-pool-size:64}") int threadPoolSize,
            @Value("${parallel-tool.max-queue-size:1000}") int maxQueueSize) {
        Validation.between(threadPoolSize,
                MIN_THREAD_POOL_SIZE,
                MAX_THREAD_POOL_SIZE,
                StringUtils.format("The parallel tool thread pool size should between {0} and {1}.",
                        MIN_THREAD_POOL_SIZE,
                        MAX_THREAD_POOL_SIZE));
        Validation.between(maxQueueSize,
                MIN_QUEUE_SIZE,
                MAX_QUEUE_SIZE,
                StringUtils.format("The parallel tool queue size should between {0} and {1}.",
                        MIN_QUEUE_SIZE,
                        MAX_QUEUE_SIZE));
        this.executorService = new ThreadPoolExecutor(0,
                threadPoolSize,
                5L,
                TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(maxQueueSize, true));
    }

    @Override
    public void post(Runnable runnable) {
        executorService.execute(runnable);
    }
}
