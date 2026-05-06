package com.clinic.management.service;

import com.clinic.management.dao.ClinicDAO;
import com.clinic.management.dao.ServiceDAO;
import com.clinic.management.model.Clinic;
import com.clinic.management.model.GeneralService;
import com.clinic.management.model.Service;
import com.clinic.management.model.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    public static final int PAGE_SIZE = 4;

    private final ServiceDAO serviceDAO;
    private final ClinicDAO clinicDAO;

    private void attachClinic(Service svc, Integer clinicId) {
        if (clinicId != null && clinicId > 0) {
            svc.setClinic(clinicDAO.findById(clinicId).orElse(null));
        } else {
            svc.setClinic(null);
        }
    }

    public long countAllServices() {
        return serviceDAO.countAllServices();
    }

    public long countGeneralServices() {
        return serviceDAO.countGeneralServices();
    }

    public long countTestServices() {
        return serviceDAO.countTestServices();
    }

    public Page<Service> findServicesForList(Service serviceCriteria, int page) {
        String nameFilter =
                serviceCriteria.getName() != null ? serviceCriteria.getName().trim() : "";

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending());
        boolean hasKeyword = !nameFilter.isBlank();
        String typeFilter = serviceCriteria.getType();
        boolean hasType = typeFilter != null && !typeFilter.isBlank();

        Page<Service> servicePage;
        if (hasKeyword && hasType) {
            servicePage = serviceDAO.findPaged(nameFilter, typeFilter, pageable);
        } else if (hasKeyword) {
            servicePage = serviceDAO.findPaged(nameFilter, null, pageable);
        } else if (hasType) {
            servicePage = serviceDAO.findPaged(null, typeFilter, pageable);
        } else {
            servicePage = serviceDAO.findPaged(null, null, pageable);
        }

        serviceCriteria.setName(nameFilter);
        return servicePage;
    }

    public Optional<Service> getById(int id) {
        return serviceDAO.getById(id);
    }

    public List<Clinic> findAllClinics() {
        return clinicDAO.findAll();
    }

    public void addGeneral(GeneralService generalService, Integer clinicId) {
        attachClinic(generalService, clinicId);
        serviceDAO.add(generalService);
    }

    public void addTest(TestService testService, Integer clinicId) {
        attachClinic(testService, clinicId);
        serviceDAO.add(testService);
    }

    public boolean updateGeneral(int id, GeneralService incoming, Integer clinicId) {
        Optional<Service> existing = serviceDAO.getById(id);
        if (existing.isEmpty() || !(existing.get() instanceof GeneralService)) {
            return false;
        }
        incoming.setId(id);
        attachClinic(incoming, clinicId);
        serviceDAO.update(incoming);
        return true;
    }

    public boolean updateTest(int id, TestService incoming, Integer clinicId) {
        Optional<Service> existing = serviceDAO.getById(id);
        if (existing.isEmpty() || !(existing.get() instanceof TestService)) {
            return false;
        }
        incoming.setId(id);
        attachClinic(incoming, clinicId);
        serviceDAO.update(incoming);
        return true;
    }

    public boolean deleteById(int id) {
        if (!serviceDAO.existsById(id)) {
            return false;
        }
        serviceDAO.delete(id);
        return true;
    }
}
