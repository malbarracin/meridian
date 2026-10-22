package com.meridian.users.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import com.meridian.users.user.entity.User;

/** Garantiza la unicidad del email antes de completar el arranque de producción. */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class UserIndexConfig {

    @Bean
    InitializingBean userEmailIndex(MongoTemplate mongoTemplate) {
        return () -> mongoTemplate.indexOps(User.class)
                .createIndex(new Index().on("email", Sort.Direction.ASC).unique());
    }
}
