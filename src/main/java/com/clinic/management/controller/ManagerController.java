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

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private static final int PAGE_SIZE = 4;

    private final ServiceDAO serviceDAO;
    private final ClinicDAO clinicDAO;

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

    private void attachClinic(Service svc, Integer clinicId) {
        if (clinicId != null && clinicId > 0) {
            svc.setClinic(clinicDAO.findById(clinicId).orElse(null));
        } else {
            svc.setClinic(null);
        }
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

    @GetMapping("/services/detail/{id}")
    public String serviceDetail(@PathVariable int id,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String type,
                                @RequestParam(required = false) Integer page,
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

        Service svc = serviceOpt.get();
        model.addAttribute("service", svc);
        if (svc instanceof GeneralService gs) {
            model.addAttribute("generalService", gs);
            model.addAttribute("serviceKind", "GENERAL");
        } else if (svc instanceof TestService ts) {
            model.addAttribute("testService", ts);
            model.addAttribute("serviceKind", "TEST");
        }
        model.addAttribute("returnKeyword", keyword);
        model.addAttribute("returnType", type);
        model.addAttribute("returnPage", page != null ? page : 0);
        model.addAttribute("loggedUser", session.getAttribute("loggedUser"));
        return "manager/service-detail";
    }

    @GetMapping("/services/new")
    public String showAddForm(HttpSession session, Model model) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }
        List<Clinic> clinics = clinicDAO.findAll();
        GeneralService gs = new GeneralService();
        gs.setType("GENERAL");
        TestService ts = new TestService();
        ts.setType("TEST");
        model.addAttribute("generalService", gs);
        model.addAttribute("testService", ts);
        model.addAttribute("clinics", clinics);
        model.addAttribute("isEdit", false);
        model.addAttribute("loggedUser", session.getAttribute("loggedUser"));
        return "manager/service-form";
    }

    @PostMapping("/services/general/add")
    public String addGeneral(@ModelAttribute("generalService") GeneralService generalService,
                             @RequestParam(required = false) Integer clinicId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }
        attachClinic(generalService, clinicId);
        serviceDAO.add(generalService);
        redirectAttributes.addFlashAttribute("successMsg", "Thêm dịch vụ thành công!");
        return "redirect:/manager/services";
    }

    @PostMapping("/services/test/add")
    public String addTest(@ModelAttribute("testService") TestService testService,
                          @RequestParam(required = false) Integer clinicId,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }
        attachClinic(testService, clinicId);
        serviceDAO.add(testService);
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
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Loại dịch vụ không hợp lệ (không phải khám hoặc xét nghiệm).");
            return "redirect:/manager/services";
        }

        model.addAttribute("clinics", clinics);
        model.addAttribute("isEdit", true);
        model.addAttribute("editId", id);
        model.addAttribute("loggedUser", session.getAttribute("loggedUser"));
        return "manager/service-form";
    }

    @PostMapping("/services/general/update/{id}")
    public String updateGeneral(@PathVariable int id,
                                @ModelAttribute("generalService") GeneralService incoming,
                                @RequestParam(required = false) Integer clinicId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }

        Optional<Service> existing = serviceDAO.getById(id);
        if (existing.isEmpty() || !(existing.get() instanceof GeneralService)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy hoặc loại dịch vụ không khớp!");
            return "redirect:/manager/services";
        }

        incoming.setId(id);
        attachClinic(incoming, clinicId);
        serviceDAO.update(incoming);
        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật dịch vụ thành công!");
        return "redirect:/manager/services";
    }

    @PostMapping("/services/test/update/{id}")
    public String updateTest(@PathVariable int id,
                             @ModelAttribute("testService") TestService incoming,
                             @RequestParam(required = false) Integer clinicId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }

        Optional<Service> existing = serviceDAO.getById(id);
        if (existing.isEmpty() || !(existing.get() instanceof TestService)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy hoặc loại dịch vụ không khớp!");
            return "redirect:/manager/services";
        }

        incoming.setId(id);
        attachClinic(incoming, clinicId);
        serviceDAO.update(incoming);
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
