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
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            return "redirect:/login";
        }
        if ("MANAGER".equals(loggedUser.getRole())) {
            return "redirect:/manager/home";
        }
        return "redirect:/patient/home";
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
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Optional<User> found = userDAO.findByUsernameAndPassword(username, password);
        if (found.isPresent()) {
            session.setAttribute("loggedUser", found.get());
            if ("MANAGER".equals(found.get().getRole())) {
                return "redirect:/manager/home";
            }
            return "redirect:/patient/home";
        }
        redirectAttributes.addFlashAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
