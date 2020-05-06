
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
2) Create New DB / Use Existing DB
3) Create two Collections named Employee and DuplicateEmployee
4) Modify `application.properties` to update MONGO URI and DB Name


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
This Java class subscribes to the ChangeStream of Employee Collection and acts to process the change 
(ie) inserting / updating same document in DuplicateEmployee Collection.

