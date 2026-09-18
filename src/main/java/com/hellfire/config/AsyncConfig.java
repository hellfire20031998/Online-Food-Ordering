package com.hellfire.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables @Async (used for outbound email so requests never wait on a mail server). */
@Configuration
@EnableAsync
public class AsyncConfig {
}
