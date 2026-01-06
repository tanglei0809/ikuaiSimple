package com.ikuai.controller;


import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ikuai.entity.IkuaiParam;
import com.ikuai.entity.UpdateParam;
import com.ikuai.result.Result;
import com.ikuai.service.RouterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // 支持的文件扩展名
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("json");

    private static final String projectPath = System.getProperty("user.dir") + File.separator + "ikuai.json";


    @PostMapping("/updateDstNatList")
    public ResponseEntity updateDstNatList(@RequestBody List<UpdateParam> params) {
        return ResponseEntity.ok(routerService.updateDstNatList(params));
    }



    @PostMapping("/getDstNatList")
    public ResponseEntity getDstNatList(@RequestBody IkuaiParam param) {
        if (!param.getIkuaiIp().contains("http")) {
            param.setIkuaiIp("http://" + param.getIkuaiIp());
        }
            return ResponseEntity.ok(routerService.getDstNatList(param));
    }

    @PostMapping("/submitDynamicForm")
    public ResponseEntity submitDynamicForm(@RequestBody IkuaiParam param) {
        if (!param.getIkuaiIp().contains("http")) {
            param.setIkuaiIp("http://" + param.getIkuaiIp());
        }

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
            s = FileUtil.readUtf8String(projectPath);
        } catch (IORuntimeException e) {
            IkuaiParam ikuaiParam = new IkuaiParam();
            ikuaiParam.setGetIpUrls(Arrays.asList("https://www.ipdeny.com/ipblocks/data/countries/cn.zone", "https://metowolf.github.io/iplist/data/special/china.txt"));
            s = JSON.toJSONString(ikuaiParam);
            FileUtil.writeUtf8String(s, projectPath);
        }
        JSONObject jsonObject = JSON.parseObject(s);
        JSONArray jsonArray = jsonObject.getJSONArray("getIpUrls");
        if (CollectionUtil.isEmpty(jsonArray)) {
            jsonObject.put("getIpUrls", Arrays.asList("https://www.ipdeny.com/ipblocks/data/countries/cn.zone", "https://metowolf.github.io/iplist/data/special/china.txt"));
        }
        return Result.ok(jsonObject);
    }

    @PostMapping("/upload")
    public ResponseEntity upload(@RequestParam("file") MultipartFile file) throws IOException {

        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                return Result.fail("请选择要上传的文件");
            }

            // 验证文件扩展名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.fail("文件名不能为空");
            }

            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                return Result.fail("只支持 .json 和 .txt 文件格式");
            }

            // 验证文件大小（限制10MB）
            if (file.getSize() > 10 * 1024 * 1024) {
                return Result.fail("文件大小不能超过10MB");
            }

            // 保存文件
            FileUtil.writeBytes(file.getBytes(), projectPath);

        } catch (Exception e) {
            return Result.fail("文件上传失败：" + e.getMessage());
        }
        return Result.ok("上传成功");
    }


    @GetMapping("/download")
    public void download(HttpServletResponse response) {
        File file = FileUtil.file(projectPath);
        // 设置响应头，指定下载时的文件名（解决中文乱码问题）
        response.setHeader("Content-Disposition", "attachment;filename=" + "ikuai.json");
        response.setContentType("application/octet-stream");
        // 写入响应
        ServletUtil.write(response, file);
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
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