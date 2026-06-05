package com.book.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.book.common.Constants;
import com.book.entity.User;
import com.book.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties properties;

    public AdminBootstrapRunner(UserMapper userMapper,
                                PasswordEncoder passwordEncoder,
                                AdminBootstrapProperties properties) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(properties.getUsername()) || !StringUtils.hasText(properties.getPassword())) {
            log.warn("Skip admin bootstrap because username or password is blank.");
            return;
        }

        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, properties.getUsername())
                .last("LIMIT 1"));

        if (admin == null) {
            createAdmin();
            return;
        }

        boolean updated = false;
        if (admin.getRole() == null || admin.getRole() != Constants.ROLE_ADMIN) {
            admin.setRole(Constants.ROLE_ADMIN);
            updated = true;
        }
        if (admin.getStatus() == null || admin.getStatus() != Constants.USER_STATUS_NORMAL) {
            admin.setStatus(Constants.USER_STATUS_NORMAL);
            updated = true;
        }
        if (!StringUtils.hasText(admin.getNickname()) && StringUtils.hasText(properties.getNickname())) {
            admin.setNickname(properties.getNickname());
            updated = true;
        }
        if (properties.isSyncPasswordOnStartup() && !matchesConfiguredPassword(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(properties.getPassword()));
            updated = true;
        }

        if (updated) {
            userMapper.updateById(admin);
            log.info("Synchronized default admin account: {}", properties.getUsername());
        }
    }

    private void createAdmin() {
        User user = new User();
        user.setUsername(properties.getUsername());
        user.setPassword(passwordEncoder.encode(properties.getPassword()));
        user.setNickname(StringUtils.hasText(properties.getNickname()) ? properties.getNickname() : properties.getUsername());
        user.setRole(Constants.ROLE_ADMIN);
        user.setStatus(Constants.USER_STATUS_NORMAL);
        userMapper.insert(user);
        log.info("Created default admin account: {}", properties.getUsername());
    }

    private boolean matchesConfiguredPassword(String encodedPassword) {
        return StringUtils.hasText(encodedPassword)
                && passwordEncoder.matches(properties.getPassword(), encodedPassword);
    }
}
