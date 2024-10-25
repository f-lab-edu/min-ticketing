package com.flab.ticketing.common.service

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.support.CompositeCacheManager
import java.util.concurrent.Callable

class CustomCompositeCacheManager(
    private val cacheManagers : List<CacheManager>
) : CompositeCacheManager(){

    init {
        super.setCacheManagers(cacheManagers)
    }

    override fun getCache(name: String): Cache? {
        val foundCaches = cacheManagers.mapNotNull { it.getCache(name) }

        if (foundCaches.isEmpty()) {
            return null
        }

        return CompositeCache(name, foundCaches)
    }
}

internal class CompositeCache(
    private val name: String,
    private val caches: List<Cache>
) : Cache {

    override fun getName(): String = name

    override fun getNativeCache(): Any = this

    override fun get(key: Any): Cache.ValueWrapper? {
        return caches.firstNotNullOfOrNull { it.get(key) }
    }

    override fun <T : Any?> get(key: Any, type: Class<T>?): T? {
        return caches.firstNotNullOfOrNull { it.get(key, type) }
    }

    override fun <T : Any?> get(key: Any, valueLoader: Callable<T>): T? {
        val value = caches.firstOrNull()?.get(key, valueLoader)
        if (value != null) {
            caches.drop(1).forEach { cache ->
                cache.put(key, value)
            }
        }
        return value
    }

    override fun put(key: Any, value: Any?) {
        // 모든 캐시에 값을 저장
        caches.forEach { it.put(key, value) }
    }

    override fun evict(key: Any) {
        // 모든 캐시에서 해당 키의 값을 제거
        caches.forEach { it.evict(key) }
    }

    override fun clear() {
        // 모든 캐시를 초기화
        caches.forEach { it.clear() }
    }
}