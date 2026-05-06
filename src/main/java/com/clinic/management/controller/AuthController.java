package com.clinic.management.controller;

import com.clinic.management.model.User;
import com.clinic.management.service.UserAuthService;
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

    private final UserAuthService userAuthService;

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
        Optional<User> found = userAuthService.authenticate(form);
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
        Optional<String> error = userAuthService.registerPatient(form);
        if (error.isPresent()) {
            redirectAttributes.addFlashAttribute("error", error.get());
            return "redirect:/register";
        }
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
