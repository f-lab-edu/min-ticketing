package com.flab.ticketing.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.flab.ticketing.common.enums.CacheType
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration


@Configuration
class CacheConfig {

    @Bean
    fun redisCacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {

        val cacheConfig = cacheConfiguration()

        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory)
            .cacheDefaults(cacheConfig)
            .withInitialCacheConfigurations(typedCacheConfiguration())
            .build()

    }


    /**
     *  Redis 캐시 설정 구성(Key : String, Value : JSON, TTL : 30m)
     */
    private fun cacheConfiguration(): RedisCacheConfiguration {
        val keySerializer = RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
        val valueSerializer = getValueSerializer()

        val cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(keySerializer)
            .serializeValuesWith(valueSerializer)
            .entryTtl(Duration.ofMinutes(30L))
        return cacheConfig
    }

    fun getValueSerializer(): RedisSerializationContext.SerializationPair<Any> {
        // objectMapper가 모든 JSON을 직렬화/역직렬화하도록 설정
        val typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Any::class.java)
            .build()

        // Jackson ObjectMapper 설정(DateTime 처리, Kotlin 처리)
        val objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
            activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL)
        }

        val jsonSerializer = GenericJackson2JsonRedisSerializer(objectMapper)

        return RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
    }

    fun typedCacheConfiguration(): Map<String, RedisCacheConfiguration> {
        return CacheType.values().associateBy(
            keySelector = { it.cacheName },
            valueTransform = { cacheConfiguration().entryTtl(it.ttl) }
        )
    }

}