/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowcontext;

import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.domain.context.FlowSession;
import modelengine.fit.waterflow.domain.context.FlowTrace;
import modelengine.fit.waterflow.domain.context.TraceOwner;
import modelengine.fit.waterflow.domain.context.Window;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextMemoRepo;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextRepo;
import modelengine.fit.waterflow.domain.context.repo.flowtrace.FlowTraceRepo;
import modelengine.fit.waterflow.domain.enums.FlowNodeStatus;
import modelengine.fit.waterflow.domain.enums.FlowTraceStatus;
import modelengine.fit.waterflow.domain.stream.operators.Operators;
import modelengine.fit.waterflow.domain.utils.IdGenerator;
import modelengine.fit.waterflow.exceptions.WaterflowException;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowData;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.FlowRetry;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowretry.FlowRetryRepo;
import modelengine.fit.waterflow.flowsengine.persist.entity.FlowContextUpdateInfo;
import modelengine.fit.waterflow.flowsengine.persist.mapper.FlowContextMapper;
import modelengine.fit.waterflow.flowsengine.persist.po.FlowContextPO;
import modelengine.fitframework.annotation.Alias;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Value;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.util.CollectionUtils;
import modelengine.fitframework.util.ObjectUtils;
import modelengine.fitframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static modelengine.fit.waterflow.ErrorCodes.ENTITY_NOT_FOUND;
import static modelengine.fit.waterflow.common.Constant.CONTEXT_EXCLUSIVE_STATUS_MAP;
import static modelengine.fit.waterflow.common.Constant.RETRY_INTERVAL;
import static modelengine.fit.waterflow.common.Constant.STREAM_ID_SEPARATOR;
import static modelengine.fit.waterflow.common.Constant.TO_BATCH_KEY;

/**
 * 持久化{@link FlowContext}对象到数据库核心类
 * 与{@link FlowContextMemoRepo}组成{@link FlowContextRepo}的不同实现
 *
 * @author 高诗意
 * @since 2023/08/14
 */
@Component
@Alias("flowContextPersistRepo")
public class FlowContextPersistRepo implements FlowContextRepo {
    private static final Logger log = Logger.get(FlowContextPersistRepo.class);

    private static final String TRACE_ID_SEPARATE = ", ";

    private static final String PASS_DATA = "system_key_pass_data";

    private final FlowContextMapper contextMapper;

    private final FlowTraceRepo traceRepo;

    private final FlowRetryRepo retryRepo;

    private final TraceOwner traceOwner;

    private final boolean useLimit;

    private final long maxRetryCount;

    private final Integer defaultLimitation;

    private final Map<String, FlowSession> contextSessions = new ConcurrentHashMap<>();

    public FlowContextPersistRepo(FlowContextMapper contextMapper, FlowTraceRepo traceRepo, FlowRetryRepo retryRepo,
            TraceOwner traceOwner, @Value("${modelengine.limit}") Integer limit,
            @Value("${modelengine.useLimit}") boolean hasUseLimit,
            @Value("${jane.flowsEngine.retry.maxCount}") long maxRetryCount) {
        this.traceOwner = traceOwner;
        this.useLimit = hasUseLimit;
        this.contextMapper = contextMapper;
        this.traceRepo = traceRepo;
        this.retryRepo = retryRepo;
        this.defaultLimitation = limit;
        this.maxRetryCount = maxRetryCount;
    }

    /**
     * convertTextToSet
     *
     * @param textData textData
     * @return Set<String>
     */
    public static Set<String> convertTextToSet(String textData) {
        if (textData == null || "".equals(textData)) {
            return new HashSet<>();
        }
        Set<String> resultSet = new HashSet<>();
        if (!textData.isEmpty()) {
            String[] items = textData.split(", ");
            Collections.addAll(resultSet, items);
        }
        return resultSet;
    }

