package com.ikuai.controller;


import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ikuai.entity.IkuaiParam;
import com.ikuai.result.Result;
import com.ikuai.service.RouterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * @description:
 * @author: TangLei
 * @date: 2025/12/15 15:28
 */
@RestController
@RequestMapping("/api/ikuai")
@ResponseBody
public class IkuaiController {

    @Resource
    RouterService routerService;

    @PostMapping("/submitDynamicForm")
    public ResponseEntity submitDynamicForm(@RequestBody IkuaiParam param) {
        if (!param.getIkuaiIp().contains("http")) {
            param.setIkuaiIp("http://" + param.getIkuaiIp());
        }

        if (StringUtils.isNotEmpty(param.getBlockAddress())) {
            String blockAddress = param.getBlockAddress();
            if (blockAddress.contains(",") || blockAddress.contains("，")) {
                param.setBlockAddress(blockAddress.replaceAll("，", ","));
            } else if (blockAddress.contains("\n")) {
                String[] split = blockAddress.split("\n");
                param.setBlockAddress(String.join(",", split));
            }
        }
        String projectPath = System.getProperty("user.dir") + File.separator + "ikuai.json";
        String jsonString = JSON.toJSONString(param);
        FileUtil.writeUtf8String(jsonString, projectPath);
        //是否立即执行
        if (param.getIsExecute()) {
            return ResponseEntity.ok(routerService.submitDynamicForm(param));
        }

        return Result.ok(param);
    }

    @GetMapping("/echo")
    public ResponseEntity echo() {
        String s = null;
        try {
            s = FileUtil.readUtf8String(System.getProperty("user.dir") + File.separator + "ikuai.json");
        } catch (IORuntimeException e) {
            s = JSON.toJSONString(new IkuaiParam());
            FileUtil.writeUtf8String(s, System.getProperty("user.dir") + File.separator + "ikuai.json");
        }
        JSONObject jsonObject = JSON.parseObject(s);
        return Result.ok(jsonObject);
    }


    public static void main(String[] args) {
        IkuaiParam ikuaiParam = new IkuaiParam();
        ikuaiParam.setPassword("password");
        IkuaiParam ikuaiParam1 = new IkuaiParam();
        ikuaiParam1.setPassword("password1");

        System.out.println(Optional.ofNullable(ikuaiParam.getIkuaiIp()));
        List<IkuaiParam> list = Arrays.asList(ikuaiParam, ikuaiParam1);
        HashSet<IkuaiParam> set = new HashSet<>(list);
        set.add(ikuaiParam);
        set.add(ikuaiParam1);


        ArrayList<IkuaiParam> distinct = CollectionUtil.distinct(list);
        System.out.println(distinct);


    }
}