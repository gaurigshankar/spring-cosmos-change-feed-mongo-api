package com.gauri.cosmosdemo.db.lease;

import com.gauri.cosmosdemo.config.LeaseConfig;
import com.gauri.cosmosdemo.db.changefeed.EmployeeChangeFeedListener;
import com.gauri.cosmosdemo.db.core.CosmosDAL;
import com.gauri.cosmosdemo.db.models.EmployeeLease;
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

import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class EmployeeLeaseLogic implements
        ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    LeaseConfig leaseConfig;

    @Autowired
    EmployeeChangeFeedListener employeeChangeFeedListener;

    @Autowired
    CosmosDAL cosmosDAL;

    @Autowired
    ReactiveMongoTemplate reactiveMongoTemplate;


    private static Disposable disposable = null;
    private static Integer MAX_TIME_FOR_HOST_IN_SECONDS = 180;
    private static long lastHealthCheck = System.currentTimeMillis();
    // lastHealthCheckValueFromDB will be set in event listener of establishAndSubscribeToChangeStream and used in passiveWorker
    private static long lastHealthCheckValueFromDB = 0;

    Random random = new Random();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        initiate();
    }

    enum LeaseOwnershipState {
        OwnerExists, OwnerDoesntExists, OwnershipExpired
    }

    /*
    * This method is used only by Active Worker and not by Passive worker.
    *
    * */
    private void leaseHealthCheck(BsonDocument resumeToken) {

        // This method will help ActiveWorker to continue with the lease
        // It will update the healthcheck timestamp after reaching close to expiry to
        // avoid multiple roundtrips to lease collection.
        long currentTimeInMs = System.currentTimeMillis();
        long timediff = (currentTimeInMs - lastHealthCheck);
        // System.out.println(timediff + "," + currentTimeInMs + "," + lastHealthCheck);

        // Check if the time gap is high enough to update the healthcheck
        if (timediff > (leaseConfig.getHealthCheckTimeIntervalInSec() * 1000)) {
            lastHealthCheck = currentTimeInMs;

            String runtimeHostName = System.getProperty("hostName");
            String hostName = runtimeHostName != null ? runtimeHostName : leaseConfig.getLeaseHostName();

            EmployeeLease employeeLease = cosmosDAL.findAndUpdateLeasedEmployee(
                    leaseConfig.getLeaseOwnerName(), hostName, lastHealthCheck, resumeToken);

            if (employeeLease == null) {
                // if the lease got expired and passive worker took it.
                System.out.println("Lease expired for ");
                passiveWorker();
            }

        }

    }

    private void activeWorker(EmployeeLease employeeLease) {
        employeeChangeFeedListener.establishAndSubscribeToChangeStream(employeeLease.getResumeToken());

        long activeWorkerWhileLoopTimer = System.currentTimeMillis();
        while (true) {
            long whileLoopInterval = activeWorkerWhileLoopTimer - System.currentTimeMillis();
            activeWorkerWhileLoopTimer = System.currentTimeMillis();
            System.out.println("While of activeWorker after "+whileLoopInterval);

            TimerTask repeatedTask = new TimerTask() {
                public void run() {
                    leaseHealthCheck(ResumeToken.employeeResumeToken);
                }
            };

            try {

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                long delay  = 1000L;
                long period = 1000L;
                executor.scheduleAtFixedRate(repeatedTask, delay, period, TimeUnit.MILLISECONDS);
                Thread.sleep(leaseConfig.getHealthCheckTimeIntervalInSec() * 1000);
                executor.shutdown();
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }

        }

    }

