package com.clinic.management.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TestService extends Service {
    private String preparationInstructions;
    private String method;

    public TestService(int id, String name, String type, String des, double price, Clinic clinic,
                       String preparationInstructions, String method) {
        super(id, name, type, des, price, clinic);
        this.preparationInstructions = preparationInstructions;
        this.method = method;
    }
}
