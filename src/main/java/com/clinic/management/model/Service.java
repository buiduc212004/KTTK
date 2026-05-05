package com.clinic.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Model domain: dịch vụ cha (JOINED subclass general / test). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service implements Serializable {
    private int id;
    private String name;
    private String type;
    private String des;
    private double price;
    private Clinic clinic;
}
