package com.ruijie.supplysystem.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionStartupChecker implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("Connected to the target MySQL server");
        } catch (Exception ex) {
            log.error("MySQL connection failed", ex);
        }

        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("Redis connection successful");
        } catch (Exception ex) {
            log.error("Redis connection failed", ex);
        }
    }
}
