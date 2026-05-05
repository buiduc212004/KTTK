package com.clinic.management.controller;

import com.clinic.management.dao.*;
import com.clinic.management.model.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Controller cho luồng đặt lịch online của bệnh nhân/khách hàng.
 * Dữ liệu truy cập qua tầng DAO (JDBC) — format giống demo CNPM của thầy; giao diện Thymeleaf giữ nguyên.
 */
@Controller
@RequestMapping("/patient")
@RequiredArgsConstructor
public class BookingController {

    private static final int PAGE_SIZE = 12;

    private final ServiceDAO serviceDAO;
    private final DoctorDAO doctorDAO;
    private final ScheduleRegisterDAO scheduleRegisterDAO;
    private final ScheduleCustomerBookingDAO scheduleCustomerBookingDAO;
    private final PatientDAO patientDAO;
    private final BookingReceiptDAO bookingReceiptDAO;
    private final ServiceRoomDAO serviceRoomDAO;

    private User checkPatientAccess(HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return null;
        return loggedUser;
    }

    @GetMapping("/home")
    public String patientHome(HttpSession session, Model model) {
        User loggedUser = checkPatientAccess(session);
        if (loggedUser == null) return "redirect:/login";

        List<BookingReceipt> myBookings = bookingReceiptDAO.findByUserIdHydrated(loggedUser.getId());
        model.addAttribute("loggedUser", loggedUser);
        model.addAttribute("myBookings", myBookings);
        return "patient/home";
    }

