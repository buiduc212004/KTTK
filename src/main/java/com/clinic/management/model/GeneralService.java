package com.clinic.management.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GeneralService extends Service {
    private boolean isActive = true;

    public GeneralService(int id, String name, String type, String des, double price,
                          Clinic clinic, boolean isActive) {
        super(id, name, type, des, price, clinic);
        this.isActive = isActive;
    }
}
