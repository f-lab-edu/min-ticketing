package com.flab.ticketing.common.config

import com.flab.ticketing.common.utils.TraceIdHolder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor


@EnableAsync
@Configuration
class AsyncThreadPoolConfig {


    @Bean
    fun asyncExecutor1(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        val taskDecorator = TaskDecorator {
            val currentTraceId = TraceIdHolder.get() ?: throw Exception()
            Runnable {
                TraceIdHolder.set(currentTraceId)
                it.run()
                TraceIdHolder.release()
            }
        }
        executor.setTaskDecorator(
            taskDecorator
        )
        executor.initialize()
        return executor
    }

}