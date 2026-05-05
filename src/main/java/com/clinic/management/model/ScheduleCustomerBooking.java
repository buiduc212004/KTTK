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
public class ScheduleCustomerBooking implements Serializable {
    private int id;
    private LocalDate dateSchedule;
    private LocalTime timeSchedule;
    private String isState;
    private ServiceRoom serviceRoom;
    private Doctor doctor;
}
