package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Slf4j
@Service
public class VersionService {

    private String version;
    private String buildTime;

    public VersionService() {
        loadVersionInfo();
    }

    private void loadVersionInfo() {
        try {
            Properties props = new Properties();
            props.load(new ClassPathResource("version.properties").getInputStream());

            this.version = props.getProperty("app.version", "V2.1.0");
            this.buildTime = props.getProperty("app.build.time",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            log.info("应用版本信息: {}, 构建时间: {}", version, buildTime);
        } catch (IOException e) {
            log.warn("无法加载版本信息，使用默认值");
            this.version = "V2.1.0";
            this.buildTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    public String getVersion() {
        return version;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public LocalDateTime getBuildDateTime() {
        try {
            return LocalDateTime.parse(buildTime.replace(" ", "T"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}