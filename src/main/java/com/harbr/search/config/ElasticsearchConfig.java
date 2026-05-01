package com.harbr.search.config;

import com.harbr.search.domain.PropertyDocument;
import com.harbr.search.infrastructure.PropertySearchRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(name = "harbr.search.elasticsearch.enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackageClasses = PropertySearchRepository.class)
public class ElasticsearchConfig {
}