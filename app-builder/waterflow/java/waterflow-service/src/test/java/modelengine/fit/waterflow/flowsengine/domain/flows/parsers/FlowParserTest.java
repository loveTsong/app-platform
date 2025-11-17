/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.flowsengine.domain.flows.parsers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import modelengine.fit.waterflow.FlowsDataBaseTest;
import modelengine.fit.waterflow.MethodNameLoggerExtension;
import modelengine.fit.waterflow.domain.enums.FlowNodeTriggerMode;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.FlowDefinition;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.FlowConditionNode;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.FlowNode;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.callbacks.FlowCallback;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.events.FlowEvent;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.filters.FlowFilter;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.jobers.FlowGenericableJober;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.jobers.FlowJober;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.jobers.FlowStoreJober;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.tasks.FlowTask;
import modelengine.fit.waterflow.flowsengine.domain.flows.enums.FlowCallbackType;
import modelengine.fit.waterflow.flowsengine.domain.flows.enums.FlowDefinitionStatus;
import modelengine.fit.waterflow.flowsengine.domain.flows.enums.FlowJoberProperties;
import modelengine.fit.waterflow.flowsengine.domain.flows.enums.FlowJoberType;
import modelengine.fit.waterflow.domain.enums.FlowNodeType;
import modelengine.fit.waterflow.flowsengine.domain.flows.enums.FlowTaskType;
import modelengine.fit.waterflow.flowsengine.domain.flows.parsers.nodes.events.EventParser;
import modelengine.fitframework.util.ObjectUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link FlowParser}对应测试类
 *
 * @author 杨祥宇
 * @since 2023/8/16
 */
@ExtendWith(MethodNameLoggerExtension.class)
public class FlowParserTest extends FlowsDataBaseTest {
    private static final String FILE_PATH_PREFIX = "flows/parsers/";

    private FlowParser flowParser;

    @BeforeEach
    void before() {
        flowParser = new FlowParser(null);
    }

