package com.clinic.management.controller;

import com.clinic.management.model.Clinic;
import com.clinic.management.model.GeneralService;
import com.clinic.management.model.Service;
import com.clinic.management.model.TestService;
import com.clinic.management.model.User;
import com.clinic.management.service.ServiceCatalogService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    private final ServiceCatalogService serviceCatalog;

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
        model.addAttribute("totalServices", serviceCatalog.countAllServices());
        model.addAttribute("totalGeneral", serviceCatalog.countGeneralServices());
        model.addAttribute("totalTest", serviceCatalog.countTestServices());
        model.addAttribute("loggedUser", session.getAttribute("loggedUser"));
        return "manager/home";
    }

    @GetMapping("/services")
    public String getAllServices(@ModelAttribute("service") Service serviceCriteria,
                                 @RequestParam(defaultValue = "0") int page,
                                 HttpSession session,
                                 Model model) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }

        Page<Service> servicePage = serviceCatalog.findServicesForList(serviceCriteria, page);

        model.addAttribute("servicePage", servicePage);
        model.addAttribute("services", servicePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("loggedUser", session.getAttribute("loggedUser"));
        return "manager/services";
    }

    @GetMapping("/services/detail/{id}")
    public String serviceDetail(@PathVariable int id,
                                @RequestParam(required = false) String name,
                                @RequestParam(required = false) String type,
                                @RequestParam(required = false) Integer page,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        String redir = guardManager(session);
        if (redir != null) {
            return redir;
        }

        Optional<Service> serviceOpt = serviceCatalog.getById(id);
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
        Service returnCriteria = new Service();
        returnCriteria.setName(name != null ? name : "");
        returnCriteria.setType(type);
        model.addAttribute("returnService", returnCriteria);
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
        List<Clinic> clinics = serviceCatalog.findAllClinics();
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
        serviceCatalog.addGeneral(generalService, clinicId);
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
        serviceCatalog.addTest(testService, clinicId);
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

        Optional<Service> serviceOpt = serviceCatalog.getById(id);
        if (serviceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy dịch vụ!");
            return "redirect:/manager/services";
        }

        Service service = serviceOpt.get();
        List<Clinic> clinics = serviceCatalog.findAllClinics();

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

        if (!serviceCatalog.updateGeneral(id, incoming, clinicId)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy hoặc loại dịch vụ không khớp!");
            return "redirect:/manager/services";
        }
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

        if (!serviceCatalog.updateTest(id, incoming, clinicId)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy hoặc loại dịch vụ không khớp!");
            return "redirect:/manager/services";
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
        if (serviceCatalog.deleteById(id)) {
            redirectAttributes.addFlashAttribute("successMsg", "Xóa dịch vụ thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy dịch vụ để xóa!");
        }
        return "redirect:/manager/services";
    }
}
