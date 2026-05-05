package com.clinic.management.config;

import com.clinic.management.dao.ClinicDAO;
import com.clinic.management.dao.ServiceDAO;
import com.clinic.management.dao.UserDAO;
import com.clinic.management.model.Clinic;
import com.clinic.management.model.GeneralService;
import com.clinic.management.model.TestService;
import com.clinic.management.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ClinicDAO clinicDAO;
    private final ServiceDAO serviceDAO;
    private final UserDAO userDAO;

    @Override
    public void run(String... args) {
        if (userDAO.count() == 0) {
            User manager = new User();
            manager.setUsername("admin");
            manager.setPassword("admin123");
            manager.setFullName("Nguyễn Văn Quản Lý");
            manager.setRole("MANAGER");
            userDAO.insert(manager);
            System.out.println("=== Tài khoản quản lý mẫu: admin / admin123 ===");
        }

        if (serviceDAO.countAllServices() > 0) {
            return;
        }

        Clinic clinic = new Clinic();
        clinic.setName("Phòng Khám Tư Nhân Đức Thịnh");
        clinic.setAddress("123 Đường Láng, Đống Đa, Hà Nội");
        clinic.setDes("Phòng khám tư nhân uy tín, chất lượng cao");
        clinicDAO.insert(clinic);

        GeneralService gs1 = new GeneralService();
        gs1.setName("Khám tổng quát");
        gs1.setType("GENERAL");
        gs1.setDes("Khám sức khỏe tổng quát toàn thân");
        gs1.setPrice(200_000);
        gs1.setActive(true);
        gs1.setClinic(clinic);
        serviceDAO.add(gs1);

        GeneralService gs2 = new GeneralService();
        gs2.setName("Khám nội khoa");
        gs2.setType("GENERAL");
        gs2.setDes("Khám và điều trị các bệnh nội khoa");
        gs2.setPrice(250_000);
        gs2.setActive(true);
        gs2.setClinic(clinic);
        serviceDAO.add(gs2);

        TestService ts1 = new TestService();
        ts1.setName("Xét nghiệm máu tổng quát");
        ts1.setType("TEST");
        ts1.setDes("Xét nghiệm công thức máu đầy đủ CBC");
        ts1.setPrice(300_000);
        ts1.setPreparationInstructions("Nhịn ăn ít nhất 8 tiếng trước khi xét nghiệm.");
        ts1.setMethod("Lấy mẫu máu tĩnh mạch, phân tích tự động");
        ts1.setClinic(clinic);
        serviceDAO.add(ts1);

        System.out.println("=== Đã khởi tạo phòng khám + dịch vụ mẫu ===");
    }
}
