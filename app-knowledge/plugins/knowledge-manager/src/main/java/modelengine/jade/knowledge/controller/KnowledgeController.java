/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.jade.knowledge.controller;

import modelengine.fit.http.annotation.GetMapping;
import modelengine.fit.http.annotation.RequestBean;
import modelengine.fit.http.annotation.RequestMapping;
import modelengine.fit.http.annotation.RequestParam;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.broker.client.BrokerClient;
import modelengine.fitframework.broker.client.filter.route.FitableIdFilter;
import modelengine.fitframework.validation.Validated;
import modelengine.jade.authentication.context.UserContextHolder;
import modelengine.fel.tool.model.transfer.ToolGroupData;
import modelengine.fel.tool.service.ToolGroupService;
import modelengine.jade.common.vo.PageVo;
import modelengine.jade.knowledge.KnowledgeI18nInfo;
import modelengine.jade.knowledge.KnowledgeI18nService;
import modelengine.jade.knowledge.KnowledgeProperty;
import modelengine.jade.knowledge.KnowledgeRepo;
import modelengine.jade.knowledge.KnowledgeRepoService;
import modelengine.jade.knowledge.ListRepoQueryParam;
import modelengine.jade.knowledge.SchemaItem;
import modelengine.jade.knowledge.controller.entity.KnowledgeRepoInfo;
import modelengine.jade.knowledge.controller.vo.KnowledgePropertyVo;
import modelengine.jade.knowledge.enums.IndexType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库服务接口。
 *
 * @author 邱晓霞
 * @since 2024-09-29
 */
@Component
@RequestMapping(path = {"/knowledge-manager"})
public class KnowledgeController {
    /**
     * 默认知识库
     */
    private static final String DEFAULT_KNOWLEDGE = "default";

    /**
     * 默认知识库名称
     */
    private static final String DEFAULT_KNOWLEDGE_NAME = "EDM内置知识库";

    /**
     * 默认知识库描述
     */
    private static final String DEFAULT_KNOWLEDGE_DESC = "支持语检索EDM知识库";

    private final KnowledgeI18nService knowledgeI18nService;

    private final ToolGroupService toolGroupService;

    private final BrokerClient brokerClient;

    private final Map<String, KnowledgeRepoInfo> repoInfoMap = new HashMap<String, KnowledgeRepoInfo>();

    /**
     * 表示 {@link KnowledgeController} 的构造器。
     *
     * @param knowledgeI18nService 表示获取知识库国际化服务的 {@link KnowledgeI18nService}。
     * @param toolGroupService 工具组服务
     * @param brokerClient fit的调度器
     */
    public KnowledgeController(KnowledgeI18nService knowledgeI18nService, ToolGroupService toolGroupService,
            BrokerClient brokerClient) {
        this.knowledgeI18nService = knowledgeI18nService;
        this.toolGroupService = toolGroupService;
        this.brokerClient = brokerClient;
        this.repoInfoMap.put(DEFAULT_KNOWLEDGE,
                new KnowledgeRepoInfo(DEFAULT_KNOWLEDGE, DEFAULT_KNOWLEDGE_NAME, DEFAULT_KNOWLEDGE_DESC));
        // 暂时保留用于演示，待store提供group的name和desc后删除
        this.repoInfoMap.put("food", new KnowledgeRepoInfo("food", "美食知识库", "支持检索美食相关的知识"));
        this.repoInfoMap.put("cat", new KnowledgeRepoInfo("cat", "猫的知识库", "支持检索猫相关的知识"));
    }

    /**
     * 查询知识库列表。
     *
     * @param groupId 表示调用的知识库服务的唯一标识的 {@link String}。
     * @param param 表示查询参数的 {@link ListRepoQueryParam}。
     * @return 表示知识库分页结果的 {@link PageVo}{@code <}{@link KnowledgeRepo}{@code >}。
     */
    @GetMapping("/list/repos")
    public PageVo<KnowledgeRepo> getRepoList(@RequestParam(value = "groupId", required = false) String groupId,
            @RequestBean @Validated ListRepoQueryParam param) {
        return this.brokerClient.getRouter(KnowledgeRepoService.class, KnowledgeRepoService.GENERICABLE_LIST_REPOS)
                .route(new FitableIdFilter(groupId))
                .invoke(UserContextHolder.get().getName(), param);
    }

    /**
     * 查询知识库组标识列表。
     *
     * @return 表示知识库组标识列表的 {@link List}{@code <}{@link String}{@code >}。
     */
    @GetMapping("/list/groups")
    public List<KnowledgeRepoInfo> getRepoInfo() {
        List<ToolGroupData> defGroupKnowledgeList = this.toolGroupService.get(
                KnowledgeRepoService.STORE_DEF_GROUP_KNOWLEDGE);
        List<KnowledgeRepoInfo> result = new ArrayList<>();
        result.add(this.repoInfoMap.get(DEFAULT_KNOWLEDGE));
        defGroupKnowledgeList.stream()
                .filter(knowledge -> !DEFAULT_KNOWLEDGE.equals(knowledge.getName()))
                .map(knowledge -> this.repoInfoMap.get(knowledge.getName()))
                .forEach(result::add);
        return result;
    }

    /**
     * 查询知识库支持的检索参数信息。
     *
     * @param groupId 表示调用的知识库服务的唯一标识的 {@link String}。
     * @return 表示检索参数信息的 {@link KnowledgeProperty}。
     */
    @GetMapping("/properties")
    public KnowledgePropertyVo getProperty(@RequestParam(value = "groupId", required = false) String groupId) {
        KnowledgeProperty property = this.brokerClient.getRouter(KnowledgeRepoService.class,
                        KnowledgeRepoService.GENERICABLE_GET_PROPERTY)
                .route(new FitableIdFilter(groupId))
                .invoke(UserContextHolder.get().getName());
        Set<String> enableIndexType = property.indexType().stream().map(SchemaItem::type).collect(Collectors.toSet());
        List<KnowledgeProperty.IndexInfo> disableIndexType = Arrays.stream(IndexType.values())
                .filter(type -> !enableIndexType.contains(type.value()))
                .map(type -> {
                    KnowledgeI18nInfo i18nInfo = this.knowledgeI18nService.localizeText(type);
                    return new KnowledgeProperty.IndexInfo(type, i18nInfo.getName(), i18nInfo.getDescription());
                })
                .collect(Collectors.toList());
        return new KnowledgePropertyVo(disableIndexType, property.indexType(), property.filterConfig(),
                property.rerankConfig());
    }
}