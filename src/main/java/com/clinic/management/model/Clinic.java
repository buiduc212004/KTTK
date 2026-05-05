package com.clinic.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Clinic implements Serializable {
    private int id;
    private String name;
    private String address;
    private String des;
}
