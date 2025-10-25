/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowcontext;

import lombok.RequiredArgsConstructor;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowContextMapper;
import modelengine.fit.waterflow.flowsengine.persist.po.FlowContextPO;
import modelengine.fitframework.annotation.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static modelengine.fit.waterflow.common.Constant.STREAM_ID_SEPARATOR;

/**
 * 查询流程上下文核心类
 *
 * @author 杨祥宇
 * @since 2023/10/17
 */
@Component
@RequiredArgsConstructor
public class QueryFlowContextPersistRepo {
    private final FlowContextMapper flowContextMapper;

    /**
     * findByStreamIdList
     *
     * @param streamIds streamIds
     * @return List<FlowContextPO>
     */
    public List<FlowContextPO> findByStreamIdList(List<String> streamIds) {
        return flowContextMapper.findByStreamIdList(streamIds);
    }

    /**
     * pageQueryContextByStreamId
     *
     * @param streamId streamId
     * @param limit limit
     * @param offset offset
     * @return List<FlowContextPO>
     */
    public List<FlowContextPO> pageQueryContextByStreamId(String streamId, int limit, long offset) {
        return flowContextMapper.pageQueryByStreamId(streamId, limit, offset);
    }

    /**
     * getTotalByStreamId
     *
     * @param streamId streamId
     * @return int
     */
    public int getTotalByStreamId(String streamId) {
        return flowContextMapper.getTotalByStreamId(streamId);
    }

    /**
     * findByContextIdList
     *
     * @param contextIds contextIds
     * @return List<FlowContextPO>
     */
    public List<FlowContextPO> findByContextIdList(List<String> contextIds) {
        if (contextIds == null || contextIds.isEmpty()) {
            return new ArrayList<>();
        }
        return flowContextMapper.findByContextIdList(contextIds);
    }

    /**
     * findWithFlowDataByContextIdList
     *
     * @param contextIds contextIds
     * @return List<FlowContextPO>
     */
    public List<FlowContextPO> findWithFlowDataByContextIdList(List<String> contextIds) {
        return flowContextMapper.findWithoutFlowDataByContextIdList(contextIds);
    }

    /**
     * findByTransIdList
     *
     * @param transIds transIds
     * @return List<FlowContextPO>
     */
    public List<FlowContextPO> findByTransIdList(List<String> transIds) {
        if (transIds.size() == 0) {
            return Collections.emptyList();
        }
        return flowContextMapper.findByTransIdList(transIds);
    }

    /**
     * findWithoutFlowDataByTransIdList
     *
     * @param transIds transIds
     * @return List<FlowContextPO>
     */
    public List<FlowContextPO> findWithoutFlowDataByTransIdList(List<String> transIds) {
        if (transIds.size() == 0) {
            return Collections.emptyList();
        }
        return flowContextMapper.findWithoutFlowDataByTransIdList(transIds);
    }

    /**
     * 根据traceId查询数据
     *
     * @param traceId traceId
     * @return 返回traceId下的context信息
     */
    public List<FlowContextPO> findWithoutFlowDataByTraceId(String traceId) {
        return flowContextMapper.findWithoutFlowDataByTraceId(traceId);
    }

    /**
     * findUnarchivedContextCountByMetaId
     *
     * @param metaId metaId
     * @param version version
     * @return Integer
     */
    public Integer findUnarchivedContextCountByMetaId(String metaId, String version) {
        String streamId = metaId + STREAM_ID_SEPARATOR + version;
        return flowContextMapper.findUnarchivedContextCountByMetaId(streamId);
    }
}
