/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.tool;

import modelengine.fitframework.annotation.Genericable;

import java.util.Map;

/**
 * 同步工具调用接口
 *
 * @author 夏斐
 * @since 2025/3/12
 */
public interface SyncToolCall {
    /**
     * 支持携带工具上下文的调用。
     *
     * @param uniqueName 工具唯一标识。
     * @param toolArgs 工具调用参数。
     * @param toolContext 工具调用上下文。
     * @return 工具执行结果。
     */
    @Genericable("modelengine.jober.aipp.tool.sync.call")
    String call(String uniqueName, String toolArgs, Map<String, Object> toolContext);
}
