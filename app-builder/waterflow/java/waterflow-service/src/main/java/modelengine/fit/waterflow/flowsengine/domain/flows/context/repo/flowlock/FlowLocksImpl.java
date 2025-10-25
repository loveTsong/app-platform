/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowlock;

import modelengine.fit.waterflow.domain.context.repo.flowlock.FlowLocks;
import modelengine.fit.waterflow.domain.context.repo.flowlock.FlowLocksMemo;
import modelengine.fit.waterflow.spi.lock.DistributedLockProvider;
import modelengine.fitframework.annotation.Alias;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Value;
import modelengine.fitframework.ioc.BeanContainer;

import java.util.concurrent.locks.Lock;

/**
 * 流程锁，数据库版本的实现
 *
 * @author 李哲峰
 * @since 2023/11/30
 */
@Component
@Alias("flowLocksDatabase")
public class FlowLocksImpl extends FlowLocksMemo {
    private final DistributedLockProvider distributedLockProvider;

    public FlowLocksImpl(@Value("${distributed-lock-provider}") String providerAlias, BeanContainer beanContainer)
            throws IllegalAccessException {
        this.distributedLockProvider =
                beanContainer.lookup(providerAlias).orElseThrow(IllegalAccessException::new).get();
    }

    /**
     * 获取分布式锁
     *
     * @param key 获取分布式锁的key值，一般是prefix-streamID-eventId
     * 比如key值为：flow-event-streamId-eventId; flow-node-streamId-eventId
     * @return {@link Lock} 锁对象
     */
    @Override
    public Lock getDistributeLock(String key) {
        return distributedLockProvider.get(key);
    }
}
