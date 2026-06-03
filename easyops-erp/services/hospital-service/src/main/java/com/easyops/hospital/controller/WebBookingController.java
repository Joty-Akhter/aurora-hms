package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.WebBookingAppointmentRequest;
import com.easyops.hospital.dto.response.WebBookableDoctorResponse;
import com.easyops.hospital.dto.response.WebBookingAppointmentResponse;
import com.easyops.hospital.dto.response.WebBookingAvailabilityResponse;
import com.easyops.hospital.service.WebBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/web-booking")
@RequiredArgsConstructor
@Tag(name = "Public Web Booking", description = "Public appointment booking APIs for aurora.hospital")
public class WebBookingController {

    private final WebBookingService webBookingService;

    @GetMapping("/doctors")
    @Operation(summary = "List doctors available for web booking")
    public ResponseEntity<List<WebBookableDoctorResponse>> listDoctors() {
        return ResponseEntity.ok(webBookingService.listWebBookableDoctors());
    }

    @GetMapping("/doctors/{doctorId}/availability")
    @Operation(summary = "Get available appointment slots for a doctor")
    public ResponseEntity<List<WebBookingAvailabilityResponse>> getAvailability(
            @PathVariable UUID doctorId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(webBookingService.getDoctorAvailability(doctorId, fromDate, toDate));
    }

    @PostMapping("/appointments")
    @Operation(summary = "Book an appointment from the public website")
    public ResponseEntity<WebBookingAppointmentResponse> bookAppointment(
            @Valid @RequestBody WebBookingAppointmentRequest request) {
        WebBookingAppointmentResponse response = webBookingService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
