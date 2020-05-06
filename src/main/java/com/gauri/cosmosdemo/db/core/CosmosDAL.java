package com.gauri.cosmosdemo.db.core;

import com.gauri.cosmosdemo.db.models.DuplicateEmployee;
import com.gauri.cosmosdemo.db.models.Employee;

import java.util.Collection;
import java.util.List;

public interface CosmosDAL {
    Collection<Employee> persistEmployee(List<Employee> models);
    Collection<DuplicateEmployee> persistDuplicateEmployee(List<DuplicateEmployee> models);

    List<Employee> getAllEmployees();
}
