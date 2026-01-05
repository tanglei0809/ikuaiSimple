package com.ikuai.entity;


import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: TangLei
 * @date: 2025/12/15 14:21
 */
@Data
public class IkuaiParam {

    //"是否马上执行")
    private Boolean isExecute = false;

    //"ikuai地址")
    private String ikuaiIp;

    //"ikuai用户密码")
    private String password;

    //"ikuai用户账号")
    private String userName;

    //"自定添加ip")
    private List<String> blockAddress;

    //"更新类型")
    private String updateType;

    //"请求获取ip的地址集合")
    private List<String> getIpUrls;

    //"请求获取ip的地址集合")
    private List<String> dstNatIds;



}