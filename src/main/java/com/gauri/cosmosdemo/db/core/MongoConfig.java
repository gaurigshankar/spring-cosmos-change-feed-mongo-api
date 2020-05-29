package com.gauri.cosmosdemo.db.core;

import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMongoRepositories(basePackages = "com.gauri")
public class MongoConfig extends AbstractMongoClientConfiguration {

    private final List<Converter<?, ?>> converters = new ArrayList<>();


    @Value("${spring.data.mongodb.database}")
    private String dbName;

    @Value("${spring.data.mongodb.uri}")
    private String dbUri;

    @Override
    protected String getDatabaseName() {
        return dbName;
    }

    @Override
    public com.mongodb.client.MongoClient mongoClient() {
        return MongoClients.create(dbUri);
    }

    @Override
    public MongoCustomConversions customConversions() {
        converters.add(new EmployeeLeaseConvertor());
        return new MongoCustomConversions(converters);
    }

}
