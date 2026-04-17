package cn.vahoa.draw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 抽奖服务启动类
 * 
 * @author vahoa
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "cn.vahoa.draw")
@EnableCaching
@EnableAsync
@EnableScheduling
public class DrawServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrawServiceApplication.class, args);
    }
}