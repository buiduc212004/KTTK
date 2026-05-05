package com.clinic.management.controller;

import com.clinic.management.dao.UserDAO;
import com.clinic.management.model.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserDAO userDAO;

    @GetMapping("/")
    public String index(HttpSession session) {
        User u = (User) session.getAttribute("loggedUser");
        if (u == null) {
            return "redirect:/login";
        }
        if ("MANAGER".equals(u.getRole())) {
            return "redirect:/manager/home";
        }
        return "redirect:/account";
    }

    @GetMapping("/login")
    public String showLogin(HttpSession session, Model model) {
        if (session.getAttribute("loggedUser") != null) {
            return "redirect:/";
        }
        model.addAttribute("user", new User());
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@ModelAttribute("user") User form,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Optional<User> found = userDAO.findByUsernameAndPassword(
                form.getUsername() != null ? form.getUsername().trim() : "",
                form.getPassword() != null ? form.getPassword() : "");
        if (found.isPresent()) {
            session.setAttribute("loggedUser", found.get());
            return "redirect:/";
        }
        redirectAttributes.addFlashAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String showRegister(HttpSession session, Model model) {
        if (session.getAttribute("loggedUser") != null) {
            return "redirect:/";
        }
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@ModelAttribute("user") User form,
                                  RedirectAttributes redirectAttributes) {
        String u = form.getUsername() != null ? form.getUsername().trim() : "";
        String pw = form.getPassword() != null ? form.getPassword() : "";

        if (u.isBlank() || pw.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập đủ tên đăng nhập và mật khẩu.");
            return "redirect:/register";
        }
        if (!pw.equals(form.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "redirect:/register";
        }
        if (userDAO.existsByUsername(u)) {
            redirectAttributes.addFlashAttribute("error", "Tên đăng nhập đã được sử dụng.");
            return "redirect:/register";
        }

        User nu = new User();
        nu.setUsername(u);
        nu.setPassword(pw);
        nu.setFullName(form.getFullName() != null ? form.getFullName().trim() : "");
        nu.setRole("PATIENT");
        userDAO.insert(nu);

        redirectAttributes.addFlashAttribute("success", "Đăng ký thành công. Vui lòng đăng nhập.");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/account")
    public String account(HttpSession session, Model model) {
        User u = (User) session.getAttribute("loggedUser");
        if (u == null) {
            return "redirect:/login";
        }
        if ("MANAGER".equals(u.getRole())) {
            return "redirect:/manager/home";
        }
        model.addAttribute("loggedUser", u);
        return "auth/account";
    }
}
