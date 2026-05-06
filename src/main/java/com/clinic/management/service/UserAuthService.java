package com.clinic.management.service;

import com.clinic.management.dao.UserDAO;
import com.clinic.management.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserAuthService {

    private final UserDAO userDAO;

    public Optional<User> authenticate(User form) {
        String username = form.getUsername() != null ? form.getUsername().trim() : "";
        String password = form.getPassword() != null ? form.getPassword() : "";
        return userDAO.findByUsernameAndPassword(username, password);
    }

    public Optional<String> registerPatient(User form) {
        String u = form.getUsername() != null ? form.getUsername().trim() : "";
        String pw = form.getPassword() != null ? form.getPassword() : "";

        if (u.isBlank() || pw.isBlank()) {
            return Optional.of("Vui lòng nhập đủ tên đăng nhập và mật khẩu.");
        }
        if (!pw.equals(form.getConfirmPassword())) {
            return Optional.of("Mật khẩu xác nhận không khớp.");
        }
        if (userDAO.existsByUsername(u)) {
            return Optional.of("Tên đăng nhập đã được sử dụng.");
        }

        User nu = new User();
        nu.setUsername(u);
        nu.setPassword(pw);
        nu.setFullName(form.getFullName() != null ? form.getFullName().trim() : "");
        nu.setRole("PATIENT");
        userDAO.insert(nu);
        return Optional.empty();
    }
}
