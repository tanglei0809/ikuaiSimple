package com.ikuai.service;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ikuai.entity.IkuaiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 旧版方法,使用的是导入接口
 * @description:
 * @author: TangLei
 * @date: 2025/12/12 14:58
 */
@Service
@Slf4j
public class RouterServiceImplOld {


    public static void main1(String[] args) {
        String projectPath = System.getProperty("user.dir") + File.separator + "ikuai.json";
        String s = FileUtil.readUtf8String(projectPath);

        IkuaiParam jsonObject = JSON.parseObject(s, IkuaiParam.class);
        System.out.println(jsonObject);

    }

    public static void main(String[] args) {
        IkuaiParam ikuaiParam = new IkuaiParam();
        ikuaiParam.setIkuaiIp("ikuaiIp1");
        ikuaiParam.setPassword("password1");
        ikuaiParam.setUserName("userName1");

        String projectPath = System.getProperty("user.dir") + File.separator + "ikuai.json";
        FileUtil.writeUtf8String(JSON.toJSONString(ikuaiParam), projectPath);
    }



    public Object submitDynamicForm(IkuaiParam param) {
        log.info("-----------------------开始更新----------------");
        //登录
        loginIkuai(param);
        log.info("-----------------------登录成功----------------");
        //获取到ip分组列表
        String ipGroupListParam = "{\"func_name\":\"ipgroup\",\"action\":\"show\",\"param\":{\"TYPE\":\"total,data\",\"limit\":\"0,100\",\"ORDER_BY\":\"\",\"ORDER\":\"\"}}";
        String ipGroupListPost = HttpUtil.post(param.getIkuaiIp() + "/Action/call", ipGroupListParam);
        if (StringUtils.isNotEmpty(ipGroupListPost)) {
            JSONObject jsonObject = JSON.parseObject(ipGroupListPost);
            Integer result = jsonObject.getInteger("Result");
            String errMsg = jsonObject.getString("ErrMsg");

            if (result == 30000 && errMsg.equals("Success")) {
                update(jsonObject, param);
            }
        }
        return null;
    }


    @Scheduled(cron = "0 0 0 */3 * ?")
    public void taskUpdate() {
        log.info("-----------------------开始更新----------------");
        //读取写入本地的配置
        String projectPath = System.getProperty("user.dir") + File.separator + "ikuai.json";
        String s = FileUtil.readUtf8String(projectPath);
        IkuaiParam param = JSON.parseObject(s, IkuaiParam.class);
        //登录
        loginIkuai(param);
        log.info("-----------------------登录成功----------------");
        //获取到ip分组列表
        String ipGroupListParam = "{\"func_name\":\"ipgroup\",\"action\":\"show\",\"param\":{\"TYPE\":\"total,data\",\"limit\":\"0,100\",\"ORDER_BY\":\"\",\"ORDER\":\"\"}}";
        String ipGroupListPost = HttpUtil.post(param.getIkuaiIp() + "/Action/call", ipGroupListParam);
        if (StringUtils.isNotEmpty(ipGroupListPost)) {
            JSONObject jsonObject = JSON.parseObject(ipGroupListPost);
            Integer result = jsonObject.getInteger("Result");
            String errMsg = jsonObject.getString("ErrMsg");
            if (result == 30000 && errMsg.equals("Success")) {
                update(jsonObject, param);


            }
        }
    }

    /**
     * 更新
     *
     * @param jsonObject
     */
    private void update1(JSONObject jsonObject) {
        JSONArray data = jsonObject.getJSONObject("Data").getJSONArray("data");
        if (CollectionUtil.isNotEmpty(data)) {
            List<String> ids = data.stream().map(item -> {
                JSONObject itemObject = (JSONObject) item;
                return itemObject.getString("id");
            }).collect(Collectors.toList());
        }
    }