    @Override
    public <T> List<FlowContext<T>> getContextsByPosition(String streamId, List<String> posIds, String status) {
        List<String> traceIds = this.traceOwner.getTraces();
        if (traceIds.isEmpty()) {
            log.warn("There is no trace owned.");
            return Collections.emptyList();
        }
        List<FlowContextPO> pos = contextMapper.findByPositions(streamId, posIds, status, traceIds);
        if (pos.isEmpty()) {
            log.info("[getContextsByPosition] Empty contexts. traceIds={}, pos={}.", StringUtils.join(',', traceIds),
                    StringUtils.join(',', posIds));
        }
        return pos.stream().map(this::serializer).map(ObjectUtils::<FlowContext<T>>cast).collect(Collectors.toList());
    }

    @Override
    public <T> List<FlowContext<T>> getContextsByPosition(String streamId, String posId, String batchId,
            String status) {
        List<FlowContextPO> pos = contextMapper.findByPositionWithBatchId(streamId, posId, batchId, status);
        return pos.stream().map(this::serializer).map(ObjectUtils::<FlowContext<T>>cast).collect(Collectors.toList());
    }

    @Override
    public <T> List<FlowContext<T>> findWithoutFlowDataByTraceId(String traceId) {
        return contextMapper.findWithoutFlowDataByTraceId(traceId)
                .stream()
                .map(this::serializerAsString)
                .map(ObjectUtils::<FlowContext<T>>cast)
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<FlowContext<T>> getContextsByTrace(String traceId) {
        FlowTrace trace = traceRepo.find(traceId);
        if (!Optional.ofNullable(trace).isPresent() || CollectionUtils.isEmpty(trace.getContextPool())) {
            return new ArrayList<>();
        }
        List<FlowContextPO> pos = contextMapper.findByContextIdList(new ArrayList<>(trace.getContextPool()));
        return pos.stream()
                .map(this::serializerAsString)
                .map(ObjectUtils::<FlowContext<T>>cast)
                .collect(Collectors.toList());
    }

    @Override
    public <T> void save(List<FlowContext<T>> flowContexts) {
        if (CollectionUtils.isEmpty(flowContexts)) {
            return;
        }
        FlowContextPO flowContextPO = contextMapper.find(flowContexts.get(0).getId());
        List<FlowContextPO> flowContextPOS = flowContexts.stream()
                .map(ObjectUtils::<FlowContext<FlowData>>cast)
                .peek(context -> {
                    if (flowContextPO == null) {
                        if (context.getStatus().isRunningStatus()) {
                            this.contextSessions.putIfAbsent(context.getId(), context.getSession());
                        }
                    }
                })
                .map(this::serializer)
                .collect(Collectors.toList());
        if (flowContextPO == null) {
            contextMapper.batchCreate(flowContextPOS);

        } else {
            batchUpdate(flowContextPOS);
        }
    }

    private void batchUpdate(List<FlowContextPO> flowContextPOS) {
        contextMapper.batchUpdate(flowContextPOS);
    }

    @Override
    public <T> void update(List<FlowContext<T>> contexts) {
        List<FlowContextPO> flowContextPOS = contexts.stream()
                .map(ObjectUtils::<FlowContext<FlowData>>cast)
                .map(this::serializer)
                .collect(Collectors.toList());
        batchUpdate(flowContextPOS);
    }

    @Override
    public <T> void updateToSent(List<FlowContext<T>> contexts) {
        contexts.forEach(context -> this.contextSessions.remove(context.getId()));
        contextMapper.updateToSent(contexts.stream().map(IdGenerator::getId).collect(Collectors.toList()));
    }

    @Override
    public <T> void updateFlowDataAndToBatch(List<FlowContext<T>> contexts) {
        List<FlowContextPO> flowContextPOS = contexts.stream()
                .map(ObjectUtils::<FlowContext<FlowData>>cast)
                .map(this::serializer)
                .collect(Collectors.toList());
        this.contextMapper.updateFlowDataAndToBatch(flowContextPOS);
    }

    @Override
    public <T> void updateFlowData(Map<String, T> flowDataList) {
        this.contextMapper.updateFlowData(flowDataList.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry<String, T>::getKey,
                        entry -> ObjectUtils.<FlowData>cast(entry.getValue()).translateToJson())));
    }

    @Override
    public <T> void updateFlowData(List<FlowContext<T>> flowContexts) {
        this.contextMapper.updateFlowData(flowContexts.stream()
                .collect(Collectors.toMap(FlowContext<T>::getId,
                        context -> ObjectUtils.<FlowData>cast(context).translateToJson())));
    }

    @Override
    public <T> void updateStatus(List<FlowContext<T>> contexts, String status, String position) {
        List<String> ids = contexts.stream().map(IdGenerator::getId).collect(Collectors.toList());
        String toBatch = contexts.get(0).getToBatch();
        LocalDateTime updateAt = LocalDateTime.now();
        LocalDateTime archivedAt = status.equals(FlowNodeStatus.ARCHIVED.toString()) ? updateAt : null;
        if (FlowNodeStatus.ARCHIVED.toString().equals(status) || FlowNodeStatus.ERROR.toString().equals(status)
                || FlowNodeStatus.TERMINATE.toString().equals(status)) {
            contexts.forEach(context -> this.contextSessions.remove(context.getId()));
        }
        contextMapper.updateStatusAndPosition(ids,
                new FlowContextUpdateInfo(toBatch, status, position, updateAt, archivedAt),
                CONTEXT_EXCLUSIVE_STATUS_MAP.get(status));
    }

    @Override
    public void updateToTerminated(List<String> traceIds) {
        List<FlowContext<String>> contexts = getContextsByTrace(traceIds.get(0));
        List<String> ids = contexts.stream().map(IdGenerator::getId).collect(Collectors.toList());
        String status = FlowTraceStatus.TERMINATE.toString();
        contexts.forEach(context -> this.contextSessions.remove(context.getId()));
        contextMapper.updateStatusAndPosition(ids,
                new FlowContextUpdateInfo(status, contexts.get(0).getPosition(), LocalDateTime.now(), null),
                CONTEXT_EXCLUSIVE_STATUS_MAP.get(status));

        traceRepo.updateStatus(traceIds, status);
    }

    @Override
    public boolean isTracesTerminate(List<String> traceIds) {
        return traceRepo.getByIds(traceIds)
                .stream()
                .anyMatch(flowTrace -> FlowTraceStatus.TERMINATE.equals(flowTrace.getStatus()));
    }

    @Override
    public <T> void updateIndex(List<FlowContext<T>> flowContexts) {

    }

    @Override
    public <T> List<FlowContext<T>> getContextsByParallel(String parallelId) {
        return new ArrayList<>();
    }

    @Override
    public <T> FlowContext<T> getById(String id) {
        return Optional.ofNullable(contextMapper.find(id))
                .map(this::serializer)
                .map(ObjectUtils::<FlowContext<T>>cast)
                .orElseThrow(() -> {
                    log.error("Cannot find flow context by ID {}.", id);
                    return new WaterflowException(ENTITY_NOT_FOUND, "FlowContext", id);
                });
    }

    @Override
    public <T> List<FlowContext<T>> getByToBatch(List<String> toBatchIds) {
        if (toBatchIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<FlowContextPO> pos = contextMapper.findByToBatch(toBatchIds);
        return pos.stream().map(this::serializer).map(ObjectUtils::<FlowContext<T>>cast).collect(Collectors.toList());
    }

    @Override
    public <T> List<FlowContext<T>> getPendingAndSentByIds(List<String> ids) {
        return contextMapper.findByContextIdList(ids)
                .stream()
                .filter(p -> p.getStatus().equals(FlowNodeStatus.PENDING.toString()))
                .filter(FlowContextPO::isSent)
                .map(this::serializer)
                .map(ObjectUtils::<FlowContext<T>>cast)
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<FlowContext<T>> getByIds(List<String> ids) {
        return contextMapper.findByContextIdList(ids)
                .stream()
                .map(this::serializer)
                .map(ObjectUtils::<FlowContext<T>>cast)
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<FlowContext<T>> requestMappingContext(String streamId, List<String> subscriptions,
            Map<String, Integer> sessions) {
        List<String> traces = this.traceOwner.getTraces();
        List<FlowContextPO> pos = contextMapper.findBySubscriptions(streamId, subscriptions,
                FlowNodeStatus.PENDING.toString(), traces);
        List<FlowContext<FlowData>> all = pos.stream().map(this::serializer).toList();
        return all.stream()
                .filter(context -> {
                    boolean found = false;
                    for (String s : sessions.keySet()) {
                        found = context.getSession().getId().equals(s)
                                && (Objects.equals(context.getIndex(), sessions.get(s)));
                        if (found) {
                            break;
                        }
                    }
                    // 找到需要保序的当前序列或者不需要保序的
                    return context.getIndex() == -1 || context.getIndex() == 0 || found;
                })
                .limit(1)
                .map(ObjectUtils::<FlowContext<T>>cast)
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<FlowContext<T>> requestProducingContext(String streamId, List<String> subscriptions,
            Operators.Filter<T> filter) {
        List<FlowContextPO> pos;
        List<String> traces = this.traceOwner.getTraces();
        if (traces.isEmpty()) {
            log.warn("There is no trace owned.");
            return Collections.emptyList();
        }
        if (useLimit) {
            pos = contextMapper.findSomeBySubscriptions(streamId, subscriptions, FlowNodeStatus.PENDING.toString(),
                    traces, defaultLimitation);
        } else {
            pos = contextMapper.findBySubscriptions(streamId, subscriptions, FlowNodeStatus.PENDING.toString(), traces);
        }
        List<FlowContext<T>> result = filter.process(pos.stream()
                .map(this::serializer)
                .map(ObjectUtils::<FlowContext<T>>cast)
                .collect(Collectors.toList()));
        if (result.isEmpty()) {
            log.info("[requestProducingContext] Empty contexts. traceIds={}, pos={}, beforeSize={}, afterSize={}.",
                    StringUtils.join(',', traces), StringUtils.join(',', subscriptions), pos.size(), result.size());
        }
        return result;
    }

    @Override
    public <T> List<FlowContext<T>> findByStreamId(String metaId, String version) {
        String streamId = metaId + STREAM_ID_SEPARATOR + version;
        List<FlowContextPO> flowContextPOs = contextMapper.findByStreamId(streamId);
        return flowContextPOs.stream()
                .map(this::serializer)
                .map(ObjectUtils::<FlowContext<T>>cast)
                .collect(Collectors.toList());
    }

    @Override
    public Integer findRunningContextCountByMetaId(String metaId, String version) {
        String streamId = metaId + STREAM_ID_SEPARATOR + version;
        return contextMapper.findRunningContextCountByMetaId(streamId);
    }

    @Override
    public void delete(String metaId, String version) {
        String streamId = StringUtils.join(STREAM_ID_SEPARATOR, metaId, version);
        contextMapper.delete(streamId);
        traceRepo.delete(streamId);
    }

    @Override
    public <T> void updateContextPool(List<FlowContext<T>> after, Set<String> traces) {
        traceRepo.updateContextPool(new ArrayList<>(traces),
                after.stream().map(IdGenerator::getId).collect(Collectors.toList()));
    }

    @Override
    public <T> void save(FlowTrace trace, FlowContext<T> flowContext) {
        FlowContext<FlowData> realFlowContext = ObjectUtils.<FlowContext<FlowData>>cast(flowContext);
        trace.setOperator(realFlowContext.getData().getOperator());
        trace.setApplication(realFlowContext.getData().getApplication());
        trace.setStartTime(realFlowContext.getData().getStartTime());
        traceRepo.save(trace);
    }

    public FlowRetry getRetrySchedule(String entityId) {
        return retryRepo.getById(entityId);
    }

    public boolean isMaxRetryCount(String entityId) {
        if (maxRetryCount == 0) {
            return true;
        }
        FlowRetry flowRetry = retryRepo.getById(entityId);
        if (flowRetry == null || maxRetryCount == -1) {
            return false;
        }
        return flowRetry.getRetryCount() >= maxRetryCount;
    }

    public void createRetrySchedule(List<FlowRetry> flowRetryList) {
        retryRepo.save(flowRetryList);
    }

    public void updateRetrySchedule(List<String> entityIdList, LocalDateTime nextRetryTime) {
        retryRepo.updateNextRetryTime(entityIdList, nextRetryTime);
    }

    public void saveRetrySchedule(List<FlowContext<FlowData>> contexts) {
        String toBatch = contexts.get(0).getToBatch();
        FlowRetry flowRetry = this.getRetrySchedule(toBatch);
        if (flowRetry == null) {
            flowRetry = new FlowRetry(toBatch, TO_BATCH_KEY, LocalDateTime.now(), null, 0, 1);
            this.createRetrySchedule(Collections.singletonList(flowRetry));
        } else {
            LocalDateTime nextRetryTime = flowRetry.getLastRetryTime().plus(RETRY_INTERVAL, ChronoUnit.MILLIS);
            this.updateRetrySchedule(Collections.singletonList(flowRetry.getEntityId()), nextRetryTime);
        }
    }

    public void deleteRetryRecord(List<String> entityIdList) {
        retryRepo.delete(entityIdList);
    }

    private FlowContextPO serializer(FlowContext<FlowData> context) {
        String traceId = String.join(TRACE_ID_SEPARATE, context.getTraceId());
        context.getData().getContextData().put("flowTransId", context.getSession().getId());
        context.getData().getContextData().put("metaId", context.getId());
        context.getData().getBusinessData().put(PASS_DATA, context.getData().getPassData());
        context.getData().getContextData().put("contextId", context.getId());
        context.getData().getContextData().put("nodeMetaId", context.getPosition());
        context.getData().getContextData().put("flowTraceIds", new ArrayList<>(context.getTraceId()));
        FlowContextPO result = FlowContextPO.builder()
            .contextId(context.getId())
            .traceId(traceId)
            .transId(context.getSession().getId())
            .rootId(context.getRootId())
            .streamId(context.getStreamId())
            .flowData(context.getData().translateToJson())
            .positionId(context.getPosition())
            .status(context.getStatus().toString())
            .parallel(context.getParallel())
            .parallelMode(context.getParallelMode())
            .previous(context.getPrevious())
            .batchId(context.getBatchId())
            .toBatch(context.getToBatch())
            .sent(context.isSent())
            .createAt(context.getCreateAt())
            .updateAt(context.getUpdateAt())
            .archivedAt(context.getArchivedAt())
            .build();
        context.getData().getBusinessData().remove(PASS_DATA);
        return result;
    }

    private FlowContext<FlowData> serializer(FlowContextPO po) {
        Set<String> traceId = convertTextToSet(po.getTraceId());
        FlowContext<FlowData> context = new FlowContext<>(po.getStreamId(),
                po.getRootId(),
                getFlowData(po),
                traceId,
                po.getPositionId(),
                po.getParallel(),
                po.getParallelMode(),
                this.getFlowSession(po),
                LocalDateTime.now());
        convertOthers(po, context);
        return context;
    }

    private FlowContext<String> serializerAsString(FlowContextPO po) {
        Set<String> traceIds = convertTraceIds(po);
        FlowContext<String> context = new FlowContext<>(po.getStreamId(),
                po.getRootId(),
                po.getFlowData(),
                traceIds,
                po.getPositionId(),
                po.getParallel(),
                po.getParallelMode(),
                this.getFlowSession(po),
                LocalDateTime.now());
        convertOthers(po, context);
        return context;
    }

    private FlowSession getFlowSession(FlowContextPO po) {
        return this.contextSessions.computeIfAbsent(po.getContextId(), __ -> {
            FlowSession newSession = new FlowSession(po.getTransId());
            Window window = newSession.begin();
            window.createToken();
            window.complete();
            return newSession;
        });
    }

    private Set<String> convertTraceIds(FlowContextPO po) {
        Set<String> traceIds = new HashSet<>();
        if (StringUtils.isNotEmpty(po.getTraceId())) {
            Collections.addAll(traceIds, po.getTraceId().split(TRACE_ID_SEPARATE));
        }
        return traceIds;
    }

    private <T> void convertOthers(FlowContextPO po, FlowContext<T> context) {
        context.setId(po.getContextId());
        context.setPrevious(po.getPrevious());
        context.setStatus(FlowNodeStatus.valueOf(po.getStatus()));
        context.batchId(po.getBatchId());
        context.toBatch(po.getToBatch());
        context.setSent(po.isSent());
        context.setCreateAt(po.getCreateAt());
        context.setUpdateAt(po.getUpdateAt());
        context.setArchivedAt(po.getArchivedAt());
    }

    private FlowData getFlowData(FlowContextPO po) {
        FlowData flowData = FlowData.parseFromJson(po.getFlowData());
        flowData.setPassData(ObjectUtils.cast(flowData.getBusinessData().get(PASS_DATA)));
        flowData.getBusinessData().remove(PASS_DATA);
        return flowData;
    }

    /**
     * updateStatus
     *
     * @param contextId contextId
     * @param status status
     */
    public void updateStatus(List<String> contextId, FlowNodeStatus status) {
        contextMapper.updateStatus(contextId, status);
    }

    /**
     * 根据traceId查询所有上下文
     *
     * @param traceId traceId
     * @return 上下文集合
     */
    public List<FlowContext<FlowData>> findByTraceId(String traceId) {
        return contextMapper.findByTraceId(traceId).stream().map(this::serializer).collect(Collectors.toList());
    }

    /**
     * 根据traceId查询所有错误上下文
     *
     * @param traceId traceId
     * @return 错误上下文集合
     */
    public <T> List<FlowContext<T>> findErrorContextsByTraceId(String traceId) {
        return contextMapper.findErrorContextByTraceId(traceId)
                .stream()
                .map(this::serializer)
                .map(ObjectUtils::<FlowContext<T>>cast)
                .collect(Collectors.toList());
    }

    /**
     * 根据transId查询所有错误上下文
     *
     * @param transId transId
     * @return 错误上下文集合
     */
    public List<FlowContext<FlowData>> findErrorContextsByTransId(String transId) {
        return contextMapper.findErrorContextByTransId(transId)
                .stream()
                .map(this::serializer)
                .collect(Collectors.toList());
    }

    /**
     * getRunningContextsByStreamIds
     *
     * @param streamIds streamIds
     * @return List<FlowContext < FlowData>>
     */
    public List<FlowContext<FlowData>> getRunningContextsByStreamIds(List<String> streamIds) {
        return contextMapper.findRunningContextByStreamIds(streamIds).stream().map(c -> {
            try {
                return this.serializer(c);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 根据transId返回运行状态的contextId
     *
     * @param flowTransId transId
     * @return context ID列表
     */
    public List<String> getRunningContextsIdByTransaction(String flowTransId) {
        return contextMapper.getRunningContextsIdByTransaction(flowTransId);
    }

    /**
     * 根据transId返回运行状态的contextId
     *
     * @param traceId traceId
     * @return context ID列表
     */
    public List<String> getRunningContextsIdByTraceId(String traceId) {
        return contextMapper.getRunningContextsIdByTraceId(traceId);
    }

    @Override
    public String getStreamIdByTransId(String flowTransId) {
        return contextMapper.getStreamIdByTransId(flowTransId);
    }

    /**
     * 根据transId获取已完成上下文呢数量
     * 包括end节点和错误状态的上下文
     *
     * @param flowTransId trans id
     * @param endNode 结束节点Id
     * @return 已完成上下文集合
     */
    public int findFinishedPageNumByTransId(String flowTransId, String endNode) {
        return contextMapper.findFinishedPageNumByTransId(flowTransId, endNode);
    }

    /**
     * 获取结束节点上下文数量
     *
     * @param flowTransId trans id
     * @param endNode 结束节点id
     * @return 结束节点上下文个数
     */
    public int findEndContextsPageNumByTransId(String flowTransId, String endNode) {
        return contextMapper.findEndContextsNumByTransId(flowTransId, endNode);
    }

    /**
     * 获取错误状态上下文数量
     *
     * @param flowTransId trans id
     * @return 上下文集合
     */
    public int findErrorContextsPageNumByTransId(String flowTransId) {
        return contextMapper.findErrorContextsNumByTransId(flowTransId);
    }

    @Override
    public List<String> getTraceByTransId(String transId) {
        return contextMapper.getTraceByTransId(transId);
    }

    @Override
    public void deleteByTransId(String transId) {
        contextMapper.deleteByTransId(transId);
    }

    /**
     * findFinishedContextsPagedByTraceId
     *
     * @param traceId traceId
     * @param endNode endNode
     * @param pageNum pageNum
     * @param limit limit
     * @return List<FlowContext < FlowData>>
     */
    public List<FlowContext<FlowData>> findFinishedContextsPagedByTraceId(String traceId, String endNode,
            Integer pageNum, Integer limit) {
        return contextMapper.findFinishedContextsPagedByTraceId(traceId, endNode, pageNum, limit)
                .stream()
                .map(this::serializer)
                .collect(Collectors.toList());
    }

    /**
     * getEndContextsPagedByTraceId
     *
     * @param traceId traceId
     * @param endNode endNode
     * @param pageNum pageNum
     * @param limit limit
     * @return List<FlowContext < FlowData>>
     */
    public List<FlowContext<FlowData>> getEndContextsPagedByTraceId(String traceId, String endNode, Integer pageNum,
            Integer limit) {
        return contextMapper.findEndContextsPagedByTraceId(traceId, endNode, pageNum, limit)
                .stream()
                .map(this::serializer)
                .collect(Collectors.toList());
    }

    /**
     * getErrorContextsPagedByTraceId
     *
     * @param traceId traceId
     * @param pageNum pageNum
     * @param limit limit
     * @return List<FlowContext < FlowData>>
     */
    public List<FlowContext<FlowData>> getErrorContextsPagedByTraceId(String traceId, Integer pageNum, Integer limit) {
        return contextMapper.findErrorContextsPagedByTraceId(traceId, pageNum, limit)
                .stream()
                .map(this::serializer)
                .collect(Collectors.toList());
    }

    /**
     * findFinishedPageNumByTraceId
     *
     * @param traceId traceId
     * @param endNode endNode
     * @return int
     */
    public int findFinishedPageNumByTraceId(String traceId, String endNode) {
        return contextMapper.findFinishedPageNumByTraceId(traceId, endNode);
    }

    /**
     * findEndContextsPageNumByTraceId
     *
     * @param traceId traceId
     * @param endNode endNode
     * @return int
     */
    public int findEndContextsPageNumByTraceId(String traceId, String endNode) {
        return contextMapper.findEndContextsNumByTraceId(traceId, endNode);
    }

    /**
     * findErrorContextsPageNumByTraceId
     *
     * @param traceId traceId
     * @return int
     */
    public int findErrorContextsPageNumByTraceId(String traceId) {
        return contextMapper.findErrorContextsNumByTraceId(traceId);
    }

    /**
     * getRunningContextsByTraceId
     *
     * @param traceId traceId
     * @return List<FlowContext < T>>
     */
    public List<FlowContext<FlowData>> getRunningContextsByTraceId(String traceId) {
        return contextMapper.getRunningContextsByTraceId(traceId)
                .stream()
                .map(this::serializer)
                .collect(Collectors.toList());
    }

    @Override
    public TraceOwner getTraceOwner() {
        return traceOwner;
    }

    @Override
    public void deleteByContextIds(List<String> contextIds) {
        contextMapper.deleteByContextIds(contextIds);
    }

    /**
     * 根据contextIds获取traceIds
     *
     * @param contextIds contextIds
     * @return traceId列表
     */
    public List<String> findTraceIdsByContextIds(List<String> contextIds) {
        return contextMapper.findTraceIdsByContextIds(contextIds);
    }

    /**
     * 获取所有已完成上下文呢集合
     * 包括end节点和错误状态的上下文
     *
     * @param flowTransId transId
     * @param endNode 结束节点id
     * @return 上下文集合
     */
    public List<FlowContext<FlowData>> findFinishedContextsByTransId(String flowTransId, String endNode) {
        return contextMapper.findFinishedContextsByTransId(flowTransId, endNode)
                .stream()
                .map(this::serializer)
                .collect(Collectors.toList());
    }

    /**
     * findFinishedContextsByTraceId
     *
     * @param flowTraceId flowTraceId
     * @param endNode endNode
     * @return List<FlowContext < FlowData>>
     */
    public List<FlowContext<FlowData>> findFinishedContextsByTraceId(String flowTraceId, String endNode) {
        return contextMapper.findFinishedContextsByTraceId(flowTraceId, endNode)
                .stream()
                .map(this::serializer)
                .collect(Collectors.toList());
    }

    /**
     * 在节点处理完成后，之前这批context的状态，toBatch, 节点位置信息
     *
     * @param contexts 同一批context
     */
    public void updateProcessStatus(List<FlowContext<FlowData>> contexts) {
        List<String> ids = contexts.stream().map(IdGenerator::getId).collect(Collectors.toList());

        String toBatch = contexts.get(0).getToBatch();
        String status = contexts.get(0).getStatus().toString();
        String position = contexts.get(0).getPosition();

        LocalDateTime updateAt = LocalDateTime.now();
        LocalDateTime archivedAt = status.equals(FlowNodeStatus.ARCHIVED.toString()) ? updateAt : null;
        if (FlowNodeStatus.ARCHIVED.toString().equals(status) || FlowNodeStatus.ERROR.toString().equals(status)
                || FlowNodeStatus.TERMINATE.toString().equals(status)) {
            contexts.forEach(context -> this.contextSessions.remove(context.getId()));
        }

        contextMapper.updateProcessStatus(ids,
                new FlowContextUpdateInfo(toBatch, status, position, updateAt, archivedAt),
                CONTEXT_EXCLUSIVE_STATUS_MAP.get(status));
    }

    /**
     * 根据to batch id查询不带有flow data的上下文数据
     *
     * @param toBatchIds toBatchId列表
     * @return 上下文列表
     */
    public List<FlowContext<String>> getWithoutFlowDataByToBatch(List<String> toBatchIds) {
        if (toBatchIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<FlowContextPO> pos = contextMapper.findWithoutFlowDataByToBatch(toBatchIds);
        return pos.stream().map(this::serializerAsString).collect(Collectors.toList());
    }

    /**
     * 至少含有一个符合状态的context
     *
     * @param statusList 状态列表
     * @param traceId trace id
     * @return true or false
     */
    public boolean hasContextWithStatus(List<String> statusList, String traceId) {
        if (statusList.isEmpty()) {
            return false;
        }
        int count = contextMapper.findCountByStatus(statusList, traceId);
        return count != 0;
    }

    /**
     * 所有context状态都符合要求
     *
     * @param statusList 状态列表
     * @param traceId trace id
     * @return true or false
     */
    public boolean isAllContextStatus(List<String> statusList, String traceId) {
        if (statusList.isEmpty()) {
            return false;
        }
        int count = contextMapper.findCountNotInStatus(statusList, traceId);
        return count == 0;
    }

    /**
     * 在某个节点至少含有一个符合状态的context
     *
     * @param statusList 状态列表
     * @param traceId trace id
     * @param position 位置
     * @return true or false
     */
    public boolean hasContextWithStatusAtPosition(List<String> statusList, String traceId, String position) {
        if (statusList.isEmpty()) {
            return false;
        }
        int count = contextMapper.findCountByStatusAtPosition(statusList, traceId, position);
        return count != 0;
    }

    /**
     * 根据trace id获取trans id
     *
     * @param traceId trace id
     * @return trans id
     */
    public String getTransIdByTrace(String traceId) {
        return contextMapper.getTransIdByTrace(traceId);
    }

    @Override
    public void deleteByTraceIdList(List<String> traceIdList) {
        if (CollectionUtils.isEmpty(traceIdList)) {
            return;
        }
        contextMapper.deleteByTraceIdList(traceIdList);
    }
}