    @Test
    @DisplayName("测试解析流程定义成功")
    public void testParserFlowDefinitionSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_empty_nodes.json"));
        JSONObject graphData = JSONObject.parseObject(jsonData);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        Assertions.assertEquals(graphData.getString("name"), flowDefinition.getName());
        Assertions.assertEquals(graphData.getString("description"), flowDefinition.getDescription());
        Assertions.assertEquals(graphData.getString("metaId"), flowDefinition.getMetaId());
        Assertions.assertEquals(graphData.getString("version"), flowDefinition.getVersion());
        Assertions.assertEquals(FlowDefinitionStatus.ACTIVE, flowDefinition.getStatus());
        Assertions.assertEquals(graphData.getString("definitionId"), flowDefinition.getDefinitionId());
    }

    @Test
    @DisplayName("测试解析Start节点成功")
    public void testStartNodeParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_start_nodes.json"));
        JSONArray nodes = JSONObject.parseObject(jsonData).getJSONArray("nodes");
        JSONObject startNode = getNode(nodes, FlowNodeType.START);
        JSONObject eventObj = getNode(nodes, FlowNodeType.EVENT);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowNode node = flowDefinition.getFlowNode("j7rlcr");
        FlowEvent event = node.getEvents().get(0);
        Assertions.assertEquals(FlowNodeType.START, node.getType());
        Assertions.assertEquals(startNode.getString("metaId"), node.getMetaId());
        Assertions.assertEquals(FlowNodeTriggerMode.AUTO, node.getTriggerMode());
        Assertions.assertEquals(eventObj.getString("metaId"), event.getMetaId());
        Assertions.assertEquals(eventObj.getString("from"), event.getFrom());
        Assertions.assertEquals(eventObj.getString("to"), event.getTo());
    }

    @Test
    @DisplayName("测试解析End节点成功")
    public void testEndNodeParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_end_nodes.json"));
        JSONObject endNode = getNode(JSONObject.parseObject(jsonData).getJSONArray("nodes"), FlowNodeType.END);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowNode node = flowDefinition.getFlowNode(endNode.getString("metaId"));
        Assertions.assertEquals(FlowNodeType.END, node.getType());
        Assertions.assertEquals(endNode.getString("metaId"), node.getMetaId());
        Assertions.assertEquals(FlowNodeTriggerMode.AUTO, node.getTriggerMode());
    }

    @Test
    @DisplayName("测试解析Conditional节点成功")
    public void testConditionalNodeParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_conditional_nodes.json"));
        JSONObject conditionNode = getNode(JSONObject.parseObject(jsonData).getJSONArray("nodes"), FlowNodeType.CONDITION);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowNode node = flowDefinition.getFlowNode(conditionNode.getString("metaId"));
        Assertions.assertEquals(FlowNodeType.CONDITION, node.getType());
        Assertions.assertEquals(conditionNode.getString("metaId"), node.getMetaId());
        Assertions.assertEquals(FlowNodeTriggerMode.MANUAL, node.getTriggerMode());
    }

    @Test
    @DisplayName("测试解析State节点成功")
    public void testStateNodeParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_state_nodes.json"));
        JSONObject stateNode = getNode(JSONObject.parseObject(jsonData).getJSONArray("nodes"), FlowNodeType.STATE);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowNode node = flowDefinition.getFlowNode(stateNode.getString("metaId"));
        Assertions.assertEquals(FlowNodeType.STATE, node.getType());
        Assertions.assertEquals(stateNode.getString("metaId"), node.getMetaId());
        Assertions.assertEquals(FlowNodeTriggerMode.AUTO, node.getTriggerMode());
    }

    @Test
    @DisplayName("测试general自动任务解析成功")
    public void testFitableTaskParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_fitable_task.json"));
        JSONObject stateNode = getNode(JSONObject.parseObject(jsonData).getJSONArray("nodes"), FlowNodeType.STATE);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowJober flowJober = flowDefinition.getFlowNode(stateNode.getString("metaId")).getJober();

        JSONArray jsonArray = stateNode.getJSONObject("jober").getJSONArray("fitables");
        Assertions.assertEquals(FlowJoberType.GENERAL_JOBER, flowJober.getType());
        Assertions.assertEquals(2, flowJober.getFitables().size());
        Assertions.assertTrue(flowJober.getFitables().contains(jsonArray.get(0)));
        Assertions.assertTrue(flowJober.getFitables().contains(jsonArray.get(1)));
        Assertions.assertEquals("xc9ax5", flowJober.getNodeMetaId());
        Assertions.assertEquals(2, flowJober.getFitablesConfig().size());
    }

    @Test
    @DisplayName("测试解析手动任务成功")
    public void testManualTaskParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_manual_task.json"));
        JSONObject stateNode = getNode(JSONObject.parseObject(jsonData).getJSONArray("nodes"), FlowNodeType.STATE);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowTask flowTask = flowDefinition.getFlowNode(stateNode.getString("metaId")).getTask();
        JSONObject taskObj = stateNode.getJSONObject("task");
        Assertions.assertEquals(FlowTaskType.APPROVING_TASK, flowTask.getTaskType());
        Assertions.assertEquals(taskObj.getString("taskId"), flowTask.getTaskId());
        Assertions.assertEquals(3, flowTask.getProperties().size());
        Assertions.assertTrue(flowTask.getProperties().containsKey("title"));
        Assertions.assertTrue(flowTask.getProperties().containsKey("owner"));
        Assertions.assertTrue(flowTask.getProperties().containsKey("handleRule"));
        Assertions.assertTrue(
                flowDefinition.getFlowNode(stateNode.getString("metaId")).getProperties().containsKey("flowContext"));
    }

    @Test
    @DisplayName("测试解析事件成功")
    public void testEventParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_event.json"));

        FlowGraphData flowGraphData = new FlowGraphData(jsonData);
        Map<String, FlowNode> allNodeMap = new HashMap<>();
        FlowNode node = new FlowConditionNode();
        node.setMetaId("ue30vx");
        node.setEvents(new ArrayList<>());

        allNodeMap.put(node.getMetaId(), node);

        EventParser.INSTANCE.parse(flowGraphData, allNodeMap);

        Assertions.assertEquals(1, node.getEvents().size());
        Assertions.assertEquals("!{{input.createPipeline}} && {{input.startPipeline}}",
                node.getEvents().get(0).getConditionRule());
    }

    @Test
    @DisplayName("测试解析过滤器成功")
    public void testFilterParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_filter.json"));
        JSONObject stateNode = getNode(JSONObject.parseObject(jsonData).getJSONArray("nodes"), FlowNodeType.STATE);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        JSONObject joberFilterObj = stateNode.getJSONObject("joberFilter");
        FlowFilter joberFilter = flowDefinition.getFlowNode(stateNode.getString("metaId")).getJoberFilter();
        Assertions.assertEquals(joberFilterObj.getString("type").toUpperCase(), joberFilter.getFilterType().getCode());
        Assertions.assertEquals(joberFilterObj.getString("threshold"), joberFilter.getProperties().get("threshold"));

        JSONObject taskFilterObj = stateNode.getJSONObject("taskFilter");
        FlowFilter taskFilter = flowDefinition.getFlowNode(stateNode.getString("metaId")).getTaskFilter();
        Assertions.assertEquals(taskFilterObj.getString("type").toUpperCase(), taskFilter.getFilterType().getCode());
        Assertions.assertEquals(taskFilterObj.getString("threshold"), taskFilter.getProperties().get("threshold"));
    }

    @Test
    @DisplayName("测试http自动任务解析成功")
    public void testHttpTaskParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_http_task.json"));
        JSONObject stateNode = getNode(JSONObject.parseObject(jsonData).getJSONArray("nodes"), FlowNodeType.STATE);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowJober flowJober = flowDefinition.getFlowNode(stateNode.getString("metaId")).getJober();

        JSONObject httpMeta = JSONObject.parseObject(flowJober.getProperties().get(FlowJoberProperties.ENTITY.getValue()));

        Assertions.assertEquals(FlowJoberType.HTTP_JOBER, flowJober.getType());
        Assertions.assertEquals(1, flowJober.getFitables().size());
        Assertions.assertEquals("https://localhost:8028/test", httpMeta.get("httpUrl"));
        Assertions.assertEquals("POST", httpMeta.get("httpMethod"));
    }

    @Test
    @DisplayName("测试ohscript自动任务解析成功")
    public void testOhscriptJoberParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_ohscript_jober.json"));
        JSONObject stateNode = getNode(JSONObject.parseObject(jsonData).getJSONArray("nodes"), FlowNodeType.STATE);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowJober flowJober = flowDefinition.getFlowNode(stateNode.getString("metaId")).getJober();

        JSONObject entity = JSONObject.parseObject(flowJober.getProperties().get(FlowJoberProperties.ENTITY.getValue()));

        Assertions.assertEquals(FlowJoberType.OHSCRIPT_JOBER, flowJober.getType());
        Assertions.assertEquals(3, flowJober.getFitables().size());
        Assertions.assertEquals("context>>f1>>f2>>f3", entity.get("code"));
    }

    @Test
    @DisplayName("测试genericable自动任务解析成功")
    public void testGenericableJoberParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_genericable_jober.json"));
        JSONObject stateNode = getNode(JSONObject.parseObject(jsonData).getJSONArray("nodes"), FlowNodeType.STATE);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowJober flowJober = flowDefinition.getFlowNode(stateNode.getString("metaId")).getJober();

        Assertions.assertEquals(FlowJoberType.GENERICABLE_JOBER, flowJober.getType());
        Assertions.assertTrue(flowJober instanceof FlowGenericableJober);
        FlowGenericableJober genericableJober = (FlowGenericableJober) flowJober;
        Assertions.assertTrue(flowJober.getFitables().contains(genericableJober.getFitableId()));
        Assertions.assertEquals("modelengine.fit.demo.flow.helloWorld", genericableJober.getGenericableConfig().getId());
        Assertions.assertEquals(1, genericableJober.getGenericableConfig().getParams().size());
        Assertions.assertEquals("name", genericableJober.getGenericableConfig().getParams().get(0));
    }

    @Test
    @DisplayName("测试store jober自动任务解析成功")
    public void testStoreJoberParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_store_jober.json"));
        JSONObject stateNode = getNode(JSONObject.parseObject(jsonData).getJSONArray("nodes"), FlowNodeType.STATE);

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowJober flowJober = flowDefinition.getFlowNode(stateNode.getString("metaId")).getJober();

        Assertions.assertEquals(FlowJoberType.STORE_JOBER, flowJober.getType());
        Assertions.assertTrue(flowJober instanceof FlowStoreJober);
        FlowStoreJober storeJober = (FlowStoreJober) flowJober;
        Assertions.assertEquals("toolId", storeJober.getServiceMeta().getUniqueName());
        Assertions.assertEquals(1, storeJober.getServiceMeta().getParams().size());
        Assertions.assertEquals("name", storeJober.getServiceMeta().getParams().get(0));
    }

    @Test
    @DisplayName("测试callback解析成功")
    public void testFlowCallbackParserSuccess() {
        String jsonData = getJsonData(getFilePath("flows_with_callback.json"));

        FlowDefinition flowDefinition = flowParser.parse(jsonData);

        FlowCallback callback = flowDefinition.getCallback();

        Assertions.assertNull(callback.getNodeMetaId());
        Assertions.assertEquals("通知回调", callback.getName());
        Assertions.assertEquals(FlowCallbackType.GENERAL_CALLBACK, callback.getType());
        Assertions.assertTrue(callback.getFilteredKeys().isEmpty());
        Assertions.assertEquals(1, callback.getFitables().size());
        Assertions.assertEquals("modelengine.fit.jober.fitable.FlowInfoCallback", callback.getFitables().toArray()[0]);
        Assertions.assertTrue(callback.getProperties().isEmpty());
    }

    @Override
    protected String getFilePathPrefix() {
        return FILE_PATH_PREFIX;
    }

    private JSONObject getNode(JSONArray nodes, FlowNodeType type) {
        return ObjectUtils.cast(nodes.stream()
                .filter(node -> belongTo(ObjectUtils.cast(node), type))
                .findAny()
                .orElse(new JSONObject()));
    }

    private boolean belongTo(JSONObject jsonObject, FlowNodeType type) {
        return jsonObject.getString("type").equalsIgnoreCase(type.getCode());
    }
}
