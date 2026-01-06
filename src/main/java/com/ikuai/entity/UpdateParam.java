package com.ikuai.entity;


import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: TangLei
 * @date: 2025/12/15 14:21
 */
@Data
public class UpdateParam {

    //"id")
    private String id;

    //"内网ip")
    private String lan_addr;

    //"选中的分组名")
    private List<String> groupNames;

}