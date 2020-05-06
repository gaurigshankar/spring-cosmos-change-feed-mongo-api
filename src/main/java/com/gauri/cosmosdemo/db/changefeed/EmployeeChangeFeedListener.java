package com.gauri.cosmosdemo.db.changefeed;

import com.gauri.cosmosdemo.db.core.CosmosDAL;
import com.gauri.cosmosdemo.db.models.DuplicateEmployee;
import com.gauri.cosmosdemo.db.models.Employee;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Component
public class EmployeeChangeFeedListener implements
        ApplicationListener<ContextRefreshedEvent> {

    private static final String employeeCollectionName = "Employee";


    @Autowired
    ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    CosmosDAL cosmosDAL;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        establishAndSubscribeToChangeStream();
    }

    private static Disposable disposable = null;

    public void establishAndSubscribeToChangeStream() {
        try {
            Flux<ChangeStreamEvent<Employee>> flux = reactiveMongoTemplate
                    .changeStream(employeeCollectionName, ChangeStreamOptions.
                            builder().filter(
                            Aggregation.newAggregation(
                                    Aggregation.match(Criteria.where("operationType").in("insert", "update", "replace")),
                                    Aggregation.project("_id", "fullDocument", "ns", "documentKey")
                            )
                    ).build(), Employee.class);


            disposable = flux.subscribe(membershipChangeStreamEvent -> {
                System.out.println(membershipChangeStreamEvent.getRaw().toString());
                System.out.println(membershipChangeStreamEvent.getBody());
                System.out.println(membershipChangeStreamEvent.getBody().get_id());

                Employee modifiedEmployee = membershipChangeStreamEvent.getBody();

                DuplicateEmployee duplicateEmployee = new DuplicateEmployee();

                try {
                    BeanUtils.copyProperties(duplicateEmployee, modifiedEmployee);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                List<DuplicateEmployee> duplicateEmployees = new ArrayList<>();
                duplicateEmployees.add(duplicateEmployee);
                cosmosDAL.persistDuplicateEmployee(duplicateEmployees);

            });
            System.out.println("is disposable null ? "+(null == disposable));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    public void cancelSubscription() {

        if(null != disposable && !disposable.isDisposed()) {
            disposable.dispose();
            System.out.println("Membership changes Subscription listener cancelled. Actual Status "+disposable.isDisposed());
        }

    }

    public String subscriptionStatus() {
        if(null != disposable ) {
            return disposable.isDisposed() + " -- "+disposable.toString();
        }
        return "Not Subscribed";
    }

}

