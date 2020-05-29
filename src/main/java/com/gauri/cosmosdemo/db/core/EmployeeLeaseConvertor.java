package com.gauri.cosmosdemo.db.core;

import com.gauri.cosmosdemo.db.models.EmployeeLease;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EmployeeLeaseConvertor implements Converter<Document, EmployeeLease> {

    @Override
    public EmployeeLease convert(Document document) {
        EmployeeLease employeeLease = new EmployeeLease();
        employeeLease.setId(document.getString("_id"));
        if (null != document) {

            Set keyset = document.keySet();
            if(keyset.contains("hostName")) {
                employeeLease.setHostName(document.getString("hostName"));
            }
            if (keyset.contains("ownerHealthCheckRefreshTime")) {
                employeeLease.setOwnerHealthCheckRefreshTime(document.getLong("ownerHealthCheckRefreshTime"));
            }
           if(keyset.contains("resumeToken")) {
               Document bDocument = (Document) document.get("resumeToken");
               if(bDocument !=null) {
                   System.out.println(bDocument.toJson());
                   System.out.println(bDocument.get("data"));
                   String json = bDocument.toJson();
                   BsonValue bv = BsonDocument.parse(json);

                   employeeLease.setResumeToken(bv.asDocument());
               }
           }

        }


        return employeeLease;
    }
}
