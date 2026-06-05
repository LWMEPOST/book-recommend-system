package com.book;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.book.mapper")
@EnableScheduling
public class BookRecommendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookRecommendApplication.class, args);
    }
}
