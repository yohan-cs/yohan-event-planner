package com.yohan.event_planner.config;

import com.yohan.event_planner.graphql.scalar.DateScalar;
import com.yohan.event_planner.graphql.scalar.DateTimeScalar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(DateTimeScalar.DATE_TIME)
                .scalar(DateScalar.DATE);
    }

}
