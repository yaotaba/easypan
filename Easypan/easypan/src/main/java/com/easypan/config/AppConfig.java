package com.easypan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Value("${spring.mail.username}")
    private String sendUsername;
    @Value("${admin.emails}")
    private String adminEmails;
    @Value("${project.folder}")
    private String projectFolder;

    public String getProjectFolder() {
        return projectFolder;
    }

    public String getSendUsername() {
        return sendUsername;
    }

    public String getAdminEmails() {
        return adminEmails;
    }
}
