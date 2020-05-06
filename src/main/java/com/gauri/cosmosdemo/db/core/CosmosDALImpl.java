package com.gauri.cosmosdemo.db.core;

import com.gauri.cosmosdemo.db.models.DuplicateEmployee;
import com.gauri.cosmosdemo.db.models.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class CosmosDALImpl implements CosmosDAL{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Collection<Employee> persistEmployee(List<Employee> models) {
        Collection<Employee> currentInsertedDocs = mongoTemplate.insertAll(models);
        return currentInsertedDocs;
    }

    @Override
    public Collection<DuplicateEmployee> persistDuplicateEmployee(List<DuplicateEmployee> models) {
        Collection<DuplicateEmployee> currentInsertedDocs = mongoTemplate.insertAll(models);
        return currentInsertedDocs;
    }

    @Override
    public List<Employee> getAllEmployees() {
        return mongoTemplate.findAll(Employee.class);
    }
}
