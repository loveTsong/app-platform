/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.entity.support;

import static modelengine.fit.jade.aipp.extract.code.ContentExtractRetCode.DESERIALIZE_ERROR;
import static modelengine.fit.jade.aipp.extract.code.ContentExtractRetCode.MODEL_RESPONSE_ERROR;
import static modelengine.fit.jade.aipp.extract.code.ContentExtractRetCode.TOOLCALL_SIZE_ERROR;
import static modelengine.fit.jade.aipp.extract.utils.Constant.BUILTIN_PROMPT;
import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fel.core.chat.ChatMessage;
import modelengine.fel.core.chat.ChatModel;
import modelengine.fel.core.chat.ChatOption;
import modelengine.fel.core.chat.support.ChatMessages;
import modelengine.fel.core.chat.support.HumanMessage;
import modelengine.fel.core.template.support.DefaultStringTemplate;
import modelengine.fel.core.tool.ToolCall;
import modelengine.fel.core.tool.ToolInfo;
import modelengine.fit.jade.aipp.extract.domain.entity.ContentExtractor;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Fit;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.serialization.ObjectSerializer;
import modelengine.fitframework.serialization.SerializationException;
import modelengine.fitframework.util.IoUtils;
import modelengine.jade.common.exception.ModelEngineException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示 {@link ContentExtractor} 的工具调用实现。
 *
 * @author 易文渊
 * @author 何嘉斌
 * @since 2024-10-24
 */
@Component
public class FunctionCallContentExtractor implements ContentExtractor {
    private static final Logger log = Logger.get(FunctionCallContentExtractor.class);
    private static final int MODEL_MESSAGE_AMOUNT = 1;

    private final String builtinPrompt;
    private final ChatModel modelService;
    private final ObjectSerializer serializer;

    /**
     * 创建 {@link ContentExtractor} 的实例。
     *
     * @param modelService 表示模型服务的 {@link ChatModel}。
     * @param serializer 表示序列化器的 {@link ObjectSerializer}。
     * @throws IllegalArgumentException 当 {@code modelService} 为 {@code null} 时。
     * @throws IOException 当无法读取内置提示模板文件时。
     */
    public FunctionCallContentExtractor(ChatModel modelService, @Fit(alias = "json") ObjectSerializer serializer)
            throws IOException {
        this.modelService = notNull(modelService, "The model service cannot be null.");
        this.builtinPrompt = IoUtils.content(FunctionCallContentExtractor.class, BUILTIN_PROMPT);
        this.serializer = notNull(serializer, "The serializer cannot be null.");
    }

    @Override
    public Object run(Map<String, String> variables, String outputSchema, ChatOption chatOption) {
        notNull(variables, "The extracting variables cannot be null");
        String prompt = new DefaultStringTemplate(this.builtinPrompt).render(variables);
        ToolInfo tool = ToolInfo.custom()
                .namespace("")
                .name("request_tool")
                .description("需要执行的函数")
                .parameters(toJson(outputSchema))
                .extensions(new HashMap<>())
                .build();
        ChatOption option = ChatOption.custom(chatOption).stream(false).tools(Collections.singletonList(tool)).build();
        ChatMessages chatMessages = new ChatMessages();
        chatMessages.add(new HumanMessage(prompt));
        List<ChatMessage> answer = this.modelService.generate(chatMessages, option).blockAll();
        if (answer.isEmpty()) {
            log.error(MODEL_RESPONSE_ERROR.getMsg());
            throw new ModelEngineException(MODEL_RESPONSE_ERROR);
        }
        ChatMessage message = answer.get(0);
        List<ToolCall> toolCalls = message.toolCalls();
        if (toolCalls.size() != MODEL_MESSAGE_AMOUNT) {
            log.error(TOOLCALL_SIZE_ERROR.getMsg(), toolCalls.size(), MODEL_MESSAGE_AMOUNT);
            throw new ModelEngineException(TOOLCALL_SIZE_ERROR, toolCalls.size(), MODEL_MESSAGE_AMOUNT);
        }
        return toJson(toolCalls.get(0).arguments());
    }

    private Map<String, Object> toJson(String jsonString) {
        try {
            return this.serializer.deserialize(jsonString, Map.class);
        } catch (SerializationException ex) {
            log.error(DESERIALIZE_ERROR.getMsg(), jsonString);
            throw new ModelEngineException(DESERIALIZE_ERROR, jsonString);
        }
    }
}