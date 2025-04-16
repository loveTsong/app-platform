/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package modelengine.fit.jade.wenjie.data;

import modelengine.fitframework.annotation.Genericable;
import modelengine.fitframework.annotation.Property;
import modelengine.fel.tool.annotation.Group;
import modelengine.fel.tool.annotation.ToolMethod;

import java.util.List;

/**
 * TODO
 *
 * @author 夏斐
 * @since 2025/3/17
 */
@Group(name = "Wenjie")
public interface WenjieService {
    @ToolMethod(name = "问界产品信息查询", description = "用于查询问界产品信息查询")
    @Genericable("modelengine.jober.aipp.wenjie.describe")
    String allDescribe(@Property(description = "预留参数，当前传入空字符串即可", required = true) String args);

    @ToolMethod(name = "问界产品宣传图", description = "用于查询问界产品宣传图")
    @Genericable("modelengine.jober.aipp.wenjie.url")
    List<String> url(@Property(description = "车的型号", required = true) String carType);
}
