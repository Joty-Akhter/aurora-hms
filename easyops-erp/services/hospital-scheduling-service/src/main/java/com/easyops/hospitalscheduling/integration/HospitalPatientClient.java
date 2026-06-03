package com.easyops.hospitalscheduling.integration;

import com.easyops.hospitalscheduling.integration.dto.PatientContactForSms;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "hospital-service",
        path = "/api/patients",
        contextId = "hospitalPatientClient",
        configuration = HospitalPatientClientConfiguration.class)
public interface HospitalPatientClient {

    @GetMapping("/{patientId}")
    PatientContactForSms getPatient(@PathVariable("patientId") UUID patientId);
}
