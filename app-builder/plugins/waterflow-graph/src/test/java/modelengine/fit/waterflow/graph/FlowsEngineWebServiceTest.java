/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/


package modelengine.fit.waterflow.graph;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import modelengine.fit.jade.waterflow.dto.FlowInfo;
import modelengine.fit.jane.common.entity.OperationContext;
import modelengine.fit.jane.flow.graph.entity.FlowGraphDefinition;
import modelengine.fit.jane.flow.graph.entity.FlowSaveEntity;
import modelengine.fit.jane.flow.graph.repo.FlowsGraphRepo;
import modelengine.fit.jade.waterflow.entity.FlowDefinitionResult;
import modelengine.fit.jade.waterflow.entity.FlowNodeInfo;
import modelengine.fit.jade.waterflow.service.FlowDefinitionService;
import modelengine.fit.waterflow.biz.task.TagService;
import modelengine.fit.waterflow.flowsengine.domain.flows.context.repo.flowcontext.QueryFlowContextPersistRepo;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.FlowNode;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.FlowStartNode;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.FlowStateNode;
import modelengine.fit.waterflow.flowsengine.domain.flows.definitions.nodes.tasks.FlowTask;
import modelengine.fit.waterflow.domain.enums.FlowNodeType;
import modelengine.fit.waterflow.service.FlowRuntimeService;
import modelengine.fitframework.model.RangedResultSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FlowsEngineWebService测试类
 *
 * @author 杨祥宇
 * @since 2023/12/16
 */