//    private void passiveWorkerOld() {
//
//        MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = cosmosDAL.getEmployeLeaseCursor();
//
//        long lastHealthCheckDB = 0;
//
//        while(true) {
//
//            ChangeStreamDocument<Document> csDoc = cursor.tryNext();
//            if (csDoc != null) {
//
//                // seek the last health check time
//                lastHealthCheckDB = Long
//                        .parseLong(csDoc.getFullDocument().get("ownerHealthCheckRefreshTime").toString());
//                ResumeToken.employeeResumeToken = Utils.toBson((Document) csDoc.getFullDocument().get("resumeToken"));
//
//                System.out.println("Got a CF document for lease collection. So retrieving lastHealthCheckDB from DB "+lastHealthCheckDB+
//                        " with refresh token "+csDoc.getFullDocument().get("resumeToken"));
//            }
//
//            if(checkLeaseExpiry(lastHealthCheckDB) == LeaseOwnershipState.OwnershipExpired) {
//                cursor.close();
//                return;
//            }
//
//
//            try {
//                Thread.sleep((leaseConfig.getHealthCheckTimeIntervalInSec() + random.nextInt(900)) * 1000);
//            } catch (InterruptedException interruptedException) {
//                interruptedException.printStackTrace();
//            }
//        }
//    }

    private void passiveWorker() {

        establishAndSubscribeToChangeStream();

        while (true) {

            TimerTask repeatedTask = new TimerTask() {
                public void run() {
                    // lastHealthCheckValueFromDB will be set in event listener of establishAndSubscribeToChangeStream
                    if(checkLeaseExpiry(lastHealthCheckValueFromDB) == LeaseOwnershipState.OwnershipExpired) {
                        ResumeToken.terminatePassiveWorker = Boolean.TRUE;
                        return;
                    }
                }
            };

            if (ResumeToken.terminatePassiveWorker) {
                ResumeToken.terminatePassiveWorker = Boolean.FALSE;
                if(!disposable.isDisposed()) {
                    disposable.dispose();
                }
                System.out.println("Got Signal to terminate Passive Worker !!! "+disposable.isDisposed());
                return;
            }

            try {

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                int randomIntDelay = random.nextInt(900);
                long delay  = 1000L + randomIntDelay;
                long period = 1000L;
                executor.scheduleAtFixedRate(repeatedTask, delay, period, TimeUnit.MILLISECONDS);
                Thread.sleep((leaseConfig.getHealthCheckTimeIntervalInSec() + randomIntDelay)* 1000);
                executor.shutdown();
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    private LeaseOwnershipState checkOwnership() {
        String leaseOwnerName = leaseConfig.getLeaseOwnerName() == null ? "consumer1" : leaseConfig.getLeaseOwnerName();
        EmployeeLease employeeLease = cosmosDAL.findLeasedEmployee(leaseOwnerName);
        if(employeeLease != null
                && !"".equalsIgnoreCase(employeeLease.getId())
                && !"".equalsIgnoreCase(employeeLease.getHostName())) {
            long lastHealthCheckDB = employeeLease.getOwnerHealthCheckRefreshTime();
            ResumeToken.employeeResumeToken = employeeLease.getResumeToken();
            System.out.println("calling checkLeaseExpiry from checkOwnership()");
            return checkLeaseExpiry(lastHealthCheckDB);
        }

        return LeaseOwnershipState.OwnerDoesntExists;
    }

    private void createLeaseDocument() {
        String runtimeHostName = System.getProperty("hostName");
        String hostName = runtimeHostName != null ? runtimeHostName : leaseConfig.getLeaseHostName();
        EmployeeLease employeeLease = new EmployeeLease();
        employeeLease.setId(leaseConfig.getLeaseOwnerName());
        employeeLease.setHostName(hostName);
        employeeLease.setOwnerHealthCheckRefreshTime(System.currentTimeMillis());
        employeeLease.setResumeToken(ResumeToken.employeeResumeToken);
        cosmosDAL.persistLeaseEmployee(employeeLease);
    }

    private EmployeeLease acquireLease() {
        String runtimeHostName = System.getProperty("hostName");
        String hostName = runtimeHostName != null ? runtimeHostName : leaseConfig.getLeaseHostName();
        EmployeeLease employeeLease =
                cosmosDAL.findAndUpdateLeasedEmployee(leaseConfig.getLeaseOwnerName(), hostName);

        return employeeLease;
    }

    private LeaseOwnershipState checkLeaseExpiry(long lastHealthCheckDB) {
        long systemTime = System.currentTimeMillis();
        long healthCheckGap = systemTime - lastHealthCheckDB;
        System.out.println(lastHealthCheckDB + "," + systemTime + "," + healthCheckGap + " checkexpiry");

        if (healthCheckGap > ((MAX_TIME_FOR_HOST_IN_SECONDS + leaseConfig.getHealthCheckTimeIntervalInSec()) * 1000)) {
            System.out.println("lease expired");
            return LeaseOwnershipState.OwnershipExpired;
        }

        // If lease exists and is not expired then become passive worker.
        return LeaseOwnershipState.OwnerExists;
    }

    private void initiate() {
        EmployeeLease employeeLease;
        employeeLease = cosmosDAL.findLeasedEmployee(leaseConfig.getLeaseOwnerName());
        lastHealthCheckValueFromDB = employeeLease.getOwnerHealthCheckRefreshTime();
        while(true) {
            System.out.println("While of initiate");
            switch (checkOwnership()) {
                case OwnerExists:
                    // as lease exists hence this worker will become the passive worker
                    passiveWorker();
                    break;
                case OwnerDoesntExists:
                    createLeaseDocument();
                case OwnershipExpired:
                    employeeLease = acquireLease();
                    activeWorker(employeeLease);
                    break;
                default:
                    break;
            }
        }

    }


    public void establishAndSubscribeToChangeStream() {
        String leaseEmployeeCollectionName = leaseConfig.getLeaseCollectionName();
        try {
            Flux<ChangeStreamEvent<EmployeeLease>> flux = reactiveMongoTemplate
                    .changeStream(leaseEmployeeCollectionName, ChangeStreamOptions
                            .builder()
                            .filter(
                                    Aggregation.newAggregation(
                                            Aggregation.match(Criteria.where("operationType").in("insert", "update", "replace")),
                                            Aggregation.project("_id", "fullDocument", "ns", "documentKey")
                                    )
                            ).build(), EmployeeLease.class);


            disposable = flux.subscribe(membershipChangeStreamEvent -> {
                System.out.println("Got Employee Lease Change Event "+membershipChangeStreamEvent.getRaw());
                EmployeeLease leaseEmployee = membershipChangeStreamEvent.getBody();

                leaseEmployee.getResumeToken();

                lastHealthCheckValueFromDB = leaseEmployee.getOwnerHealthCheckRefreshTime();

                ResumeToken.employeeResumeToken = leaseEmployee.getResumeToken();

            });

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
