/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jober.aipp.fel;

import modelengine.fel.core.chat.ChatMessage;
import modelengine.fel.core.chat.ChatModel;
import modelengine.fel.core.chat.ChatOption;
import modelengine.fel.core.chat.Prompt;
import modelengine.fel.core.chat.support.ChatMessages;
import modelengine.fel.core.chat.support.FlatChatMessage;
import modelengine.fel.core.chat.support.ToolMessage;
import modelengine.fel.core.tool.ToolCall;
import modelengine.fel.engine.operators.models.ChatFlowModel;
import modelengine.fel.engine.operators.patterns.AbstractAgent;
import modelengine.fit.jade.tool.SyncToolCall;
import modelengine.fit.jober.aipp.constants.AippConst;
import modelengine.fit.waterflow.domain.context.StateContext;
import modelengine.fitframework.annotation.Fit;
import modelengine.fitframework.inspection.Validation;
import modelengine.fitframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WaterFlow 场景的 {@link AbstractAgent} 实现。
 *
 * @author 刘信宏
 * @since 2024-06-04
 */
public class WaterFlowAgent extends AbstractAgent {
    private static final String AGENT_MSG_KEY = "water_flow_agent_request";

    private static final String GOTO_NODE_ID = "ahead_llm_node";

    // private final ToolProvider toolProvider;

    // private final ChatFlowModel model;

    private final String agentMsgKey;

    private final SyncToolCall syncToolCall;

    // ToolProvider toolProvider,
    public WaterFlowAgent(@Fit SyncToolCall syncToolCall, ChatModel chatStreamModel, ChatOption option) {
        super(new ChatFlowModel(chatStreamModel, option));
        // this.toolProvider = Validation.notNull(toolProvider, "The tool provider cannot be null.");
        // this.model = new ChatFlowModel(chatStreamModel, option);
        this.syncToolCall =  Validation.notNull(syncToolCall, "The tool sync tool call cannot be null.");
        this.agentMsgKey = AGENT_MSG_KEY;
    }

    @Override
    protected Prompt doToolCall(List<ToolCall> toolCalls, StateContext ctx) {
        Validation.notNull(ctx, "The state context cannot be null.");
        Map<String, Object> toolContext = ObjectUtils.getIfNull(ctx.getState(AippConst.TOOL_CONTEXT_KEY),
                Collections::emptyMap);
        return toolCalls.stream()
                .map(toolCall -> (ChatMessage) new ToolMessage(toolCall.id(),
                        this.syncToolCall.call(toolCall.name(), toolCall.arguments(), toolContext)))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ChatMessages::from));
    }

    // @Override
    // protected AiProcessFlow<Prompt, ChatMessage> buildFlow() {
    //     return AiFlows.<Prompt>create()
    //             .just((input, ctx) -> ctx.setState(this.agentMsgKey, ChatMessages.from(input.messages())))
    //             .id(GOTO_NODE_ID)
    //             .generate(chatModel)
    //             .reduce(ChatChunk::new, AbstractAgent::defaultReduce)
    //             .delegate(this::handleTool)
    //             .conditions()
    //             .matchTo(this::shouldRepeated, node -> node.map(this::getAgentMsg).to(GOTO_NODE_ID))
    //             .others(node -> node.map(this::getAgentMsg))
    //             .close();
    // }


    private ChatMessage handleTool(ChatMessage input, StateContext ctx) {
        Validation.notNull(ctx, "The state context cannot be null.");

        Map<String, Object> toolContext = ObjectUtils.getIfNull(ctx.getState(AippConst.TOOL_CONTEXT_KEY),
                Collections::emptyMap);
        ChatMessages lastRequest = ctx.getState(this.agentMsgKey);
        lastRequest.add(Validation.notNull(input, "The input message cannot be null."));
        input.toolCalls().forEach(toolCall -> {
            lastRequest.add(FlatChatMessage.from(new ToolMessage(toolCall.id(),
                    this.syncToolCall.call(toolCall.name(), toolCall.arguments(), toolContext))));
        });
        // lastRequest.addAll(AbstractAgent.toolCallHandle(this.toolProvider, input, toolContext).messages());
        return input;
    }

    // private boolean shouldRepeated(ChatMessage message) {
    //     Validation.notNull(message, "The message cannot be null.");
    //     // boolean repeat = CollectionUtils.isNotEmpty(message.toolCalls()) && !this.containAsyncTool(message);
    //     boolean repeat = CollectionUtils.isNotEmpty(message.toolCalls()) && !this.containAsyncTool(message) && !message.text().contains("</final>");
    //     // System.out.println("repeat: " + repeat + ". " + JSONObject.toJSONString(message.toolCalls()));
    //     return repeat;
    // }

    // private boolean containAsyncTool(ChatMessage message) {
    //     List<String> toolsName = Optional.ofNullable(message.toolCalls())
    //             .map(m -> m.stream().map(ToolCall::name).collect(Collectors.toList()))
    //             .orElseGet(Collections::emptyList);
    //     if (toolsName.isEmpty()) {
    //         return false;
    //     }
    //     return this.toolProvider.getTool(toolsName).stream().anyMatch(tool -> {
    //         Map<String, Object> context = tool.extensions();
    //         if (MapUtils.isEmpty(context)) {
    //             return false;
    //         }
    //         Object isAsyncValue = context.get("isAsync");
    //         if (isAsyncValue == null) {
    //             return false;
    //         }
    //         return Boolean.parseBoolean(isAsyncValue.toString());
    //     });
    // }
    //
    // private Prompt getAgentMsg(ChatMessage input, StateContext ctx) {
    //     Validation.notNull(ctx, "The state context cannot be null.");
    //     return ctx.getState(this.agentMsgKey);
    // }
}
