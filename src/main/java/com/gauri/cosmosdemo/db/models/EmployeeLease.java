package com.gauri.cosmosdemo.db.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.BsonDocument;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "EmployeeLease")
public class EmployeeLease {
    @Id
    public String id;
    public BsonDocument resumeToken; // this gets set in leaseHealthCheck
    //public BsonValue resumeToken;
    //public Binary resumeToken;
    public long ownerHealthCheckRefreshTime;//this gets set in acquireLease , leaseHealthCheck
    public String hostName; // this gets set in acquireLease
}
