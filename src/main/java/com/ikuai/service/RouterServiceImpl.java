package com.ikuai.service;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
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
 * @description:
 * @author: TangLei
 * @date: 2025/12/12 14:58
 */
@Service
@Slf4j
public class RouterServiceImpl implements RouterService {

    @Override
    public Object submitDynamicForm(IkuaiParam param) {
        return updateLogic(param);
    }

    @Override
    public Object getDstNatList(IkuaiParam param) {
        boolean b = loginIkuai(param);
        if (!b) {
            return "登录失败";
        }
        //获取端口映射列表
        String dnat = HttpUtil.post(param.getIkuaiIp() + "/Action/call", "{\"func_name\":\"dnat\",\"action\":\"show\",\"param\":{\"TYPE\":\"total,data\",\"limit\":\"0,100\",\"ORDER_BY\":\"\",\"ORDER\":\"\"}}");
        JSONObject jsonObject2 = JSON.parseObject(dnat);
        Integer result1 = jsonObject2.getInteger("Result");
        String errMsg1 = jsonObject2.getString("ErrMsg");
        if (result1 == 30000 && errMsg1.equals("Success")) {
            return jsonObject2.getJSONObject("Data").getJSONArray("data");
        }

        return null;
    }

    @Scheduled(cron = "0 0 0 */3 * ?")
    public void taskUpdate() {
        //读取写入本地的配置
        String projectPath = System.getProperty("user.dir") + File.separator + "ikuai.json";
        boolean exist = FileUtil.exist(projectPath);
        //文件是否存在
        if (exist) {
            String s = FileUtil.readUtf8String(projectPath);
            IkuaiParam param = JSON.parseObject(s, IkuaiParam.class);
            if (StringUtils.isNotEmpty(param.getIkuaiIp()) && StringUtils.isNotEmpty(param.getUserName()) && StringUtils.isNotEmpty(param.getPassword())) {
                log.info("-----------------------定时更新开始执行----------------");
                updateLogic(param);
            }


        }
    }


