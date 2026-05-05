package com.clinic.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor implements Serializable {
    private int id;
    private String name;
    private String major;
    private String phoneNumber;
    private String email;
    private Specialization specialization;
}
