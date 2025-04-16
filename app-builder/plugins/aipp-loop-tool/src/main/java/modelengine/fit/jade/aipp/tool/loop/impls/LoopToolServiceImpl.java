/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.tool.loop.impls;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import modelengine.fit.jade.aipp.tool.loop.LoopToolService;
import modelengine.fit.jade.aipp.tool.loop.dependencies.ToolCallService;
import modelengine.fit.jade.aipp.tool.loop.entities.Config;
import modelengine.fit.jade.aipp.tool.loop.entities.ToolInfo;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Fitable;
import modelengine.fitframework.annotation.Property;
import modelengine.fitframework.util.CollectionUtils;
import modelengine.fitframework.util.ObjectUtils;
import modelengine.fel.tool.annotation.Attribute;
import modelengine.fel.tool.annotation.Group;
import modelengine.fel.tool.annotation.ToolMethod;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 循环工具的实现
 *
 * @author 夏斐
 * @since 2025/3/10
 */
@Component
@AllArgsConstructor
@Group(name = "LoopToolImpl")
public class LoopToolServiceImpl implements LoopToolService {
    private final ToolCallService toolCallService;

    @Override
    @Fitable("default")
    @ToolMethod(name = "loopToolDefault", description = "用于循环执行工具", extensions = {
            @Attribute(key = "tags", value = "FIT")
    })
    @Property(description = "循环执行工具的结果")
    public List<Object> loopTool(Map<String, Object> loopArgs, Config config, ToolInfo toolInfo) {
        if (CollectionUtils.isEmpty(config.getLoopKeys())) {
            throw new IllegalArgumentException("no loop key!");
        }
        String loopKey = config.getLoopKeys().get(0);

        Object value = loopArgs;
        String lastKey = null;
        Map<String, Object> lastMap = loopArgs;
        for (String key : loopKey.split("\\.")) {
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("loop value wrong!");
            } else {
                lastKey = key;
                lastMap = ObjectUtils.cast(value);
                value = lastMap.get(key);
            }
        }
        Object loopData = value;
        if (!(loopData instanceof List<?>)) {
            throw new IllegalArgumentException("input value of [" + loopKey + "] is not an array!");
        }
        return this.loopCall(loopArgs, toolInfo, loopKey, (List<?>) loopData, lastMap, lastKey);
    }

    private List<Object> loopCall(Map<String, Object> loopArgs, ToolInfo toolInfo, String loopKey, List<?> loopData,
            Map<String, Object> lastMap, String lastKey) {
        return loopData.stream().map(item -> {
            lastMap.put(lastKey, item);
            Map<String, Object> toolArgs = toolInfo.getParams()
                    .stream()
                    .collect(Collectors.toMap(param -> param.getName(), param -> loopArgs.get(param.getName())));
            // 循环展开的参数，通过序列化的方式复制，防止同进程调用场景下，直接返回时，多条数据的覆盖污染问题
            Map<String, Object> args = JSONObject.parseObject(JSONObject.toJSONString(toolArgs),
                    toolArgs.getClass());
            return this.toolCallService.call(toolInfo.getUniqueName(), args);
        }).collect(Collectors.toList());
    }
}