    /**
     * 执行逻辑
     *
     * @param param
     * @return
     */
    private String updateLogic(IkuaiParam param) {
        log.info("-----------------------开始更新----------------");
        //登录
        boolean b = loginIkuai(param);
        if (!b) {
            return "登录失败";
        }
        //IP分组添加集合
        List<JSONObject> addGroupIp = new ArrayList<>();
        //获取到ip分组列表
        String ipGroupListParam = "{\"func_name\":\"ipgroup\",\"action\":\"show\",\"param\":{\"TYPE\":\"total,data\",\"limit\":\"0,100\",\"ORDER_BY\":\"\",\"ORDER\":\"\"}}";
        String ipGroupListPost = HttpUtil.post(param.getIkuaiIp() + "/Action/call", ipGroupListParam);
        if (StringUtils.isNotEmpty(ipGroupListPost)) {
            JSONObject jsonObject = JSON.parseObject(ipGroupListPost);
            Integer result = jsonObject.getInteger("Result");
            String errMsg = jsonObject.getString("ErrMsg");
            if (result == 30000 && errMsg.equals("Success")) {
                JSONArray data = jsonObject.getJSONObject("Data").getJSONArray("data");
                if (CollectionUtil.isNotEmpty(data)) {
                    List<JSONObject> collect = data.toJavaList(JSONObject.class).stream().filter(item -> !item.getString("group_name").contains("国内") && !item.getString("group_name").isEmpty()).collect(Collectors.toList());
                    if (CollectionUtil.isNotEmpty(collect)) {
                        List<JSONObject> jsonObjectList = new ArrayList<>();
                        for (JSONObject object : collect) {
                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("type", object.getInteger("type"));
                            jsonObject1.put("newRow", true);
                            jsonObject1.put("comment", "");
                            jsonObject1.put("group_name", object.getString("group_name"));
                            jsonObject1.put("addr_pool", object.getString("addr_pool"));
                            JSONObject requestObject = new JSONObject();
                            requestObject.put("func_name", "ipgroup");
                            requestObject.put("action", "add");
                            requestObject.put("param", jsonObject1);
                            jsonObjectList.add(requestObject);
                        }
                        addGroupIp.addAll(jsonObjectList);
                    }
                }
                //生成国内ip配置
                List<JSONObject> jsonObjects = genertatorChinaIp(param);
                if (CollectionUtil.isEmpty(jsonObjects)) {
                    //清除旧的配置
//                    List<String> ids = data.stream().map(item -> {
//                        JSONObject itemObject = (JSONObject) item;
//                        return itemObject.getString("id");
//                    }).collect(Collectors.toList());
//                    String delParamSource = "{\"func_name\":\"ipgroup\",\"action\":\"del\",\"param\":{\"id\":\"%s\"}}\n";
//                    String delParam = String.format(delParamSource, String.join(",", ids));
//                    String delPost = HttpUtil.post(param.getIkuaiIp() + "/Action/call", delParam);
                }

                addGroupIp.addAll(jsonObjects);
                List<String> groupName = addGroupIp.stream().map(item -> item.getJSONObject("param").getString("group_name")).collect(Collectors.toList());
                for (JSONObject object : addGroupIp) {
                    try {
                        String add = HttpUtil.post(param.getIkuaiIp() + "/Action/call", object.toJSONString());
                    } catch (Exception e) {
                        log.info("添加失败:" + object.toJSONString());
                    }
                }
                //获取端口映射列表
                String dnat = HttpUtil.post(param.getIkuaiIp() + "/Action/call", "{\"func_name\":\"dnat\",\"action\":\"show\",\"param\":{\"TYPE\":\"total,data\",\"limit\":\"0,100\",\"ORDER_BY\":\"\",\"ORDER\":\"\"}}");
                JSONObject jsonObject2 = JSON.parseObject(dnat);
                Integer result1 = jsonObject2.getInteger("Result");
                String errMsg1 = jsonObject2.getString("ErrMsg");
                if (result1 == 30000 && errMsg1.equals("Success")) {
                    JSONArray dataArray = jsonObject2.getJSONObject("Data").getJSONArray("data");
                    if (CollectionUtil.isNotEmpty(dataArray)) {
                        List<JSONObject> dstNatList = dataArray.toJavaList(JSONObject.class);
                        if (CollectionUtil.isNotEmpty(param.getDstNatIds())){
                            //筛选是否选择了端口映射列表
                            dstNatList = dstNatList.stream().filter(item -> param.getDstNatIds().contains(item.getString("id"))).collect(Collectors.toList());
                        }
                        for (int i = 0; i < dstNatList.size(); i++) {
                            JSONObject jsonObject3 = dataArray.getJSONObject(i);
                            String join = String.join(",", groupName);
                            String src_addr = jsonObject3.getString("src_addr");
                            if (StringUtils.isNotEmpty(src_addr)) {
                                if (src_addr.contains("国内")) {
                                    String[] split = src_addr.split(",");
                                    if (split.length > 1) {
                                        List<String> collect = Arrays.stream(split).filter(item -> !item.contains("国内") && !item.isEmpty()).collect(Collectors.toList());
                                        if (CollectionUtil.isNotEmpty(collect)) {
                                            src_addr = String.join(",", collect) + ",";
                                        } else {
                                            src_addr = "";
                                        }
                                    }
                                } else {
                                    src_addr = src_addr + ",";
                                }
                            }
                            jsonObject3.put("src_addr", src_addr + join);
                            JSONObject requestObj = new JSONObject();
                            requestObj.put("func_name", "dnat");
                            requestObj.put("action", "edit");
                            requestObj.put("param", jsonObject3);
                            String response = HttpUtil.post(param.getIkuaiIp() + "/Action/call", requestObj.toJSONString());
                            log.info("更新结果:" + response);
                        }
                    }
                }
            }
        }
        log.info("=================更新结束===============");
        return "更新成功";
    }

    public static void main(String[] args) throws Exception {
        getChinaIp();

    }

