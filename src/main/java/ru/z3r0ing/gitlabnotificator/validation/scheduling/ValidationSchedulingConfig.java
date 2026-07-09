package ru.z3r0ing.gitlabnotificator.validation.scheduling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class ValidationSchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler labelValidationTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("label-validation-");
        return scheduler;
    }
}
