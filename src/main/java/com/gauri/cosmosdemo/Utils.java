package com.gauri.cosmosdemo;

import org.bson.BsonDocument;
import org.bson.Document;

public class Utils {

    public static BsonDocument toBson(Document document) {
        if(document !=null) {
            System.out.println(document.toJson());
            System.out.println(document.get("data"));
            String json = document.toJson();
            BsonDocument bsonDocument = BsonDocument.parse(json);

           return  bsonDocument;
        }
        return null;
    }
}
