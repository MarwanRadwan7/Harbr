package com.harbr.search.config;

import com.harbr.search.domain.PropertyDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "harbr.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexInitializer implements CommandLineRunner {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void run(String... args) {
        IndexOperations indexOps = elasticsearchOperations.indexOps(PropertyDocument.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
            log.info("Created Elasticsearch index for PropertyDocument");
        }
    }
}