@ExtendWith(MockitoExtension.class)
class FlowsEngineWebServiceTest {
    private static final String GRAPH_DATA =
            "{\"id\":\"b4d0e186a6974f68aaccce0982af412a\",\"title\":\"01121128\",\"createTime\":{\"$date\":"
                    + "\"2023-12-29T06:30:18.866Z\"},\"source\":\"elsa\",\"type\":\"graph\",\"tenant\":\"default\""
                    + ",\"setting\":{\"borderColor\":\"steelblue\",\"backColor\":\"whitesmoke\",\"headColor\":"
                    + "\"steelblue\",\"fontColor\":\"steelblue\",\"captionfontColor\":\"whitesmoke\",\"fontFace\":"
                    + "\"arial\",\"captionfontFace\":\"arial black\",\"fontSize\":12,\"captionfontSize\":14,"
                    + "\"fontStyle\":\"normal\",\"captionfontStyle\":\"normal\",\"fontWeight\":\"lighter\","
                    + "\"captionfontWeight\":\"lighter\",\"hAlign\":\"center\",\"vAlign\":\"top\","
                    + "\"captionhAlign\":\"center\",\"lineHeight\":1.5,\"lineWidth\":2,\"captionlineHeight\":1,"
                    + "\"focusMargin\":0,\"focusBorderColor\":\"darkorange\",\"focusFontColor\":\"darkorange\","
                    + "\"focusBackColor\":\"whitesmoke\",\"mouseInColor\":\"orange\",\"mouseInBorderColor\":"
                    + "\"orange\",\"mouseInFontColor\":\"orange\",\"mouseInBackColor\":\"whitesmoke\","
                    + "\"borderWidth\":1,\"globalAlpha\":1,\"backAlpha\":0.15,\"cornerRadius\":4,\"dashWidth\":0,"
                    + "\"autoText\":false,\"autoHeight\":false,\"autoWidth\":false,\"margin\":25,\"pad\":10,"
                    + "\"code\":\"\",\"rotateDegree\":0,\"shadow\":false,\"shadowData\":\"2px 2px 4px\","
                    + "\"outstanding\":false,\"pDock\":\"none\",\"dockMode\":\"none\",\"priority\":0,"
                    + "\"infoType\":{\"next\":\"INFORMATION\",\"name\":\"none\"},\"progressStatus\":"
                    + "{\"next\":\"UNKNOWN\",\"color\":\"gray\",\"name\":\"NONE\"},\"progressPercent\""
                    + ":0.65,\"showedProgress\":false,\"itemPad\":[5,5,5,5],\"itemScroll\":{\"x\":0,\"y\":0},"
                    + "\"scrollLock\":{\"x\":false,\"y\":false},\"resizeable\":true,\"selectable\":true,"
                    + "\"rotateAble\":true,\"editable\":true,\"moveable\":true,\"dragable\":true,\"visible\""
                    + ":true,\"deletable\":true,\"allowLink\":true,\"shared\":false,\"strikethrough\":false,"
                    + "\"underline\":false,\"numberedList\":false,\"bulletedList\":false,\"enableAnimation\""
                    + ":false,\"enableSocial\":true,\"emphasized\":false,\"bulletSpeed\":1,\"tag\":{}},\"pages\":"
                    + "[{\"id\":\"elsa-page:4kvjlz\",\"backColor\":\"white\",\"bold\":false,\"borderColor\":"
                    + "\"white\",\"container\":\"elsa-page:4kvjlz\",\"createTime\":{\"$date\":"
                    + "\"2023-12-29T06:30:18.867Z\"},\"createUser\":\"A3000 null\",\"dirty\":false,"
                    + "\"division\":-1,\"dockAlign\":\"top\",\"dockMode\":\"none\",\"enableContextMenu\":true,"
                    + "\"fontColor\":\"#ECD0A7\",\"fontFace\":\"arial\",\"fontSize\":18,\"fontStyle\":\"normal\","
                    + "\"fontWeight\":\"bold\",\"hAlign\":\"left\",\"height\":1195,\"hideText\":true,\"index\":0,"
                    + "\"isDeleted\":false,\"isPage\":true,\"italic\":false,\"itemPad\":[0,0,0,0],\"itemScroll\":"
                    + "{\"x\":0,\"y\":0},\"itemSpace\":5,\"mode\":\"configuration\",\"moveable\":true,\"namespace\""
                    + ":\"elsa\",\"shapesAs\":{},\"text\":\"--\",\"type\":\"page\",\"updateTime\":{\"$date\":"
                    + "\"2023-12-29T06:31:20.34Z\"},\"updateUser\":\" \",\"vAlign\":\"top\",\"version\":\"1.0.0\""
                    + ",\"width\":2489,\"x\":0,\"y\":0,\"shapes\":[{\"id\":\"aem9oj\",\"autoWidth\":false,\"bold\""
                    + ":false,\"borderWidth\":0,\"completedTask\":0,\"createTime\":{\"$date\":"
                    + "\"2023-12-29T06:31:20.34Z\"},\"createUser\":\" \",\"dirty\":false,\"enableAnimation\":true,"
                    + "\"height\":30,\"hideText\":true,\"index\":100,\"isDeleted\":false,\"italic\":false,"
                    + "\"margin\":10,\"namespace\":\"flowable\",\"pad\":6,\"rotateAble\":false,\"runningTask\":0,"
                    + "\"text\":\"开始\",\"triggerMode\":\"auto\",\"type\":\"start\",\"updateTime\":{\"$date\":"
                    + "\"2023-12-29T06:31:20.34Z\"},\"updateUser\":\" \",\"version\":\"1.0.0\",\"warningTask\":0,"
                    + "\"width\":30,\"x\":330,\"y\":374,\"container\":\"elsa-page:4kvjlz\"},{\"id\":\"wyii01\","
                    + "\"allowLink\":false,\"backColor\":\"white\",\"beginArrow\":false,\"beginArrowEmpty\":false,"
                    + "\"beginArrowSize\":4,\"bold\":false,\"borderWidth\":1,\"createTime\":{\"$date\":"
                    + "\"2023-12-29T06:31:20.34Z\"},\"createUser\":\" \",\"curvePoint1\":{\"x\":0,\"y\":0},"
                    + "\"curvePoint2\":{\"x\":0,\"y\":0},\"dashWidth\":0,\"definedFromConnector\":\"E\","
                    + "\"definedToConnector\":\"\",\"dirty\":false,\"endArrow\":true,\"endArrowEmpty\":false,"
                    + "\"endArrowSize\":4,\"endpointOffsetX\":0,\"endpointOffsetY\":0,\"fromShape\":\"aem9oj\","
                    + "\"hAlign\":\"center\",\"height\":8,\"hideText\":false,\"index\":101,\"isDeleted\":false,"
                    + "\"italic\":false,\"lineMode\":{\"type\":\"broken\"},\"lineWidth\":2,\"margin\":26,"
                    + "\"namespace\":\"flowable\",\"pad\":0,\"text\":\"\",\"textInnerHtml\":\"<p id=\\\"cx3uir\\\">"
                    + "<br data-cke-filler=\\\"true\\\"></p>\",\"textX\":0,\"textY\":0,\"toShape\":\"e9pzul\","
                    + "\"type\":\"event\",\"updateTime\":{\"$date\":\"2023-12-29T06:31:20.34Z\"},\"updateUser\":"
                    + "\" \",\"version\":\"1.0.0\",\"width\":133,\"x\":360,\"y\":389,\"container\":"
                    + "\"elsa-page:4kvjlz\"},{\"id\":\"jv88jh\",\"allowLink\":false,\"backColor\":\"white\","
                    + "\"beginArrow\":false,\"beginArrowEmpty\":false,\"beginArrowSize\":4,\"bold\":false,"
                    + "\"borderWidth\":1,\"createTime\":{\"$date\":\"2023-12-29T06:31:20.34Z\"},\"createUser\":"
                    + "\" \",\"curvePoint1\":{\"x\":0,\"y\":0},\"curvePoint2\":{\"x\":0,\"y\":0},\"dashWidth\":0,"
                    + "\"definedFromConnector\":\"E\",\"definedToConnector\":\"\",\"dirty\":true,\"endArrow\":true,"
                    + "\"endArrowEmpty\":false,\"endArrowSize\":4,\"endpointOffsetX\":0,\"endpointOffsetY\":0,"
                    + "\"fromShape\":\"e9pzul\",\"hAlign\":\"center\",\"height\":7.5,\"hideText\":false,\"index\""
                    + ":102,\"isDeleted\":false,\"italic\":false,\"lineMode\":{\"type\":\"broken\"},\"lineWidth\""
                    + ":2,\"margin\":26.25,\"namespace\":\"flowable\",\"pad\":0,\"text\":\"\",\"textInnerHtml\":"
                    + "\"<p id=\\\"vvj5pf\\\"><br data-cke-filler=\\\"true\\\"></p>\",\"textX\":0,\"textY\":0,"
                    + "\"toShape\":\"jhbofa\",\"type\":\"event\",\"updateTime\":{\"$date\":"
                    + "\"2023-12-29T06:31:20.34Z\"},\"updateUser\":\" \",\"version\":\"1.0.0\",\"width\":122,"
                    + "\"x\":536,\"y\":397,\"container\":\"elsa-page:4kvjlz\"},{\"id\":\"e9pzul\",\"autoHeight\":"
                    + "true,\"autoWidth\":true,\"bold\":false,\"completedTask\":0,\"createTime\":{\"$date\":"
                    + "\"2023-12-29T06:31:20.34Z\"},\"createUser\":\" \",\"dirty\":true,\"enableAnimation\":true,"
                    + "\"height\":30,\"index\":103,\"isDeleted\":false,\"italic\":false,\"jober\":{\"fitables\":"
                    + "[\"3e460bc100a74f8ca7b94f6dce31a021\"],\"fitablesConfig\":[],\"entity\":{\"code\":"
                    + "\"let context1 = entity{\\n    .id = \\\"3e460bc100a74f8ca7b94f6dce31a021\\\";\\n.timeout ="
                    + " 30000;\\n};\\nlet f1 = fit::handleTask(context1);\\n\\next::context >> f1\"},\"retryNum\":2"
                    + ",\"type\":\"ohscript_jober\"},\"joberFilter\":{\"threshold\":1,\"type\":\"BATCH_FILTER\"},"
                    + "\"namespace\":\"flowable\",\"pad\":6,\"rotateAble\":false,\"runningTask\":0,\"text\":"
                    + "\"state\",\"triggerMode\":\"auto\",\"type\":\"state\",\"updateTime\":{\"$date\":"
                    + "\"2023-12-29T06:31:20.34Z\"},\"updateUser\":\" \",\"version\":\"1.0.0\",\"warningTask\":0,"
                    + "\"width\":43,\"x\":493,\"y\":386,\"container\":\"elsa-page:4kvjlz\"},{\"id\":\"jhbofa\","
                    + "\"autoWidth\":false,\"bold\":false,\"borderWidth\":0,\"completedTask\":0,\"createTime\":"
                    + "{\"$date\":\"2023-12-29T06:31:20.34Z\"},\"createUser\":\" \",\"dirty\":true,"
                    + "\"enableAnimation\":true,\"height\":30,\"hideText\":true,\"index\":104,\"isDeleted\":false,"
                    + "\"italic\":false,\"joberFilter\":{\"threshold\":1,\"type\":\"BATCH_FILTER\"},\"margin\":10,"
                    + "\"namespace\":\"flowable\",\"pad\":6,\"rotateAble\":false,\"runningTask\":0,\"text\":"
                    + "\"结束\",\"triggerMode\":\"auto\",\"type\":\"end\",\"updateTime\":{\"$date\":"
                    + "\"2023-12-29T06:31:20.34Z\"},\"updateUser\":\" \",\"version\":\"1.0.0\",\"warningTask\":0,"
                    + "\"width\":30,\"x\":658,\"y\":389.5,\"container\":\"elsa-page:4kvjlz\"}]}],"
                    + "\"version\":\"1.0.0\"}";

