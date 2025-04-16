/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package modelengine.fit.jade.wenjie.data;

import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Fitable;
import modelengine.fitframework.annotation.Property;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.util.LazyLoader;
import modelengine.fel.tool.annotation.Attribute;
import modelengine.fel.tool.annotation.Group;
import modelengine.fel.tool.annotation.ToolMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @author 夏斐
 * @since 2025/3/17
 */
@Group(name = "WenjieImpl")
@Component
public class WenjieServiceImpl implements WenjieService {
    private static final Logger log = Logger.get(WenjieServiceImpl.class);
    // 基础图片 URL 前缀
    private static final String BASE_URL = "";

    // 文件后缀，可以统一替换
    private static final String FILE_SUFFIX = ".jpg";

    // 构建车型名称与图片 URL 的 Map
    private static final Map<String, List<String>> carImageMap = new HashMap<>();

    private static final String DEFAULT_URL = BASE_URL + "default" + FILE_SUFFIX;

    private final LazyLoader<String> describes = new LazyLoader<>(this::readDescribes);


    // 你是一个智能体，具备自主的工具调用能力，对于用户的提问，首先进行详尽的思考和问题拆解，按照需要拆解成多个小问题处理，然后再考虑整体的解决方案。你有两种方式回答问题，一种是无需调用工具等外部方法，利用推理和历史对话记录，特别地，请仔细判定历史对话记录中是否已经包含了答案，可以直接得到答案的，则直接输出结果，无需输出思考的过程，也无需使用任何标签包装; 另一种是相对复杂的问题，你需要使用工具作为外部辅助来协助你找到最终的答案，这种回答整体采用标签体系，通过"<reasoning>"、"<step>"、"<tool>"、"<final>"四种标签来管理回复内容，所有的内容都需要被这四种标签包装，标签以外禁止有其他内容输出，这种标签化回答的详细说明如以下6条所述：1. 如果通过思考发现需要使用工具或者曾经使用过工具，则输出思考的过程，使用"<reasoning></reasoning>"来包装思考过程。2. 如果你需要调用工具，请确保工具来自于给定的tools列表，禁止给出不在tools列表中的工具，如果找不到合适的工具，则通过推理得出结果，对于不适合推理的场景，则提示无法给出结果。 对于找到的需求使用的工具，给出描述调用工具的原因和方式，如果工具名字对人类理解不友好，则优化工具名称的描述，使用"<step></step>"包装，当发起工具调用时，需等待工具结果返回后再进行后续思考和输出。3. 如果你发现输入messages中，存在新增的未解释过tool调用结果，则在进行思考前，先用自然语言简要概括tool的调用结果，然后再进行下一步的思考；使用"<tool></tool>"来包装tool的调用结果，每个tool调用输出一条，对于已经解释过的tool的调用结果，不要重复解释。4. 如果你发现已经可以得到最终的结果，则使用"<final></final>"来包装最终结果，在等待工具返回等未得到最终结果时，禁止使用"<final>"标签。5. "</final>"标签表示完全回答完毕，在该标签后，禁止输出任何文本，禁止进行任何的工具调用，禁止输出任何工具调用结果。 6. 在相邻的标签中间，输出一个空行，以更好的分隔标签
    // 问题是关于问界汽车的，请使用提供的工具查询对应的汽车型号信息，回答时必须包含具体的配置型号，如果问题指定了汽车的型号，回答时需要非常详细的介绍该型号信息，并通过工具获取对应的图片地址，在介绍车型后展示
    // 问界新M5 增程 Max, 问界新M5 增程 Max RS, 问界新M5 纯电 Max, 问界M5 EV, 问界M7 Plus 五座后驱版, 问界M7 Plus 五座四驱版, 问界M7 Plus 六座后驱版, 问界M7 Plus 六座四驱版, Seres 7, 问界M9 增程版 六座, 问界M9 增程版 五座, 问界M9 纯电版
    static {
        // 问界M5系列
        List<String> m5CarImages = new ArrayList<>();
        m5CarImages.add("M5_Exterior_1" + FILE_SUFFIX);
        m5CarImages.add("M5_Exterior_2" + FILE_SUFFIX);
        m5CarImages.add("M5_Game" + FILE_SUFFIX);
        m5CarImages.add("M5_Trim" + FILE_SUFFIX);
        carImageMap.put("问界新M5 增程 Max", m5CarImages);
        carImageMap.put("问界新M5 增程 Max RS", m5CarImages);
        carImageMap.put("问界新M5 纯电 Max", m5CarImages);
        carImageMap.put("问界M5 EV", m5CarImages);

        // 问界M7系列
        List<String> m7CarImages = new ArrayList<>();
        m7CarImages.add("M7_Exterior_1" + FILE_SUFFIX);
        m7CarImages.add("M7_Exterior_2" + FILE_SUFFIX);
        m7CarImages.add("M7_Exterior_3" + FILE_SUFFIX);
        m7CarImages.add("M7_Front" + FILE_SUFFIX);
        List<String> m7_5SeatCarImages = new ArrayList<>(m7CarImages);
        m7_5SeatCarImages.add("M7_Trim_5Seat" + FILE_SUFFIX);
        List<String> m7_6SeatCarImages = new ArrayList<>(m7CarImages);
        m7_6SeatCarImages.add("M7_Trim_6Seat" + FILE_SUFFIX);
        carImageMap.put("问界M7 Plus 五座后驱版", m7_5SeatCarImages);
        carImageMap.put("问界M7 Plus 五座四驱版", m7_5SeatCarImages);
        carImageMap.put("问界M7 Plus 六座后驱版", m7_6SeatCarImages);
        carImageMap.put("问界M7 Plus 六座四驱版", m7_6SeatCarImages);
        // 若有海外rebadged版本，也可以加入（如Seres 7）
        carImageMap.put("Seres 7", Collections.singletonList("M7_Seres" + FILE_SUFFIX));

        // 问界M9系列
        List<String> m9CarImages = new ArrayList<>();
        m9CarImages.add("M9_Exterior_1" + FILE_SUFFIX);
        m9CarImages.add("M9_Exterior_2" + FILE_SUFFIX);
        m9CarImages.add("M9_Trunk" + FILE_SUFFIX);
        List<String> m9_5SeatCarImages = new ArrayList<>(m7CarImages);
        m9_5SeatCarImages.add("M9_Trim_5Seat" + FILE_SUFFIX);
        List<String> m9_6SeatCarImages = new ArrayList<>(m7CarImages);
        m9_6SeatCarImages.add("M9_Trim_6Seat" + FILE_SUFFIX);
        carImageMap.put("问界M9 增程版 六座", m9_6SeatCarImages);
        carImageMap.put("问界M9 增程版 五座", m9_5SeatCarImages);
        carImageMap.put("问界M9 纯电版", m9CarImages);
    }

    @Override
    @Fitable("default")
    @ToolMethod(name = "问界车型信息查询", description = "用于查询问界的车型信息", extensions = {
            @Attribute(key = "tags", value = "FIT")})
    @Property(description = "问界车型介绍")
    public String allDescribe(String args) {
        return describes.get();
    }

    @Override
    @Fitable("default")
    @ToolMethod(name = "问界车型宣传图片", description = "用于查询问界车型宣传图片", extensions = {
            @Attribute(key = "tags", value = "FIT")})
    @Property(description = "问界车型的宣传图片的访问地址")
    public List<String> url(String carType) {
        List<String> res = Optional.ofNullable(carImageMap.get(carType)).orElse(Collections.singletonList(DEFAULT_URL));
        log.warn("type:{} image is: {}", carType, res);
        return res;
    }

    private String readDescribes() {
        return readResourceFile("data/a.txt");
    }

    /**
     * 读取resources下的文件内容
     * @param resourcePath resources目录下的相对路径
     * @return 文件内容字符串
     */
    public static String readResourceFile(String resourcePath) {
        ClassLoader classLoader = WenjieServiceImpl.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("文件不存在：" + resourcePath);
            }
            return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败：" + resourcePath, e);
        }
    }
}
