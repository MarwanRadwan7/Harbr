package com.harbr.search.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.util.UUID;

@Document(indexName = "properties")
@Setting(replicas = 0)
public record PropertyDocument(
        @Id UUID id,

        @Field(type = FieldType.Text, analyzer = "standard") String title,

        @Field(type = FieldType.Text, analyzer = "standard") String description,

        @Field(type = FieldType.Keyword) String propertyType,

        @Field(type = FieldType.Integer) Integer maxGuests,

        @Field(type = FieldType.Integer) Integer bedrooms,

        @Field(type = FieldType.Integer) Integer bathrooms,

        @Field(type = FieldType.Double) BigDecimal basePricePerNight,

        @Field(type = FieldType.Keyword) String status,

        @Field(type = FieldType.Boolean) Boolean isInstantBook,

        @Field(type = FieldType.Keyword) String city,

        @Field(type = FieldType.Keyword) String country,

        @Field(type = FieldType.Double) Double lat,

        @Field(type = FieldType.Double) Double lng,

        @Field(type = FieldType.Keyword) String hostName
) {}