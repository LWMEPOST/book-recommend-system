package com.book.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.admin-bootstrap")
public class AdminBootstrapProperties {

    private boolean enabled = true;

    private boolean syncPasswordOnStartup = true;

    private String username = "admin";

    private String password = "admin123456";

    private String nickname = "Admin";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSyncPasswordOnStartup() {
        return syncPasswordOnStartup;
    }

    public void setSyncPasswordOnStartup(boolean syncPasswordOnStartup) {
        this.syncPasswordOnStartup = syncPasswordOnStartup;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
