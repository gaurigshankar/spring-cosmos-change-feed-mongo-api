package com.gauri.cosmosdemo.db.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter @Setter @NoArgsConstructor @Document(collection = "DuplicateEmployee")
public class DuplicateEmployee {
    @Id
    private Integer _id;
    private String name;
    private String department;
    private String designation;

}