    @GetMapping("/booking/services")
    public String showServices(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String type,
                               @RequestParam(defaultValue = "0") int page,
                               HttpSession session,
                               Model model) {
        User loggedUser = checkPatientAccess(session);
        if (loggedUser == null) return "redirect:/login";

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending());
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasType    = type != null && !type.isBlank();

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
        model.addAttribute("loggedUser", loggedUser);
        return "patient/services";
    }

    @PostMapping("/booking/select-service")
    public String selectService(@RequestParam int serviceId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User loggedUser = checkPatientAccess(session);
        if (loggedUser == null) return "redirect:/login";

        Optional<Service> serviceOpt = serviceDAO.getById(serviceId);
        if (serviceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Dịch vụ không tồn tại!");
            return "redirect:/patient/booking/services";
        }

        session.setAttribute("selectedService", serviceOpt.get());
        return "redirect:/patient/booking/schedule";
    }

    @GetMapping("/booking/schedule")
    public String showSchedule(HttpSession session, Model model,
                               @RequestParam(required = false) Integer doctorId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User loggedUser = checkPatientAccess(session);
        if (loggedUser == null) return "redirect:/login";

        Service selectedService = (Service) session.getAttribute("selectedService");
        if (selectedService == null) return "redirect:/patient/booking/services";

        List<Doctor> doctors = doctorDAO.findAll();
        model.addAttribute("loggedUser", loggedUser);
        model.addAttribute("selectedService", selectedService);
        model.addAttribute("doctors", doctors);
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("selectedDate", date);

        if (doctorId != null) {
            List<LocalDate> availableDates = scheduleRegisterDAO.findAvailableDatesByDoctorId(doctorId);
            model.addAttribute("availableDates", availableDates);
            doctorDAO.findById(doctorId).ifPresent(d -> model.addAttribute("selectedDoctor", d));

            if (date != null) {
                List<LocalTime> availableSlots = getAvailableSlots(doctorId, date);
                model.addAttribute("availableSlots", availableSlots);
            }
        }

        return "patient/schedule";
    }

    private List<LocalTime> getAvailableSlots(int doctorId, LocalDate date) {
        List<ScheduleRegister> registers = scheduleRegisterDAO.findByDoctorIdAndDate(doctorId, date);

        List<LocalTime> allSlots = new ArrayList<>();
        for (ScheduleRegister sr : registers) {
            if (sr.getTimeScheduleRegister() != null) {
                allSlots.add(sr.getTimeScheduleRegister());
            }
        }

        List<ScheduleCustomerBooking> booked =
                scheduleCustomerBookingDAO.findByDoctorIdAndDateSchedule(doctorId, date);

        List<LocalTime> bookedTimes = booked.stream()
                .map(ScheduleCustomerBooking::getTimeSchedule)
                .filter(Objects::nonNull)
                .toList();

        allSlots.removeAll(bookedTimes);
        Collections.sort(allSlots);
        return allSlots;
    }

    @PostMapping("/booking/select-slot")
    public String selectSlot(@RequestParam int doctorId,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam String time,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User loggedUser = checkPatientAccess(session);
        if (loggedUser == null) return "redirect:/login";

        LocalTime selectedTime = LocalTime.parse(time);

        boolean alreadyBooked = scheduleCustomerBookingDAO
                .existsByDoctorIdAndDateScheduleAndTimeSchedule(doctorId, date, selectedTime);
        if (alreadyBooked) {
            redirectAttributes.addFlashAttribute("errorMsg", "Khung giờ này đã có người đặt, vui lòng chọn giờ khác!");
            return "redirect:/patient/booking/schedule?doctorId=" + doctorId + "&date=" + date;
        }

        Doctor doctor = doctorDAO.findById(doctorId).orElse(null);
        session.setAttribute("selectedDoctor", doctor);
        session.setAttribute("selectedDoctorId", doctorId);
        session.setAttribute("selectedDate", date);
        session.setAttribute("selectedTime", selectedTime);

        return "redirect:/patient/booking/patient-info";
    }

    @GetMapping("/booking/patient-info")
    public String showPatientForm(HttpSession session, Model model) {
        User loggedUser = checkPatientAccess(session);
        if (loggedUser == null) return "redirect:/login";

        if (session.getAttribute("selectedService") == null ||
            session.getAttribute("selectedDoctorId") == null) {
            return "redirect:/patient/booking/services";
        }

        int doctorId = (int) session.getAttribute("selectedDoctorId");
        Doctor doctor = doctorDAO.findById(doctorId).orElse(null);

        model.addAttribute("loggedUser", loggedUser);
        model.addAttribute("patient", new Patient());
        model.addAttribute("selectedService", session.getAttribute("selectedService"));
        model.addAttribute("selectedDoctor", doctor);
        model.addAttribute("selectedDate", session.getAttribute("selectedDate"));
        model.addAttribute("selectedTime", session.getAttribute("selectedTime"));
        return "patient/patient-form";
    }

    @PostMapping("/booking/submit-patient")
    public String submitPatientInfo(@RequestParam String name,
                                    @RequestParam(required = false) String gender,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
                                    @RequestParam(required = false) String telNumber,
                                    @RequestParam(required = false) String email,
                                    @RequestParam(required = false) String allergies,
                                    @RequestParam(required = false) String note,
                                    HttpSession session) {
        User loggedUser = checkPatientAccess(session);
        if (loggedUser == null) return "redirect:/login";

        Patient patient = new Patient();
        patient.setName(name);
        patient.setGender(gender);
        patient.setDateOfBirth(dateOfBirth);
        patient.setTelNumber(telNumber);
        patient.setEmail(email);
        patient.setAllergies(allergies);
        patientDAO.insert(patient);

        session.setAttribute("currentPatient", patient);
        session.setAttribute("patientNote", note);

        return "redirect:/patient/booking/confirm";
    }

    @GetMapping("/booking/confirm")
    public String showConfirm(HttpSession session, Model model) {
        User loggedUser = checkPatientAccess(session);
        if (loggedUser == null) return "redirect:/login";

        Service selectedService = (Service) session.getAttribute("selectedService");
        LocalDate selectedDate  = (LocalDate) session.getAttribute("selectedDate");
        LocalTime selectedTime  = (LocalTime) session.getAttribute("selectedTime");
        Patient patient         = (Patient) session.getAttribute("currentPatient");
        Integer doctorId        = (Integer) session.getAttribute("selectedDoctorId");

        if (selectedService == null || doctorId == null || patient == null) {
            return "redirect:/patient/booking/services";
        }

        Doctor selectedDoctor = doctorDAO.findById(doctorId).orElse(null);

        double total = selectedService.getPrice();

        model.addAttribute("loggedUser", loggedUser);
        model.addAttribute("selectedService", selectedService);
        model.addAttribute("selectedDoctor", selectedDoctor);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedTime", selectedTime);
        model.addAttribute("patient", patient);
        model.addAttribute("note", session.getAttribute("patientNote"));
        model.addAttribute("total", total);
        return "patient/confirm";
    }

    @PostMapping("/booking/confirm")
    public String confirmBooking(HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedUser = checkPatientAccess(session);
        if (loggedUser == null) return "redirect:/login";

        Service selectedService = (Service) session.getAttribute("selectedService");
        LocalDate selectedDate  = (LocalDate) session.getAttribute("selectedDate");
        LocalTime selectedTime  = (LocalTime) session.getAttribute("selectedTime");
        Patient patient         = (Patient) session.getAttribute("currentPatient");
        String note             = (String) session.getAttribute("patientNote");
        Integer doctorId        = (Integer) session.getAttribute("selectedDoctorId");

        if (selectedService == null || doctorId == null || patient == null) {
            return "redirect:/patient/booking/services";
        }

        Doctor selectedDoctor = doctorDAO.findById(doctorId).orElse(null);
        if (selectedDoctor == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Bác sĩ không tồn tại!");
            return "redirect:/patient/booking/schedule";
        }

        boolean alreadyBooked = scheduleCustomerBookingDAO
                .existsByDoctorIdAndDateScheduleAndTimeSchedule(doctorId, selectedDate, selectedTime);
        if (alreadyBooked) {
            redirectAttributes.addFlashAttribute("errorMsg", "Khung giờ đã bị đặt bởi người khác! Vui lòng chọn lại.");
            return "redirect:/patient/booking/schedule";
        }

        ServiceRoom serviceRoom = serviceRoomDAO.findAll().stream().findFirst().orElse(null);

        ScheduleCustomerBooking booking = new ScheduleCustomerBooking();
        booking.setDateSchedule(selectedDate);
        booking.setTimeSchedule(selectedTime);
        booking.setIsState("PENDING");
        booking.setDoctor(selectedDoctor);
        booking.setServiceRoom(serviceRoom);
        scheduleCustomerBookingDAO.insert(booking);

        BookingReceipt receipt = new BookingReceipt();
        receipt.setBookingDate(LocalDate.now());
        receipt.setSellOff(0);
        receipt.setTotal(selectedService.getPrice());
        receipt.setNote(note);
        receipt.setPatient(patient);
        receipt.setUser(loggedUser);
        receipt.setScheduleCustomerBooking(booking);
        receipt.setService(selectedService);
        bookingReceiptDAO.insert(receipt);

        session.removeAttribute("selectedService");
        session.removeAttribute("selectedDoctor");
        session.removeAttribute("selectedDoctorId");
        session.removeAttribute("selectedDate");
        session.removeAttribute("selectedTime");
        session.removeAttribute("currentPatient");
        session.removeAttribute("patientNote");

        redirectAttributes.addFlashAttribute("successMsg", "Đặt lịch thành công! Chúng tôi sẽ liên hệ xác nhận sớm nhất.");
        return "redirect:/patient/home";
    }
}
