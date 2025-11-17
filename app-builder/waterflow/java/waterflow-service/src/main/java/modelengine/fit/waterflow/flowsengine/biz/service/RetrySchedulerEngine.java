/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.biz.service;

import modelengine.fit.service.FitablesRegisteredObserver;
import modelengine.fit.waterflow.common.utils.GlobalExecutorUtil;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowretry.FlowRetryRepo;
import modelengine.fit.waterflow.service.FlowRuntimeService;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Value;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.schedule.ExecutePolicy;
import modelengine.fitframework.schedule.Task;

import java.time.Instant;

/**
 * 以定时任务方式重试流程可重试状态上下文
 *
 * @author 李哲峰
 * @since 2024/2/2
 */
public class RetrySchedulerEngine implements FitablesRegisteredObserver {
    private static final Logger log = Logger.get(RetrySchedulerEngine.class);

    private final FlowRetryService flowRetryService;

    private final long scheduleRate;

    private final FlowRetryRepo retryRepo;

    public RetrySchedulerEngine(FlowRetryService flowRetryService,
                                @Value("${jane.flowsEngine.retry.scheduleRate}") long scheduleRate,
                                FlowRetryRepo retryRepo) {
        this.flowRetryService = flowRetryService;
        this.scheduleRate = scheduleRate;
        this.retryRepo = retryRepo;
    }

    /**
     * 定时检测是否有重试数据
     */
    @Override
    public void onFitablesRegistered() {
        Task retryTask = Task.builder()
                .runnable(this::retryCheck)
                .policy(ExecutePolicy.fixedRate(scheduleRate))
                .build();
        log.info("Start scheduling retry tasks");
        GlobalExecutorUtil.getInstance().getSchedulerPool().schedule(retryTask, Instant.now());
    }

    private void retryCheck() {
        try {
            int retryCount = retryRepo.hasRetryData();
            if (retryCount == 0 || flowRetryService.isRetryRunning()) {
                return;
            }
            flowRetryService.retryTask();
        } catch (Exception ex) {
            log.error("retryCheck failed, exception: ", ex);
        }
    }
}