    private FlowsEngineWebService flowsEngineWebService;

    private FlowDefinitionService flowsService;

    private FlowsGraphRepo flowsGraphRepo;

    private TagService tagService;

    private FlowRuntimeService flowRuntimeService;

    @BeforeEach
    void setUp() {
        flowsService = Mockito.mock(FlowDefinitionService.class);
        tagService = Mockito.mock(TagService.class);
        flowsGraphRepo = Mockito.mock(FlowsGraphRepo.class);
        flowRuntimeService = Mockito.mock(FlowRuntimeService.class);
        QueryFlowContextPersistRepo queryFlowContextRepo = Mockito.mock(QueryFlowContextPersistRepo.class);
        flowsEngineWebService = new FlowsEngineWebService(flowRuntimeService, flowsService, flowsGraphRepo, null,
                tagService, queryFlowContextRepo);
    }

    @Test
    @DisplayName("测试保存流程成功")
    void testCreateFlowsSuccess() {
        FlowSaveEntity flowSaveEntity = new FlowSaveEntity();
        flowSaveEntity.setId("test");
        flowSaveEntity.setVersion("1.0.0");
        OperationContext context = new OperationContext();
        when(flowsGraphRepo.saveFlow(any(), any())).thenReturn(0);

        FlowInfo flows = flowsEngineWebService.createFlows(flowSaveEntity, context);

        Assertions.assertEquals(flowSaveEntity.getId(), flows.getFlowId());
        Assertions.assertEquals(flowSaveEntity.getVersion(), flows.getVersion());
    }

