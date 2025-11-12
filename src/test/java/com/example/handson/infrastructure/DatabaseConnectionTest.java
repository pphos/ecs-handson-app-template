package com.example.handson.infrastructure;

import com.example.handson.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * データベース接続テスト
 * TDDアプローチ: まずデータベースに接続できることを確認
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatabaseConnectionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDatabaseConnection() {
        // PostgreSQLに接続できることを確認
        String result = jdbcTemplate.queryForObject("SELECT 'Hello from PostgreSQL'", String.class);
        assertThat(result).isEqualTo("Hello from PostgreSQL");
    }

    @Test
    void testDatabaseVersion() {
        // PostgreSQL 17.5が使用されていることを確認
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertThat(version).contains("PostgreSQL 17.5");
    }

    @Test
    void testFlywayMigration() {
        // Flywayマイグレーションでusersテーブルが作成されていることを確認
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testFlywayMigrationForImageMemos() {
        // Flywayマイグレーションでimage_memosテーブルが作成されていることを確認
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'image_memos'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
}
