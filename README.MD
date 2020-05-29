## Cosmos Change Feed Demo App

Simple Demo For Cosmos [Change Feed](https://docs.microsoft.com/en-us/azure/cosmos-db/change-feed) while using 
[MongoDB API](https://docs.microsoft.com/en-us/azure/cosmos-db/mongodb-introduction) 

MongoDB natively has [ChangeStream](https://docs.mongodb.com/manual/changeStreams/) that is synonomous with Change Feed Of CosmosDB.

When using CosmosDB with MongoDB API,  We can just use MongoDB Change Stream directly. We just have to be aware,
 the entire MongoDB ChangeStream functionalities may not be supported by CosmosDB. Check 
 [Limitations](https://docs.microsoft.com/en-us/azure/cosmos-db/mongodb-change-streams#current-limitations)  

### Demo APP

In the Demo App, there are two Collections Employee & DuplicateEmployee. The App listens for 
ChangeFeed/ChangeStream messages on Employee Collection and inserts/updates the same Document into DuplicateEmployee Collection.

### PreReqs
1) Access To Azure CosmosDB
2) Ensure MongoDB APi 3.6 version is used / enabled in Azure Portal
3) Ensure MongoDB Aggregation Pipeline is enabled in Azure Portal
4) Create New DB / Use Existing DB
5) Create three Collections named Employee,  DuplicateEmployee and EmployeeLease
6) Rename [src/main/resources/sample_application.properties](./src/main/resources/sample_application.properties) to `application.properties`
7) Modify `src/main/resources/application.properties` to update MONGO URI and DB Name specific to your azure account

### Reactive Java
Reactive programming is a programming paradigm that deals with asynchronous data streams (sequences of events) 
and the specific propagation of change, which means it implements modifications to the execution 
environment (context) in a certain order. [More Details](https://www.scnsoft.com/blog/java-reactive-programming)
Being Spring boot project uses [Spring Reactive](https://spring.io/reactive). This is used via below dependency

```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb-reactive</artifactId>
    <version>2.2.6.RELEASE</version>
</dependency>
```

### Execute
1) This is a typical Spring Boot APP. Run `com.gauri.cosmosdemo.Main` class.
2) Using Azure portal add a new Document to Employee Collection as per below structure. This App would have listened 
to changes in Employee Collection and inserted/updates a document in DuplicateEmployee Collection.
```
{
    "_id": 4,
    "name": "Gauri",
    "department": "IT",
    "designation": "Engg"
}
```

### EmployeeChangeFeedListener
[EmployeeChangeFeedListener.java](./src/main/java/com/gauri/cosmosdemo/db/changefeed/EmployeeChangeFeedListener.java) 
sets up and subscribes to the ChangeStream of Employee Collection and acts to process the change 
(ie) inserting / updating same document in DuplicateEmployee Collection.

### Rest Endpoints For Testing
Few Rest Endpoints are powered by Demo App via [TestServiceController.java](./src/main/java/com/gauri/cosmosdemo/service/TestServiceController.java)
Once Server is running, base path of the endpoints are http://localhost:8080/cosmosdemo/test

| Path | Type | Functionality |
| - | - | - |
| http://localhost:8080/cosmosdemo/test/allEmployees | GET | Gets all Documents from Employee Collection |
| http://localhost:8080/cosmosdemo/test/statusChangeFeedEmployee | GET | Gets the status of Employee Change Feed Subscription that informs, if the subscription is active |
| http://localhost:8080/cosmosdemo/test/terminateChangeFeedEmployee | GET | Terminates the Employee Change Feed Subscription |
| http://localhost:8080/cosmosdemo/test/insertEmployee |  POST  <pre lang="json">{<br> "_id": 4, <br> "name": "Gauri", <br> "department": "IT", <br>  "designation": "Engg" <br> } </pre>  | Insert a document into Employee Collection |


### JVM variables
| variable | comments |
| - | - |
| -Dserver.port=8085 | to override tomcat port |
| -DhostName=host411 | to override host name |

### High Availability Settings

1) EmployeeLease collection used as a single document collection.
2) Active and Passive Workers for Change Feed use this Collection to switch role
3) Active Worker is the host that will actively listen to the Change feed and process the change. 
4) Active Worker periodically updates the EmployeeLease collection to inform passive workers that the 
    active worker is still healthy and actively processing the change by updating timestamp.
5) Active worker also periodically updates EmployeeLease Collection with RefreshToken until which it has processed the 
Change feed for source collection (ie) Employee    
6) Passive worker Listens to Change Feed of EmployeeLease Collection and checks the timestamp updated by active worker.
   If the last updated timestamp by active worker exceeds threshold, one of the passive worker will try to become active worker.

`Sample EmployeeLease Document`
```
{
        "ownerHealthCheckRefreshTime": NumberLong("1590714388686"),
        "hostName": "host311",
        "_class": "com.gauri.cosmosdemo.db.models.EmployeeLease",
        "resumeToken": {
            "_data": BinData(0, "W3sidG9rZW4iOiJcIjg1XCIiLCJyYW5nZSI6eyJtaW4iOiIiLCJtYXgiOiJGRiJ9fV0=")
        }
    }
```