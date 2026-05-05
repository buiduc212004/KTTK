package com.clinic.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRegister implements Serializable {
    private int id;
    private LocalDate dateScheduleRegister;
    private LocalTime timeScheduleRegister;
    private Clinic clinic;
    private Doctor doctor;
}