    /**
     * 更新
     *
     * @param jsonObject
     */
    private void update(JSONObject jsonObject, IkuaiParam param) {
        JSONArray data = jsonObject.getJSONObject("Data").getJSONArray("data");
        if (CollectionUtil.isNotEmpty(data)) {
            List<String> ids = data.stream().map(item -> {
                JSONObject itemObject = (JSONObject) item;
                return itemObject.getString("id");
            }).collect(Collectors.toList());
            //清除旧的配置
            String delParamSource = "{\"func_name\":\"ipgroup\",\"action\":\"del\",\"param\":{\"id\":\"%s\"}}\n";
            String delParam = String.format(delParamSource, ids);
            String delPost = HttpUtil.post(param.getIkuaiIp() + "/Action/call", delParam);
        }
        //生成新的文件
        Set<String> srcAddrSet = genertatorChinaIpFile(param);
        log.info("-----------------------获取新的国内ip并生成文件成功----------------");
        // 接口URL
        String url = param.getIkuaiIp() + "/Action/upload";  // 替换为你的接口地址
        // 本地文件路径
        File file = new File(System.getProperty("user.dir") + File.separator + "ipgroup.txt");  // 替换为你的文件路径
        // 创建请求文件参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("ipgroup.txt", file);  // 参数名 "file" 替换为你的表单字段名
        // 发起POST请求上传文件
        String response = HttpUtil.createPost(url).form(paramMap)  // 发送文件
                .execute().body();  // 获

        //验证导入是否成功
        List<String> importData = FileUtil.readLines(file, "UTF-8");
        String addPost = HttpUtil.post(param.getIkuaiIp() + "/Action/call", "{\"func_name\":\"ipgroup\",\"action\":\"IMPORT\",\"param\":{\"filename\":\"ipgroup.txt\",\"append\":" + importData + "}}");
        JSONObject jsonObject1 = JSON.parseObject(addPost);
        Integer result = jsonObject1.getInteger("Result");
        String errMsg = jsonObject1.getString("ErrMsg");
        if (result == 30000 && errMsg.equals("Success")) {
            //执行成功,端口映射添加允许访问的ip
            //获取端口映射列表
            String dnat = HttpUtil.post(param.getIkuaiIp() + "/Action/call", "{\"func_name\":\"dnat\",\"action\":\"show\",\"param\":{\"TYPE\":\"total,data\",\"limit\":\"0,100\",\"ORDER_BY\":\"\",\"ORDER\":\"\"}");
            JSONObject jsonObject2 = JSON.parseObject(dnat);
            Integer result1 = jsonObject2.getInteger("Result");
            String errMsg1 = jsonObject2.getString("ErrMsg");
            if (result1 == 30000 && errMsg1.equals("Success")) {
                JSONArray data1 = jsonObject2.getJSONObject("Data").getJSONArray("data");
                if (CollectionUtil.isNotEmpty(data1)) {
                    for (int i = 0; i < data1.size(); i++) {
                        JSONObject jsonObject3 = data1.getJSONObject(i);
                        String join = String.join(",", srcAddrSet);
                        jsonObject3.put("src_addr", join);
                        JSONObject requestObj = new JSONObject();
                        requestObj.put("func_name", "dnat");
                        requestObj.put("action", "edit");
                        requestObj.put("param", jsonObject3);
                        HttpUtil.post(param.getIkuaiIp() + "/Action/call", requestObj);
                    }
                }
            }

            log.info("********执行结束,执行结果为:" + addPost);
        }
    }

    /**
     * 登录
     */
    public boolean loginIkuai(IkuaiParam param) {
        // 获取Base64编码器
        Base64.Encoder encoder = Base64.getEncoder();
        // 编码
        String pass = encoder.encodeToString(("salt_11" + param.getPassword()).getBytes());

        // 创建一个MessageDigest对象，并指定使用MD5算法
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        // 将字符串转换为字节数组，并进行哈希计算
        byte[] messageDigest = md.digest(param.getPassword().getBytes());
        // 将字节数组转换为十六进制格式的字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : messageDigest) {
            // 将每个字节转换为两位的十六进制数
            hexString.append(String.format("%02x", b));
        }
        String passwd = hexString.toString();
        String response = HttpUtil.createPost(param.getIkuaiIp() + "/Action/login").body("{\"username\":\"" + param.getUserName() + "\",\"passwd\":\"" + passwd + "\",\"pass\":\"" + pass + "\",\"remember_password\":\"true\"}")  // 发送文件
                .execute().body();
        JSONObject jsonObject = JSON.parseObject(response);
        Integer result1 = jsonObject.getInteger("Result");
        String errMsg1 = jsonObject.getString("ErrMsg");
        if (result1 == 30000 && errMsg1.equals("Success")) {
            return true;
        }
        return false;


    }

    /**
     * 生成国内ip文件
     *
     * @param
     */
    public Set<String> genertatorChinaIpFile(IkuaiParam param) {
        String projectPath = System.getProperty("user.dir");
        boolean exist = FileUtil.exist(projectPath + File.separator + "ipgroup.txt");
        int id = 1;
        String sourceData = "";
        if (exist) {
            sourceData = FileUtil.readUtf8String(projectPath + File.separator + "source.txt");
            String[] split = sourceData.split("\n");
            id = split.length;
        }
        String urlString = "https://www.ipdeny.com/ipblocks/data/countries/cn.zone";
        int linesPerBatch = 980; // 每批次 980 行
        String path = "id=%s comment=,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,, type=0 group_name=%s addr_pool=";
        Set<String> srcAddrSet = new HashSet<>();
        try {
            // 使用 Hutool HttpUtil 获取内容
            String content = HttpUtil.get(urlString);
            // 按行分割内容
            String[] lines = content.split("\n");
            int length = lines.length;
            // 处理每 980 行数据并拼接
            StringBuilder currentBatch = new StringBuilder();
            StringBuilder finalStr = new StringBuilder();
            int index = 0;
            int nameIndex = 1;
            for (int i = 0; i < lines.length; i++) {
                // 将当前行添加到当前批次
                currentBatch.append(lines[i]).append(",");
                index++;
                String addrName = "国内ip-" + DateUtil.format(new Date(), "yyyy-MM-dd") + nameIndex;
                if (index == linesPerBatch || i == length - 1) {
                    finalStr.append(String.format(path, id, addrName)).append(currentBatch.toString()).append("\n");
                    currentBatch.setLength(0);
                    index = 0;
                    id++;
                    nameIndex++;
                    continue;
                }

                srcAddrSet.add(addrName);
            }
            // 使用 Hutool 的 FileUtil.writeUtf8String 方法写入字符串到文件

            String addIp = "";
            if (StringUtils.isNotEmpty(param.getBlockAddress())) {
                addIp = "," + param.getBlockAddress();
            }
            //写入文件
            FileUtil.writeUtf8String(sourceData + "\n" + finalStr.toString() + addIp, projectPath + File.separator + "ipgroup.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return srcAddrSet;
    }
}