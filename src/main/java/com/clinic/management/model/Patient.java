package com.clinic.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient implements Serializable {
    private int id;
    private String name;
    private String gender;
    private LocalDate dateOfBirth;
    private String telNumber;
    private String email;
    private String allergies;
}
