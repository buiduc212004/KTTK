package com.clinic.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Người dùng đăng nhập hệ thống (quan lý hoặc bệnh nhân đăng ký). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private int id;
    private String username;
    private String password;
    private String fullName;
    /** {@code MANAGER} | {@code PATIENT} */
    private String role;
}
