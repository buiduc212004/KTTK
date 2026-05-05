package com.clinic.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingReceipt implements Serializable {
    private int id;
    private LocalDate bookingDate;
    private double sellOff;
    private double total;
    private String note;
    private Patient patient;
    private User user;
    private ScheduleCustomerBooking scheduleCustomerBooking;
    private Service service;
}
