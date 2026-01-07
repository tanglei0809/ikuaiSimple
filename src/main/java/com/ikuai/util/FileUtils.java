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
            return ensureFileSeparator(windowsPath) + fileName;
        } else {
            // Linux/Unix路径
            if (StringUtils.isEmpty(linuxPath)) {
                linuxPath =ensureFileSeparator("root");
            }
            return ensureFileSeparator(linuxPath) + fileName;
        }
    }

    /**
     * 路径格式化
     *
     * @param path
     * @return
     */
    public static String ensureFileSeparator(String path) {
        // 获取系统的文件分隔符
        String separator = File.separator;
        // 替换所有斜杠和反斜杠为系统文件分隔符
        path = path.replace("/", separator).replace("\\", separator);

        // 检查路径前后是否有文件分隔符
        if (!path.startsWith(separator)) {
            path = separator + path; // 在前面添加文件分隔符
        }
        if (!path.endsWith(separator)) {
            path = path + separator; // 在后面添加文件分隔符
        }

        return path;
    }


    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名
     */
    public static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

}