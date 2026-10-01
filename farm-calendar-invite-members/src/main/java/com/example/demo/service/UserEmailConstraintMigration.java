package com.example.demo.service;

import java.sql.Connection;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * メールアドレスは複数のメンバーアカウントで再利用できるようにしつつ、
 * 同じメールアドレスの管理者(OWNER)は1件だけに制限する。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserEmailConstraintMigration implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public UserEmailConstraintMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase(Locale.ROOT).contains("postgresql")) {
                return;
            }
        }

        jdbcTemplate.execute("ALTER TABLE app_user DROP CONSTRAINT IF EXISTS uk_app_user_email");
        jdbcTemplate.execute("DROP INDEX IF EXISTS uk_app_user_email");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_app_user_email_lower ON app_user (LOWER(email))");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_app_user_owner_email "
                + "ON app_user (LOWER(email)) WHERE UPPER(farm_role) = 'OWNER'");
    }
}