    @Test
    @DisplayName("测试发布流程成功")
    void TestPublishFlowsSuccess() {
        FlowSaveEntity flowSaveEntity = new FlowSaveEntity();
        flowSaveEntity.setId("test");
        flowSaveEntity.setVersion("1.0.0");
        flowSaveEntity.setGraphData(GRAPH_DATA);
        flowSaveEntity.setTags(Collections.singletonList("test"));
        OperationContext context = new OperationContext();
        Map<String, FlowNode> nodeMap = new HashMap<>();
        nodeMap.put("start", getMockStartNode());
        FlowDefinitionResult definitionResult = new FlowDefinitionResult.Builder("123").build();
        when(flowsGraphRepo.saveFlow(any(), any())).thenReturn(0);
        when(flowsService.createFlows(any(), any())).thenReturn(definitionResult);

        FlowInfo flowInfo = flowsEngineWebService.publishFlows(flowSaveEntity, context);

        Assertions.assertEquals(flowSaveEntity.getId(), flowInfo.getFlowId());
        Assertions.assertEquals(flowSaveEntity.getVersion(), flowInfo.getVersion());
        Assertions.assertEquals(definitionResult.getFlowDefinitionId(), flowInfo.getFlowDefinitionId());
    }

    @Test
    @DisplayName("测试查询单条流程成功")
    void testGetFlowsSuccess() {
        String id = "test";
        String graph = "{\n" + "\t\"pages\":[\n" + "\t{\n" + "\t\t\"shapes\":[]\n" + "\t}\n" + "\t]\n" + "}";
        OperationContext context = new OperationContext();
        when(flowsGraphRepo.getFlow(any(), any())).thenReturn(graph);
        FlowNode flowNode = new FlowStateNode();
        flowNode.setType(FlowNodeType.START);
        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put("p1", "value");
        List<FlowNodeInfo> flowNodes = new ArrayList<>();
        FlowNodeInfo flowNodeInfo = new FlowNodeInfo();
        flowNodeInfo.setId("nodeId");
        flowNodeInfo.setProperties(propertiesMap);
        flowNodes.add(flowNodeInfo);
        FlowDefinitionResult definitionResult = new FlowDefinitionResult.Builder("123")
                .setFlowNodes(flowNodes)
                .build();
        when(flowsService.getFlowDefinitionByGraphData(any())).thenReturn(definitionResult);

        String version = "1.0.0";
        FlowInfo flows = flowsEngineWebService.getFlows(id, version, context);
        Assertions.assertEquals(id, flows.getFlowId());
        Assertions.assertEquals(version, flows.getVersion());
        Assertions.assertEquals(graph, flows.getConfigData());
        Assertions.assertEquals(1, flows.getFlowNodes().size());
        Assertions.assertEquals(propertiesMap, flows.getFlowNodes().get(0).getProperties());
    }

