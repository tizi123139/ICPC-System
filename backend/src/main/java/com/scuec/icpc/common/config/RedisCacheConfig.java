package com.scuec.icpc.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存配置：统一前缀避免与同实例其他业务冲突，按缓存名设置不同 TTL。
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * 缓存键统一前缀，便于在共享 Redis 中区分本系统的数据。
     */
    public static final String KEY_PREFIX = "icpc:cache:";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.scuec.icpc.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 缓存值需要保留类型信息，否则 List、嵌套 DTO 反序列化会退化为 Map
        objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(KEY_PREFIX)
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                // 知识点树：极少变更，写操作时主动失效
                .withCacheConfiguration("tagTree", defaults.entryTtl(Duration.ofHours(24)))
                // 统计数据：聚合查询较重，短 TTL 容忍少量延迟
                .withCacheConfiguration("statisticsMembers", defaults.entryTtl(Duration.ofSeconds(60)))
                .withCacheConfiguration("statisticsTraining", defaults.entryTtl(Duration.ofSeconds(60)))
                .build();
    }
}
