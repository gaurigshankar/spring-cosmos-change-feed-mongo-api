package com.gauri.cosmosdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:leaseConfig.properties")
public class LeaseConfig {
    @Value("${leaseCollectionName}")
    private String leaseCollectionName;
    @Value("${healthCheckTimeIntervalInSec}")
    private Integer healthCheckTimeIntervalInSec;
    @Value("${leaseOwnerName}")
    private String leaseOwnerName;
    @Value("${leaseHostName}")
    private String leaseHostName;
    @Value("${initialLeaseHostName}")
    private String initialLeaseHostName;
    @Value("${withLease}")
    private Boolean withLease;


    public String getLeaseCollectionName() {
        return leaseCollectionName;
    }

    public void setLeaseCollectionName(String leaseCollectionName) {
        this.leaseCollectionName = leaseCollectionName;
    }

    public Integer getHealthCheckTimeIntervalInSec() {
        return healthCheckTimeIntervalInSec;
    }

    public void setHealthCheckTimeIntervalInSec(Integer healthCheckTimeIntervalInSec) {
        this.healthCheckTimeIntervalInSec = healthCheckTimeIntervalInSec;
    }

    public String getLeaseOwnerName() {
        return leaseOwnerName;
    }

    public void setLeaseOwnerName(String leaseOwnerName) {
        this.leaseOwnerName = leaseOwnerName;
    }

    public String getLeaseHostName() {
        return leaseHostName;
    }

    public void setLeaseHostName(String leaseHostName) {
        this.leaseHostName = leaseHostName;
    }

    public Boolean getWithLease() {
        return withLease;
    }

    public void setWithLease(Boolean withLease) {
        this.withLease = withLease;
    }

    public String getInitialLeaseHostName() {
        return initialLeaseHostName;
    }

    public void setInitialLeaseHostName(String initialLeaseHostName) {
        this.initialLeaseHostName = initialLeaseHostName;
    }
}