    /**
     * 读取本地(方便新增)获取ip的列表来生国内ip
     *
     * @return
     * @throws Exception
     */
    public static List<String> getChinaIp() throws Exception {
        String urlString = "https://www.ipdeny.com/ipblocks/data/countries/cn.zone";
        String urlString2 = "https://metowolf.github.io/iplist/data/special/china.txt";
        List<String> urlList;

        //读取写入本地的配置
        String projectPath = System.getProperty("user.dir") + File.separator + "ikuai.json";
        boolean exist = FileUtil.exist(projectPath);
        if (exist) {
            String s = FileUtil.readUtf8String(projectPath);
            IkuaiParam ikuaiParam = JSONObject.parseObject(s, IkuaiParam.class);
            List<String> getIpUrls = ikuaiParam.getGetIpUrls();
            urlList = getIpUrls;
        } else {
            List<String> lines = new ArrayList<>();
            lines.add(urlString);
            lines.add(urlString2);
            urlList = lines;
            FileUtil.appendUtf8Lines(lines, projectPath);
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String url : urlList) {
            // 使用 Hutool HttpUtil 获取内容
            HttpResponse response = HttpRequest.get(url).execute();
            if (!response.isOk()) {
                continue;
            }
            String content = response.body();
            stringBuilder.append(content);
        }
        if (StringUtils.isEmpty(stringBuilder.toString())) {
            throw new Exception("获取ip失败");
        }
        String[] split = stringBuilder.toString().split("\n");
        return Arrays.stream(split).distinct().sorted().collect(Collectors.toList());
    }


    /**
     * 生成国内ip文件
     *
     * @param
     */
    public List<JSONObject> genertatorChinaIp(IkuaiParam param) {
        int linesPerBatch = 980; // 每批次 980 行
        List<JSONObject> jsonObjectList = new ArrayList<>();
        try {

            List<String> chinaIps = getChinaIp();
            int length = chinaIps.size();
            // 处理每 980 行数据并拼接
            List<String> currentBatch = new ArrayList<>();
            int nameIndex = 1;
            for (int i = 0; i < length; i++) {
                // 将当前行添加到当前批次
                currentBatch.add(chinaIps.get(i));
                if (currentBatch.size() == linesPerBatch || i == length - 1) {
                    String addIp = "";
                    if (i == length - 1) {
                        if (CollectionUtil.isNotEmpty(param.getBlockAddress())) {
                            addIp = "," + String.join(",", param.getBlockAddress());
                        }
                    }
                    String addrName = "国内ip-" + DateUtil.format(new Date(), "yyyyMMdd") + "-" + nameIndex;
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("type", 0);
                    jsonObject1.put("newRow", true);
                    jsonObject1.put("comment", "");
                    jsonObject1.put("group_name", addrName);
                    jsonObject1.put("addr_pool", String.join(",", currentBatch) + addIp);
                    JSONObject requestObject = new JSONObject();
                    requestObject.put("func_name", "ipgroup");
                    requestObject.put("action", "add");
                    requestObject.put("param", jsonObject1);
                    jsonObjectList.add(requestObject);
                    currentBatch.clear();
                    nameIndex++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取文件ip失败");
        }
        log.info("-----------------------获取新的国内ip并生成文件成功----------------");
        return jsonObjectList;
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

        String response = null;
        try {
            response = HttpUtil.createPost(param.getIkuaiIp() + "/Action/login").body("{\"username\":\"" + param.getUserName() + "\",\"passwd\":\"" + passwd + "\",\"pass\":\"" + pass + "\",\"remember_password\":\"true\"}")  // 发送文件
                    .execute().body();
        } catch (Exception e) {
            throw new RuntimeException("登录失败");
        }
        JSONObject jsonObject = JSON.parseObject(response);
        Integer result1 = jsonObject.getInteger("Result");
        String errMsg1 = jsonObject.getString("ErrMsg");
        if (result1 == 10000 && errMsg1.equals("Success")) {
            log.info("-----------------------登录成功----------------");
            return true;
        }
        return false;
    }

}