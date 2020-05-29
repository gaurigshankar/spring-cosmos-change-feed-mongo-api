package com.gauri.cosmosdemo.db.core;

import com.gauri.cosmosdemo.db.models.DuplicateEmployee;
import com.gauri.cosmosdemo.db.models.Employee;
import com.gauri.cosmosdemo.db.models.EmployeeLease;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;

import java.util.Collection;
import java.util.List;

public interface CosmosDAL {
    Collection<Employee> persistEmployee(List<Employee> models);

    UpdateResult persistDuplicateEmployee(DuplicateEmployee duplicateEmployee);

    List<Employee> getAllEmployees();

    EmployeeLease findLeasedEmployee(String id);

    EmployeeLease findAndUpdateLeasedEmployee(String id, String leaseHostName);

    EmployeeLease persistLeaseEmployee(EmployeeLease employeeLease);

    EmployeeLease findAndUpdateLeasedEmployee(String id, String leaseHostName,
                                              long lastHealthCheck, BsonDocument resumeToken );

    MongoChangeStreamCursor<ChangeStreamDocument<Document>> getEmployeLeaseCursor();
}
