package com.gauri.cosmosdemo.db.core;

import com.gauri.cosmosdemo.config.LeaseConfig;
import com.gauri.cosmosdemo.db.models.DuplicateEmployee;
import com.gauri.cosmosdemo.db.models.Employee;
import com.gauri.cosmosdemo.db.models.EmployeeLease;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

@Repository
public class CosmosDALImpl implements CosmosDAL{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    LeaseConfig leaseConfig;

    @Override
    public Collection<Employee> persistEmployee(List<Employee> models) {
        Collection<Employee> currentInsertedDocs = mongoTemplate.insertAll(models);
        return currentInsertedDocs;
    }

    @Override
    public UpdateResult persistDuplicateEmployee(DuplicateEmployee duplicateEmployee) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(duplicateEmployee.get_id()));
        Update update = new Update();
        update.set("_id", duplicateEmployee.get_id());
        update.set("department", duplicateEmployee.getDepartment());
        update.set("designation", duplicateEmployee.getDesignation());
        update.set("name", duplicateEmployee.getName());
        UpdateResult updateResult = mongoTemplate.upsert(query, update, DuplicateEmployee.class);
        return updateResult;
    }

    @Override
    public List<Employee> getAllEmployees() {
        return mongoTemplate.findAll(Employee.class);
    }

    @Override
    public EmployeeLease findLeasedEmployee(String id) {
        return mongoTemplate.findById(id,
                EmployeeLease.class,
                leaseConfig.getLeaseCollectionName());
    }

    //@Override
    @Deprecated
    public EmployeeLease findAndUpdateLeasedEmployeeOld(String id, String leaseHostName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));

        EmployeeLease employeeLease = mongoTemplate.findOne(query, EmployeeLease.class);
        if(null != employeeLease) {
            employeeLease.setOwnerHealthCheckRefreshTime(System.currentTimeMillis());
            employeeLease.setHostName(leaseHostName);
            mongoTemplate.save(employeeLease);
        }
        return employeeLease;
    }

    @Override
    public EmployeeLease findAndUpdateLeasedEmployee(String id, String leaseHostName) {
        Update update = new Update();
//        update.addToSet("ownerHealthCheckRefreshTime",System.currentTimeMillis());
//        update.addToSet("hostName",leaseHostName);
        update.set("ownerHealthCheckRefreshTime",System.currentTimeMillis());
        update.set("hostName",leaseHostName);

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));

        FindAndModifyOptions opts = new FindAndModifyOptions()
                .returnNew(true);

        EmployeeLease modifiedEmployeeLease = mongoTemplate.findAndModify(query, update, opts, EmployeeLease.class);

        return modifiedEmployeeLease;
    }

    @Override
    public EmployeeLease persistLeaseEmployee(EmployeeLease employeeLease){
        return mongoTemplate.insert(employeeLease);
    }

    //@Override
    public EmployeeLease findAndUpdateLeasedEmployeeOld(String leaseOwnerName, String leaseHostName,
                                                     long lastHealthCheck, BsonDocument resumeToken ) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(leaseOwnerName));

        EmployeeLease employeeLease = mongoTemplate.findOne(query, EmployeeLease.class);

        FindAndModifyOptions opts = new FindAndModifyOptions()
                .returnNew(true);

        if(null != employeeLease) {
            employeeLease.setOwnerHealthCheckRefreshTime(lastHealthCheck);
            employeeLease.setResumeToken(resumeToken);
            employeeLease.setHostName(leaseHostName);
            mongoTemplate.save(employeeLease);
        }

        return employeeLease;
    }

    @Override
    public EmployeeLease findAndUpdateLeasedEmployee(String leaseOwnerName, String leaseHostName,
                                                     long lastHealthCheck, BsonDocument resumeToken ) {
        Update update = new Update();
        update.set("ownerHealthCheckRefreshTime",lastHealthCheck);
        update.set("resumeToken",resumeToken);
        update.set("hostName", leaseHostName);

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(leaseOwnerName));


        FindAndModifyOptions opts = new FindAndModifyOptions()
                .returnNew(true);

        EmployeeLease modifiedEmployeeLease = mongoTemplate.findAndModify(query, update, opts, EmployeeLease.class);

        return modifiedEmployeeLease;
    }

    @Override
    public MongoChangeStreamCursor<ChangeStreamDocument<Document>> getEmployeLeaseCursor() {
        Bson match = Aggregates.match(Filters.in("operationType", asList("update", "replace", "insert")));

        // Pick the field you are most interested in
        Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
        List<Bson> pipeline = Arrays.asList(match, project);
        MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor =
                mongoTemplate.getDb()
                        .getCollection(leaseConfig.getLeaseCollectionName())
                        .watch(pipeline)
                        .fullDocument(FullDocument.UPDATE_LOOKUP)
                        .cursor();

        return cursor;
    }

}
