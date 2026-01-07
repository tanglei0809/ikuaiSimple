package com.ikuai.util;


import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * @description:
 * @author: TangLei
 * @date: 2026/1/7 10:44
 */
public class FileUtils {


    /**
     * 通过运行环境返回路径
     *
     * @return
     */
    public static String buildOsSpecificPath(String windowsPath, String linuxPath, String fileName) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            // Windows路径
            if (StringUtils.isEmpty(windowsPath)) {
                windowsPath = System.getProperty("user.dir");
            }
            return windowsPath + File.separator + fileName;
        } else {
            // Linux/Unix路径
            if (StringUtils.isEmpty(windowsPath)) {
                linuxPath = File.separator + "root";
            }
            return linuxPath + File.separator + fileName;
        }
    }
}