package com.clinic.management.config;

import com.clinic.management.dao.*;
import com.clinic.management.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Khởi tạo dữ liệu mẫu khi DB trống (dùng JDBC / DAO giống định dạng demo CNPM của thầy).
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserDAO userDAO;
    private final ClinicDAO clinicDAO;
    private final ServiceRoomDAO serviceRoomDAO;
    private final SpecializationDAO specializationDAO;
    private final DoctorDAO doctorDAO;
    private final ServiceDAO serviceDAO;
    private final ScheduleRegisterDAO scheduleRegisterDAO;

    @Override
    public void run(String... args) {
        if (userDAO.count() > 0) return;

        User manager = new User();
        manager.setUsername("admin");
        manager.setPassword("admin123");
        manager.setFullName("Nguyễn Văn Quản Lý");
        manager.setRole("MANAGER");
        userDAO.insert(manager);

        User patient = new User();
        patient.setUsername("patient1");
        patient.setPassword("patient123");
        patient.setFullName("Trần Thị Bệnh Nhân");
        patient.setRole("PATIENT");
        userDAO.insert(patient);

        Clinic clinic = new Clinic();
        clinic.setName("Phòng Khám Tư Nhân Đức Thịnh");
        clinic.setAddress("123 Đường Láng, Đống Đa, Hà Nội");
        clinic.setDes("Phòng khám tư nhân uy tín, chất lượng cao");
        clinicDAO.insert(clinic);

        ServiceRoom room1 = new ServiceRoom();
        room1.setName("Phòng Khám 01");
        room1.setType("Khám bệnh");
        room1.setDes("Phòng khám tổng quát");
        room1.setClinic(clinic);
        serviceRoomDAO.insert(room1);

        ServiceRoom room2 = new ServiceRoom();
        room2.setName("Phòng Xét Nghiệm 01");
        room2.setType("Xét nghiệm");
        room2.setDes("Phòng xét nghiệm máu và sinh hóa");
        room2.setClinic(clinic);
        serviceRoomDAO.insert(room2);

        Specialization spec1 = new Specialization();
        spec1.setName("Nội khoa");
        spec1.setDes("Chuyên về bệnh nội tạng");
        specializationDAO.insert(spec1);

        Specialization spec2 = new Specialization();
        spec2.setName("Da liễu");
        spec2.setDes("Chuyên về bệnh da và các vấn đề ngoài da");
        specializationDAO.insert(spec2);

        Specialization spec3 = new Specialization();
        spec3.setName("Xét nghiệm");
        spec3.setDes("Chuyên thực hiện các xét nghiệm y tế");
        specializationDAO.insert(spec3);

        Doctor doctor1 = new Doctor();
        doctor1.setName("BS. Nguyễn Văn An");
        doctor1.setMajor("Nội khoa");
        doctor1.setPhoneNumber("0901234567");
        doctor1.setEmail("bs.an@clinic.vn");
        doctor1.setSpecialization(spec1);
        doctorDAO.insert(doctor1);

        Doctor doctor2 = new Doctor();
        doctor2.setName("BS. Trần Thị Bình");
        doctor2.setMajor("Da liễu");
        doctor2.setPhoneNumber("0907654321");
        doctor2.setEmail("bs.binh@clinic.vn");
        doctor2.setSpecialization(spec2);
        doctorDAO.insert(doctor2);

        Doctor doctor3 = new Doctor();
        doctor3.setName("BS. Lê Minh Cường");
        doctor3.setMajor("Xét nghiệm");
        doctor3.setPhoneNumber("0909876543");
        doctor3.setEmail("bs.cuong@clinic.vn");
        doctor3.setSpecialization(spec3);
        doctorDAO.insert(doctor3);

        GeneralService gs1 = new GeneralService();
        gs1.setName("Khám tổng quát");
        gs1.setType("GENERAL");
        gs1.setDes("Khám sức khỏe tổng quát toàn thân");
        gs1.setPrice(200000);
        gs1.setActive(true);
        gs1.setClinic(clinic);
        serviceDAO.add(gs1);

        GeneralService gs2 = new GeneralService();
        gs2.setName("Khám nội khoa");
        gs2.setType("GENERAL");
        gs2.setDes("Khám và điều trị các bệnh nội khoa");
        gs2.setPrice(250000);
        gs2.setActive(true);
        gs2.setClinic(clinic);
        serviceDAO.add(gs2);

        GeneralService gs3 = new GeneralService();
        gs3.setName("Tư vấn sức khỏe");
        gs3.setType("GENERAL");
        gs3.setDes("Tư vấn chế độ dinh dưỡng và lối sống lành mạnh");
        gs3.setPrice(150000);
        gs3.setActive(true);
        gs3.setClinic(clinic);
        serviceDAO.add(gs3);

        TestService ts1 = new TestService();
        ts1.setName("Xét nghiệm máu tổng quát");
        ts1.setType("TEST");
        ts1.setDes("Xét nghiệm công thức máu đầy đủ CBC");
        ts1.setPrice(300000);
        ts1.setPreparationInstructions("Nhịn ăn ít nhất 8 tiếng trước khi xét nghiệm. Uống nhiều nước lọc.");
        ts1.setMethod("Lấy mẫu máu tĩnh mạch, phân tích bằng máy huyết học tự động");
        ts1.setClinic(clinic);
        serviceDAO.add(ts1);

        TestService ts2 = new TestService();
        ts2.setName("Xét nghiệm sinh hóa máu");
        ts2.setType("TEST");
        ts2.setDes("Kiểm tra đường huyết, cholesterol, chức năng gan thận");
        ts2.setPrice(450000);
        ts2.setPreparationInstructions("Nhịn ăn 10-12 tiếng. Không uống rượu bia 24h trước.");
        ts2.setMethod("Lấy mẫu máu, phân tích trên máy sinh hóa tự động");
        ts2.setClinic(clinic);
        serviceDAO.add(ts2);

        TestService ts3 = new TestService();
        ts3.setName("Xét nghiệm nước tiểu");
        ts3.setType("TEST");
        ts3.setDes("Phân tích tổng phân tích nước tiểu 10 thông số");
        ts3.setPrice(120000);
        ts3.setPreparationInstructions("Lấy mẫu nước tiểu giữa dòng buổi sáng ngủ dậy.");
        ts3.setMethod("Phân tích bằng que thử và máy phân tích nước tiểu tự động");
        ts3.setClinic(clinic);
        serviceDAO.add(ts3);

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfter = today.plusDays(2);

        LocalTime[] slots = {
                LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0),
                LocalTime.of(14, 0), LocalTime.of(15, 0), LocalTime.of(16, 0)
        };

        for (LocalDate d : new LocalDate[]{today, tomorrow, dayAfter}) {
            for (LocalTime t : slots) {
                ScheduleRegister sr = new ScheduleRegister();
                sr.setDateScheduleRegister(d);
                sr.setTimeScheduleRegister(t);
                sr.setClinic(clinic);
                sr.setDoctor(doctor1);
                scheduleRegisterDAO.insert(sr);
            }
            for (LocalTime t : slots) {
                ScheduleRegister sr = new ScheduleRegister();
                sr.setDateScheduleRegister(d);
                sr.setTimeScheduleRegister(t);
                sr.setClinic(clinic);
                sr.setDoctor(doctor2);
                scheduleRegisterDAO.insert(sr);
            }
            for (LocalTime t : slots) {
                ScheduleRegister sr = new ScheduleRegister();
                sr.setDateScheduleRegister(d);
                sr.setTimeScheduleRegister(t);
                sr.setClinic(clinic);
                sr.setDoctor(doctor3);
                scheduleRegisterDAO.insert(sr);
            }
        }

        System.out.println("=== Dữ liệu mẫu đã được tạo thành công! ===");
        System.out.println("Manager: admin / admin123");
        System.out.println("Patient: patient1 / patient123");
    }
}
