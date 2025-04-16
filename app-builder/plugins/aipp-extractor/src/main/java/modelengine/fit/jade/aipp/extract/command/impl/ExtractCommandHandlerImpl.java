/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.command.impl;

import static modelengine.fit.jade.aipp.extract.utils.Constant.DESC_KEY;
import static modelengine.fit.jade.aipp.extract.utils.Constant.HISTORY_KEY;
import static modelengine.fit.jade.aipp.extract.utils.Constant.TEXT_KEY;
import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fel.core.chat.ChatOption;
import modelengine.fel.core.memory.Memory;
import modelengine.fel.core.model.http.SecureConfig;
import modelengine.fit.jade.aipp.extract.command.ContentExtractCommand;
import modelengine.fit.jade.aipp.extract.command.ExtractCommandHandler;
import modelengine.fit.jade.aipp.extract.domain.entity.ContentExtractor;
import modelengine.fit.jade.aipp.memory.AippMemoryFactory;
import modelengine.fit.jade.aipp.model.dto.ModelAccessInfo;
import modelengine.fit.jade.aipp.model.service.AippModelCenter;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.util.StringUtils;

import java.util.HashMap;

/**
 * 表示 {@link ExtractCommandHandler} 的默认实现。
 *
 * @author 何嘉斌
 * @since 2024-10-26
 */
@Component
public class ExtractCommandHandlerImpl implements ExtractCommandHandler {
    private final AippMemoryFactory memoryFactory;
    private final ContentExtractor contentExtractor;
    private final AippModelCenter aippModelCenter;

    /**
     * 创建 {@link ExtractCommandHandlerImpl} 的实例。
     *
     * @param memoryFactory 表示用于创建内存对象的 {@link AippMemoryFactory}。
     * @param contentExtractor 表示信息提取算子的 {@link ContentExtractor}。
     * @param aippModelCenter 表示用于获取模型的 {@link AippModelCenter}。
     */
    public ExtractCommandHandlerImpl(AippMemoryFactory memoryFactory, ContentExtractor contentExtractor,
            AippModelCenter aippModelCenter) {
        this.memoryFactory = notNull(memoryFactory, "The memory factory cannot be null.");
        this.contentExtractor = notNull(contentExtractor, "The extractor cannot be null.");
        this.aippModelCenter = notNull(aippModelCenter, "The model center cannot be null.");
    }

    @Override
    public Object handle(ContentExtractCommand command) {
        notNull(command, "The command cannot be null.");
        Memory memory = this.memoryFactory.create(command.getMemoryConfig(), command.getHistories());
        HashMap<String, String> variables = new HashMap<>();
        variables.put(TEXT_KEY, StringUtils.blankIf(command.getText(), StringUtils.EMPTY));
        variables.put(DESC_KEY, StringUtils.blankIf(command.getDesc(), StringUtils.EMPTY));
        variables.put(HISTORY_KEY, StringUtils.blankIf(memory.text(), StringUtils.EMPTY));
        ModelAccessInfo modelAccessInfo = this.aippModelCenter.getModelAccessInfo(command.getModelTag(),
                command.getModel(), null);
        ChatOption chatOption = ChatOption.custom()
                .model(command.getModel())
                .baseUrl(modelAccessInfo.getBaseUrl())
                .stream(false)
                .secureConfig(modelAccessInfo.isSystemModel() ? null : SecureConfig.custom().ignoreTrust(true).build())
                .apiKey(modelAccessInfo.getAccessKey())
                .temperature(command.getTemperature())
                .build();
        return this.contentExtractor.run(variables, command.getOutputSchema(), chatOption);
    }
}