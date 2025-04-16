/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.memory.support;

import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fel.core.chat.ChatMessage;
import modelengine.fel.core.template.BulkStringTemplate;
import modelengine.fel.core.tokenizer.Tokenizer;
import modelengine.fit.jade.aipp.memory.AippChatRound;

import java.util.Collections;
import java.util.List;

/**
 * 用分词数设置滑动窗口大小的历史记录。
 *
 * @author 邱晓霞
 * @since 2024-09-20
 */
public class AippTokenWindowMemory extends AbstractAippChatMemory {
    private final List<AippChatRound> histories;

    /**
     * 构造历史记录。
     *
     * @param histories 表示历史记录列表的 {@link List}{@code <}{@link AippChatRound}{@code >}。
     * @param maxTokenCount 表示可记录的最大分词数的 {@code int}。
     * @param template 表示模板的 {@link BulkStringTemplate}。
     * @param tokenizer 表示分词器的 {@link Tokenizer}。
     */
    public AippTokenWindowMemory(List<AippChatRound> histories, int maxTokenCount, BulkStringTemplate template,
            Tokenizer tokenizer) {
        super(template);
        this.histories = filterWithMaxTokenCount(histories, maxTokenCount, tokenizer);
    }

    private static List<AippChatRound> filterWithMaxTokenCount(List<AippChatRound> histories, int maxTokenCount,
            Tokenizer tokenizer) {
        notNull(histories, "The histories cannot be null.");
        notNull(tokenizer, "The tokenizer cannot be null.");
        int tokenCount = 0;
        int index = histories.size();
        while (tokenCount <= maxTokenCount && --index >= 0) {
            AippChatRound chatRound = histories.get(index);
            tokenCount += tokenizer.countToken(chatRound.getQuestion()) + tokenizer.countToken(chatRound.getAnswer());
        }
        if (index >= 0) {
            return histories.subList(index + 1, histories.size());
        }
        return histories;
    }

    @Override
    protected List<AippChatRound> getAvailableHistories() {
        return Collections.unmodifiableList(this.histories);
    }
}
