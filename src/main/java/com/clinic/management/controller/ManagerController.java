package com.clinic.management.controller;

import com.clinic.management.dao.ClinicDAO;
import com.clinic.management.dao.ServiceDAO;
import com.clinic.management.model.Clinic;
import com.clinic.management.model.GeneralService;
import com.clinic.management.model.Service;
import com.clinic.management.model.TestService;
import com.clinic.management.model.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Controller quản lý dịch vụ — chỉ vai trò {@code MANAGER} (sau đăng nhập).
 */
@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private static final int PAGE_SIZE = 10;

    private final ServiceDAO serviceDAO;
    private final ClinicDAO clinicDAO;

    /** @return {@code null} nếu OK; hoặc chuỗi redirect cho chặn MANAGER-only. */
    private String guardManager(HttpSession session) {
        User logged = (User) session.getAttribute("loggedUser");
        if (logged == null) {
            return "redirect:/login";
        }
        if (!"MANAGER".equals(logged.getRole())) {
            return "redirect:/account";
        }
        return null;
    }

    @GetMapping("/home")
    public String managerHome(HttpSession session, Model model) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }
        model.addAttribute("totalServices", serviceDAO.countAllServices());
        model.addAttribute("totalGeneral", serviceDAO.countGeneralServices());
        model.addAttribute("totalTest", serviceDAO.countTestServices());
        model.addAttribute("loggedUser", session.getAttribute("loggedUser"));
        return "manager/home";
    }

    @GetMapping("/services")
    public String getAllServices(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(defaultValue = "0") int page,
                                 HttpSession session,
                                 Model model) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending());
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasType = type != null && !type.isBlank();

        Page<Service> servicePage;
        if (hasKeyword && hasType) {
            servicePage = serviceDAO.findPaged(keyword, type, pageable);
        } else if (hasKeyword) {
            servicePage = serviceDAO.findPaged(keyword, null, pageable);
        } else if (hasType) {
            servicePage = serviceDAO.findPaged(null, type, pageable);
        } else {
            servicePage = serviceDAO.findPaged(null, null, pageable);
        }

        model.addAttribute("servicePage", servicePage);
        model.addAttribute("services", servicePage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type);
        model.addAttribute("currentPage", page);
        model.addAttribute("loggedUser", session.getAttribute("loggedUser"));
        return "manager/services";
    }

    @GetMapping("/services/new")
    public String showAddForm(HttpSession session, Model model) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }
        List<Clinic> clinics = clinicDAO.findAll();
        model.addAttribute("generalService", new GeneralService());
        model.addAttribute("testService", new TestService());
        model.addAttribute("clinics", clinics);
        model.addAttribute("isEdit", false);
        model.addAttribute("loggedUser", session.getAttribute("loggedUser"));
        return "manager/service-form";
    }

    @PostMapping("/services/add")
    public String addService(@RequestParam String serviceKind,
                             @RequestParam String name,
                             @RequestParam String type,
                             @RequestParam(required = false) String des,
                             @RequestParam double price,
                             @RequestParam(required = false, defaultValue = "true") boolean isActive,
                             @RequestParam(required = false) String preparationInstructions,
                             @RequestParam(required = false) String method,
                             @RequestParam(required = false) Integer clinicId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }

        Clinic clinic = null;
        if (clinicId != null) {
            clinic = clinicDAO.findById(clinicId).orElse(null);
        }

        if ("GENERAL".equals(serviceKind)) {
            GeneralService gs = new GeneralService();
            gs.setName(name);
            gs.setType(type);
            gs.setDes(des);
            gs.setPrice(price);
            gs.setActive(isActive);
            gs.setClinic(clinic);
            serviceDAO.add(gs);
        } else {
            TestService ts = new TestService();
            ts.setName(name);
            ts.setType(type);
            ts.setDes(des);
            ts.setPrice(price);
            ts.setPreparationInstructions(preparationInstructions);
            ts.setMethod(method);
            ts.setClinic(clinic);
            serviceDAO.add(ts);
        }

        redirectAttributes.addFlashAttribute("successMsg", "Thêm dịch vụ thành công!");
        return "redirect:/manager/services";
    }

    @GetMapping("/services/edit/{id}")
    public String showEditForm(@PathVariable int id,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }

        Optional<Service> serviceOpt = serviceDAO.getById(id);
        if (serviceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy dịch vụ!");
            return "redirect:/manager/services";
        }

        Service service = serviceOpt.get();
        List<Clinic> clinics = clinicDAO.findAll();

        if (service instanceof GeneralService gs) {
            model.addAttribute("generalService", gs);
            model.addAttribute("testService", new TestService());
            model.addAttribute("serviceKind", "GENERAL");
        } else if (service instanceof TestService ts) {
            model.addAttribute("generalService", new GeneralService());
            model.addAttribute("testService", ts);
            model.addAttribute("serviceKind", "TEST");
        }

        model.addAttribute("clinics", clinics);
        model.addAttribute("isEdit", true);
        model.addAttribute("editId", id);
        model.addAttribute("loggedUser", session.getAttribute("loggedUser"));
        return "manager/service-form";
    }

    @PostMapping("/services/update/{id}")
    public String updateService(@PathVariable int id,
                                @RequestParam String serviceKind,
                                @RequestParam String name,
                                @RequestParam String type,
                                @RequestParam(required = false) String des,
                                @RequestParam double price,
                                @RequestParam(required = false, defaultValue = "false") boolean isActive,
                                @RequestParam(required = false) String preparationInstructions,
                                @RequestParam(required = false) String method,
                                @RequestParam(required = false) Integer clinicId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }

        Optional<Service> serviceOpt = serviceDAO.getById(id);
        if (serviceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy dịch vụ!");
            return "redirect:/manager/services";
        }

        Clinic clinic = null;
        if (clinicId != null) {
            clinic = clinicDAO.findById(clinicId).orElse(null);
        }

        Service service = serviceOpt.get();
        service.setName(name);
        service.setType(type);
        service.setDes(des);
        service.setPrice(price);
        service.setClinic(clinic);

        if (service instanceof GeneralService gs) {
            gs.setActive(isActive);
            serviceDAO.update(gs);
        } else if (service instanceof TestService ts) {
            ts.setPreparationInstructions(preparationInstructions);
            ts.setMethod(method);
            serviceDAO.update(ts);
        }

        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật dịch vụ thành công!");
        return "redirect:/manager/services";
    }

    @PostMapping("/services/delete/{id}")
    public String deleteService(@PathVariable int id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }
        if (serviceDAO.existsById(id)) {
            serviceDAO.delete(id);
            redirectAttributes.addFlashAttribute("successMsg", "Xóa dịch vụ thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy dịch vụ để xóa!");
        }
        return "redirect:/manager/services";
    }
}
