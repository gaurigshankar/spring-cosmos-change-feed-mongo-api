package com.gauri.cosmosdemo.db.changefeed;

import com.gauri.cosmosdemo.db.core.CosmosDAL;
import com.gauri.cosmosdemo.db.lease.ResumeToken;
import com.gauri.cosmosdemo.db.models.DuplicateEmployee;
import com.gauri.cosmosdemo.db.models.Employee;
import org.apache.commons.beanutils.BeanUtils;
import org.bson.BsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;

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
        BsonDocument resumeToken = null;
        //establishAndSubscribeToChangeStream(resumeToken);
    }

    private static Disposable disposable = null;

    public Disposable establishAndSubscribeToChangeStream(BsonDocument resumeToken) {
        try {

            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("operationType").in("insert", "update", "replace")),
                    Aggregation.project("_id", "fullDocument", "ns", "documentKey")
            );

            ChangeStreamOptions.ChangeStreamOptionsBuilder cb = ChangeStreamOptions.builder();
            if(resumeToken != null) {
                cb = cb.resumeAfter(resumeToken);
            }
            ChangeStreamOptions changeStreamOptions = cb.filter(aggregation).build();

            Flux<ChangeStreamEvent<Employee>> flux = reactiveMongoTemplate
                    .changeStream(employeeCollectionName, changeStreamOptions, Employee.class);


            disposable = flux.subscribe(membershipChangeStreamEvent -> {
                System.out.println(membershipChangeStreamEvent.getRaw().toString());
                System.out.println(membershipChangeStreamEvent.getResumeToken());
                System.out.println(membershipChangeStreamEvent.getBody());
                System.out.println(membershipChangeStreamEvent.getBody().get_id());
                //resumetoken
                ResumeToken.employeeResumeToken = membershipChangeStreamEvent.getResumeToken().asDocument();
                // write a logic here to update the lease document with the latest resume token, so that we done lose if the health check is done later and active worker dies before it
                Employee modifiedEmployee = membershipChangeStreamEvent.getBody();

                DuplicateEmployee duplicateEmployee = new DuplicateEmployee();

                try {
                    BeanUtils.copyProperties(duplicateEmployee, modifiedEmployee);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                cosmosDAL.persistDuplicateEmployee(duplicateEmployee);

            });
            System.out.println("is disposable null ? "+(null == disposable));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return disposable;
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

