package com.harbr.search.infrastructure;

import com.harbr.search.domain.PropertyDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.UUID;

public interface PropertySearchRepository extends ElasticsearchRepository<PropertyDocument, UUID> {

    Page<PropertyDocument> findByTitleOrDescriptionOrCity(
            String title, String description, String city, Pageable pageable);
}