    @Test
    @DisplayName("测试删除流程成功")
    void TestDeleteFlowsSuccess() {
        String id = "test";
        String version = "1.0.0";
        OperationContext context = new OperationContext();
        when(flowsGraphRepo.deleteFlow(any(), any())).thenReturn(0);

        int ans = flowsEngineWebService.deleteFlows(id, version, context);

        Assertions.assertEquals(0, ans);
    }

    @Test
    @DisplayName("测试查询流程列表成功")
    void TestGetFlowListSuccess() {
        FlowGraphDefinition flowGraphDefinition = new FlowGraphDefinition();
        flowGraphDefinition.setFlowId("123");
        when(flowsGraphRepo.getFlowList(any(), any())).thenReturn(
                RangedResultSet.create(Collections.singletonList(flowGraphDefinition), 0, 10, 1));
        Map<String, List<String>> tagMap = new HashMap<>();
        tagMap.put("123", Collections.singletonList("test"));
        when(tagService.list("FLOW GRAPH", Collections.singletonList("123"), null)).thenReturn(tagMap);
        RangedResultSet<FlowGraphDefinition> res = flowsEngineWebService.findFlowList("testUser", new ArrayList<>(), 0,
                10, new OperationContext());
        Assertions.assertEquals(res.getRange().getTotal(), 1);
        Assertions.assertEquals("test", res.getResults().get(0).getTags().get(0));
    }

    private FlowStartNode getMockStartNode() {
        FlowStartNode startNode = new FlowStartNode();
        startNode.setName("start");
        startNode.setType(FlowNodeType.START);
        startNode.setTask(new FlowTask());
        startNode.setProperties(new HashMap<>());
        return startNode;
    }
}