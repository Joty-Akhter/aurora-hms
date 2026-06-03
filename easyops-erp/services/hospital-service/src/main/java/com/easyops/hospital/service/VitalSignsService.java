package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.VitalSignsRequest;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.entity.VitalSigns;
import com.easyops.hospital.repository.PatientRepository;
import com.easyops.hospital.repository.VitalSignsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VitalSignsService {
    
    private final VitalSignsRepository vitalSignsRepository;
    private final PatientRepository patientRepository;
    
    // Reference ranges for adults (can be made configurable)
    private static final int NORMAL_SYSTOLIC_MIN = 90;
    private static final int NORMAL_SYSTOLIC_MAX = 120;
    private static final int NORMAL_DIASTOLIC_MIN = 60;
    private static final int NORMAL_DIASTOLIC_MAX = 80;
    private static final int NORMAL_HEART_RATE_MIN = 60;
    private static final int NORMAL_HEART_RATE_MAX = 100;
    private static final int NORMAL_RESPIRATORY_MIN = 12;
    private static final int NORMAL_RESPIRATORY_MAX = 20;
    private static final BigDecimal NORMAL_TEMP_MIN_F = new BigDecimal("97.0");
    private static final BigDecimal NORMAL_TEMP_MAX_F = new BigDecimal("99.5");
    private static final BigDecimal NORMAL_TEMP_MIN_C = new BigDecimal("36.1");
    private static final BigDecimal NORMAL_TEMP_MAX_C = new BigDecimal("37.5");
    private static final BigDecimal NORMAL_SPO2_MIN = new BigDecimal("95.0");
    private static final BigDecimal NORMAL_SPO2_MAX = new BigDecimal("100.0");
    
    // Critical thresholds
    private static final int CRITICAL_SYSTOLIC_MIN = 70;
    private static final int CRITICAL_SYSTOLIC_MAX = 180;
    private static final int CRITICAL_DIASTOLIC_MIN = 40;
    private static final int CRITICAL_DIASTOLIC_MAX = 120;
    private static final int CRITICAL_HEART_RATE_MIN = 40;
    private static final int CRITICAL_HEART_RATE_MAX = 150;
    private static final int CRITICAL_RESPIRATORY_MIN = 8;
    private static final int CRITICAL_RESPIRATORY_MAX = 30;
    private static final BigDecimal CRITICAL_SPO2_MIN = new BigDecimal("90.0");
    
    /**
     * Calculate BMI from weight and height
     */
    public BigDecimal calculateBmi(BigDecimal weight, VitalSigns.WeightUnit weightUnit, 
                                   BigDecimal height, VitalSigns.HeightUnit heightUnit) {
        if (weight == null || height == null || height.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        // Convert weight to kg
        BigDecimal weightKg;
        if (weightUnit == VitalSigns.WeightUnit.lbs) {
            weightKg = weight.multiply(new BigDecimal("0.453592"));
        } else {
            weightKg = weight;
        }
        
        // Convert height to meters
        BigDecimal heightM;
        if (heightUnit == VitalSigns.HeightUnit.in) {
            heightM = height.multiply(new BigDecimal("0.0254"));
        } else if (heightUnit == VitalSigns.HeightUnit.cm) {
            heightM = height.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            heightM = height;
        }
        
        // BMI = weight (kg) / height (m)^2
        BigDecimal heightSquared = heightM.multiply(heightM);
        return weightKg.divide(heightSquared, 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Check if vital signs are abnormal or critical
     */
    private void checkAbnormalValues(VitalSigns vitalSigns) {
        List<String> abnormalReasons = new ArrayList<>();
        boolean isAbnormal = false;
        boolean isCritical = false;
        
        // Check Blood Pressure
        if (vitalSigns.getSystolicBp() != null && vitalSigns.getDiastolicBp() != null) {
            if (vitalSigns.getSystolicBp() < CRITICAL_SYSTOLIC_MIN || 
                vitalSigns.getSystolicBp() > CRITICAL_SYSTOLIC_MAX ||
                vitalSigns.getDiastolicBp() < CRITICAL_DIASTOLIC_MIN || 
                vitalSigns.getDiastolicBp() > CRITICAL_DIASTOLIC_MAX) {
                isCritical = true;
                abnormalReasons.add("Critical blood pressure: " + 
                    vitalSigns.getSystolicBp() + "/" + vitalSigns.getDiastolicBp());
            } else if (vitalSigns.getSystolicBp() < NORMAL_SYSTOLIC_MIN || 
                      vitalSigns.getSystolicBp() > NORMAL_SYSTOLIC_MAX ||
                      vitalSigns.getDiastolicBp() < NORMAL_DIASTOLIC_MIN || 
                      vitalSigns.getDiastolicBp() > NORMAL_DIASTOLIC_MAX) {
                isAbnormal = true;
                abnormalReasons.add("Abnormal blood pressure: " + 
                    vitalSigns.getSystolicBp() + "/" + vitalSigns.getDiastolicBp());
            }
        }
        
        // Check Heart Rate
        if (vitalSigns.getHeartRate() != null) {
            if (vitalSigns.getHeartRate() < CRITICAL_HEART_RATE_MIN || 
                vitalSigns.getHeartRate() > CRITICAL_HEART_RATE_MAX) {
                isCritical = true;
                abnormalReasons.add("Critical heart rate: " + vitalSigns.getHeartRate());
            } else if (vitalSigns.getHeartRate() < NORMAL_HEART_RATE_MIN || 
                      vitalSigns.getHeartRate() > NORMAL_HEART_RATE_MAX) {
                isAbnormal = true;
                abnormalReasons.add("Abnormal heart rate: " + vitalSigns.getHeartRate());
            }
        }
        
        // Check Respiratory Rate
        if (vitalSigns.getRespiratoryRate() != null) {
            if (vitalSigns.getRespiratoryRate() < CRITICAL_RESPIRATORY_MIN || 
                vitalSigns.getRespiratoryRate() > CRITICAL_RESPIRATORY_MAX) {
                isCritical = true;
                abnormalReasons.add("Critical respiratory rate: " + vitalSigns.getRespiratoryRate());
            } else if (vitalSigns.getRespiratoryRate() < NORMAL_RESPIRATORY_MIN || 
                      vitalSigns.getRespiratoryRate() > NORMAL_RESPIRATORY_MAX) {
                isAbnormal = true;
                abnormalReasons.add("Abnormal respiratory rate: " + vitalSigns.getRespiratoryRate());
            }
        }
        
        // Check Temperature
        if (vitalSigns.getTemperature() != null) {
            BigDecimal tempMin, tempMax;
            if (vitalSigns.getTemperatureUnit() == VitalSigns.TemperatureUnit.C) {
                tempMin = NORMAL_TEMP_MIN_C;
                tempMax = NORMAL_TEMP_MAX_C;
            } else {
                tempMin = NORMAL_TEMP_MIN_F;
                tempMax = NORMAL_TEMP_MAX_F;
            }
            
            if (vitalSigns.getTemperature().compareTo(tempMin) < 0 || 
                vitalSigns.getTemperature().compareTo(tempMax) > 0) {
                isAbnormal = true;
                abnormalReasons.add("Abnormal temperature: " + vitalSigns.getTemperature() + 
                    vitalSigns.getTemperatureUnit());
            }
        }
        
        // Check Oxygen Saturation
        if (vitalSigns.getOxygenSaturation() != null) {
            if (vitalSigns.getOxygenSaturation().compareTo(CRITICAL_SPO2_MIN) < 0) {
                isCritical = true;
                abnormalReasons.add("Critical oxygen saturation: " + vitalSigns.getOxygenSaturation() + "%");
            } else if (vitalSigns.getOxygenSaturation().compareTo(NORMAL_SPO2_MIN) < 0) {
                isAbnormal = true;
                abnormalReasons.add("Abnormal oxygen saturation: " + vitalSigns.getOxygenSaturation() + "%");
            }
        }
        
        vitalSigns.setIsAbnormal(isAbnormal);
        vitalSigns.setIsCritical(isCritical);
        if (!abnormalReasons.isEmpty()) {
            vitalSigns.setAbnormalReason(String.join("; ", abnormalReasons));
        }
    }
    
    /**
     * Create vital signs record
     */
    @Transactional
    public VitalSignsResponse createVitalSigns(UUID patientId, VitalSignsRequest request, UUID userId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        VitalSigns vitalSigns = VitalSigns.builder()
            .patient(patient)
            .encounterId(request.getEncounterId())
            .measurementDate(request.getMeasurementDate())
            .measurementTime(request.getMeasurementTime())
            .systolicBp(request.getSystolicBp())
            .diastolicBp(request.getDiastolicBp())
            .heartRate(request.getHeartRate())
            .respiratoryRate(request.getRespiratoryRate())
            .temperature(request.getTemperature())
            .temperatureUnit(request.getTemperatureUnit() != null ? 
                request.getTemperatureUnit() : VitalSigns.TemperatureUnit.F)
            .oxygenSaturation(request.getOxygenSaturation())
            .weight(request.getWeight())
            .weightUnit(request.getWeightUnit() != null ? 
                request.getWeightUnit() : VitalSigns.WeightUnit.lbs)
            .height(request.getHeight())
            .heightUnit(request.getHeightUnit() != null ? 
                request.getHeightUnit() : VitalSigns.HeightUnit.in)
            .painScale(request.getPainScale())
            .bloodGlucose(request.getBloodGlucose())
            .headCircumference(request.getHeadCircumference())
            .measuredBy(userId)
            .measurementLocationId(request.getMeasurementLocationId())
            .deviceUsed(request.getDeviceUsed())
            .patientPosition(request.getPatientPosition())
            .notes(request.getNotes())
            .createdBy(userId)
            .build();
        
        // Calculate BMI if weight and height are provided
        if (vitalSigns.getWeight() != null && vitalSigns.getHeight() != null) {
            BigDecimal bmi = calculateBmi(vitalSigns.getWeight(), vitalSigns.getWeightUnit(),
                                         vitalSigns.getHeight(), vitalSigns.getHeightUnit());
            vitalSigns.setBmi(bmi);
        }
        
        // Check for abnormal/critical values
        checkAbnormalValues(vitalSigns);
        
        vitalSigns = vitalSignsRepository.save(vitalSigns);
        return mapToResponse(vitalSigns);
    }
    
    /**
     * Get vital signs by ID
     */
    public VitalSignsResponse getVitalSignsById(UUID vitalSignId) {
        VitalSigns vitalSigns = vitalSignsRepository.findById(vitalSignId)
            .orElseThrow(() -> new RuntimeException("Vital signs not found"));
        return mapToResponse(vitalSigns);
    }
    
    /**
     * Get all vital signs for a patient
     */
    public List<VitalSignsResponse> getVitalSignsByPatient(UUID patientId) {
        List<VitalSigns> vitalSignsList = vitalSignsRepository
            .findByPatientPatientIdOrderByMeasurementDateDescMeasurementTimeDesc(patientId);
        return vitalSignsList.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get vital signs by date range
     */
    public List<VitalSignsResponse> getVitalSignsByDateRange(UUID patientId, LocalDate startDate, LocalDate endDate) {
        List<VitalSigns> vitalSignsList = vitalSignsRepository
            .findByPatientPatientIdAndMeasurementDateBetween(patientId, startDate, endDate);
        return vitalSignsList.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get latest vital signs for a patient, or {@code null} when none recorded (normal for new patients).
     */
    public VitalSignsResponse getLatestVitalSigns(UUID patientId) {
        VitalSigns vitalSigns = vitalSignsRepository.findLatestVitalSignsByPatient(patientId);
        if (vitalSigns == null) {
            return null;
        }
        return mapToResponse(vitalSigns);
    }
    
    /**
     * Get abnormal vital signs
     */
    public List<VitalSignsResponse> getAbnormalVitalSigns(UUID patientId) {
        List<VitalSigns> vitalSignsList = vitalSignsRepository.findAbnormalVitalSignsByPatient(patientId);
        return vitalSignsList.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get critical vital signs
     */
    public List<VitalSignsResponse> getCriticalVitalSigns(UUID patientId) {
        List<VitalSigns> vitalSignsList = vitalSignsRepository.findCriticalVitalSignsByPatient(patientId);
        return vitalSignsList.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get vital signs trends
     */
    public List<VitalSignsTrendResponse> getVitalSignsTrends(UUID patientId, LocalDate startDate) {
        List<Object[]> trends = vitalSignsRepository.findVitalSignsTrends(patientId, startDate);
        return trends.stream()
            .map(this::mapTrendToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get vital signs summary
     */
    public VitalSignsSummaryResponse getVitalSignsSummary(UUID patientId) {
        VitalSigns latest = vitalSignsRepository.findLatestVitalSignsByPatient(patientId);
        
        List<VitalSigns> recent = vitalSignsRepository
            .findByPatientPatientIdOrderByMeasurementDateDescMeasurementTimeDesc(patientId)
            .stream()
            .limit(10)
            .collect(Collectors.toList());
        
        List<VitalSignsTrendResponse> trends = getVitalSignsTrends(patientId, LocalDate.now().minusDays(30));
        
        List<VitalSigns> allVitalSigns = vitalSignsRepository.findByPatientPatientId(patientId);
        long totalMeasurements = allVitalSigns.size();
        long abnormalCount = allVitalSigns.stream().filter(v -> Boolean.TRUE.equals(v.getIsAbnormal())).count();
        long criticalCount = allVitalSigns.stream().filter(v -> Boolean.TRUE.equals(v.getIsCritical())).count();
        
        return VitalSignsSummaryResponse.builder()
            .latest(latest != null ? mapToResponse(latest) : null)
            .recent(recent.stream().map(this::mapToResponse).collect(Collectors.toList()))
            .trends(trends)
            .totalMeasurements(totalMeasurements)
            .abnormalCount(abnormalCount)
            .criticalCount(criticalCount)
            .build();
    }
    
    /**
     * Update vital signs
     */
    @Transactional
    public VitalSignsResponse updateVitalSigns(UUID vitalSignId, VitalSignsRequest request, UUID userId) {
        VitalSigns vitalSigns = vitalSignsRepository.findById(vitalSignId)
            .orElseThrow(() -> new RuntimeException("Vital signs not found"));
        
        vitalSigns.setEncounterId(request.getEncounterId());
        vitalSigns.setMeasurementDate(request.getMeasurementDate());
        vitalSigns.setMeasurementTime(request.getMeasurementTime());
        vitalSigns.setSystolicBp(request.getSystolicBp());
        vitalSigns.setDiastolicBp(request.getDiastolicBp());
        vitalSigns.setHeartRate(request.getHeartRate());
        vitalSigns.setRespiratoryRate(request.getRespiratoryRate());
        vitalSigns.setTemperature(request.getTemperature());
        if (request.getTemperatureUnit() != null) {
            vitalSigns.setTemperatureUnit(request.getTemperatureUnit());
        }
        vitalSigns.setOxygenSaturation(request.getOxygenSaturation());
        vitalSigns.setWeight(request.getWeight());
        if (request.getWeightUnit() != null) {
            vitalSigns.setWeightUnit(request.getWeightUnit());
        }
        vitalSigns.setHeight(request.getHeight());
        if (request.getHeightUnit() != null) {
            vitalSigns.setHeightUnit(request.getHeightUnit());
        }
        vitalSigns.setPainScale(request.getPainScale());
        vitalSigns.setBloodGlucose(request.getBloodGlucose());
        vitalSigns.setHeadCircumference(request.getHeadCircumference());
        vitalSigns.setDeviceUsed(request.getDeviceUsed());
        vitalSigns.setPatientPosition(request.getPatientPosition());
        vitalSigns.setNotes(request.getNotes());
        vitalSigns.setUpdatedBy(userId);
        
        // Recalculate BMI if weight or height changed
        if (vitalSigns.getWeight() != null && vitalSigns.getHeight() != null) {
            BigDecimal bmi = calculateBmi(vitalSigns.getWeight(), vitalSigns.getWeightUnit(),
                                         vitalSigns.getHeight(), vitalSigns.getHeightUnit());
            vitalSigns.setBmi(bmi);
        }
        
        // Recheck abnormal/critical values
        checkAbnormalValues(vitalSigns);
        
        vitalSigns = vitalSignsRepository.save(vitalSigns);
        return mapToResponse(vitalSigns);
    }
    
    /**
     * Delete vital signs
     */
    @Transactional
    public void deleteVitalSigns(UUID vitalSignId) {
        vitalSignsRepository.deleteById(vitalSignId);
    }
    
    /**
     * Get vital signs by encounter
     */
    public List<VitalSignsResponse> getVitalSignsByEncounter(UUID encounterId) {
        List<VitalSigns> vitalSignsList = vitalSignsRepository.findByEncounterId(encounterId);
        return vitalSignsList.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // ========== Mapping Methods ==========
    
    private VitalSignsResponse mapToResponse(VitalSigns vitalSigns) {
        return VitalSignsResponse.builder()
            .vitalSignId(vitalSigns.getVitalSignId())
            .patientId(vitalSigns.getPatient().getPatientId())
            .encounterId(vitalSigns.getEncounterId())
            .measurementDate(vitalSigns.getMeasurementDate())
            .measurementTime(vitalSigns.getMeasurementTime())
            .systolicBp(vitalSigns.getSystolicBp())
            .diastolicBp(vitalSigns.getDiastolicBp())
            .heartRate(vitalSigns.getHeartRate())
            .respiratoryRate(vitalSigns.getRespiratoryRate())
            .temperature(vitalSigns.getTemperature())
            .temperatureUnit(vitalSigns.getTemperatureUnit())
            .oxygenSaturation(vitalSigns.getOxygenSaturation())
            .weight(vitalSigns.getWeight())
            .weightUnit(vitalSigns.getWeightUnit())
            .height(vitalSigns.getHeight())
            .heightUnit(vitalSigns.getHeightUnit())
            .bmi(vitalSigns.getBmi())
            .painScale(vitalSigns.getPainScale())
            .bloodGlucose(vitalSigns.getBloodGlucose())
            .headCircumference(vitalSigns.getHeadCircumference())
            .measuredBy(vitalSigns.getMeasuredBy())
            .measurementLocationId(vitalSigns.getMeasurementLocationId())
            .deviceUsed(vitalSigns.getDeviceUsed())
            .patientPosition(vitalSigns.getPatientPosition())
            .notes(vitalSigns.getNotes())
            .isAbnormal(vitalSigns.getIsAbnormal())
            .isCritical(vitalSigns.getIsCritical())
            .abnormalReason(vitalSigns.getAbnormalReason())
            .createdAt(vitalSigns.getCreatedAt())
            .updatedAt(vitalSigns.getUpdatedAt())
            .createdBy(vitalSigns.getCreatedBy())
            .updatedBy(vitalSigns.getUpdatedBy())
            .build();
    }
    
    private VitalSignsTrendResponse mapTrendToResponse(Object[] trend) {
        return VitalSignsTrendResponse.builder()
            .measurementDate(toLocalDate(trend[1]))
            .avgSystolicBp(toBigDecimal(trend[2]))
            .avgDiastolicBp(toBigDecimal(trend[3]))
            .avgHeartRate(toBigDecimal(trend[4]))
            .avgRespiratoryRate(toBigDecimal(trend[5]))
            .avgTemperature(toBigDecimal(trend[6]))
            .avgOxygenSaturation(toBigDecimal(trend[7]))
            .avgWeight(toBigDecimal(trend[8]))
            .avgBmi(toBigDecimal(trend[9]))
            .measurementCount(trend[10] != null ? ((Number) trend[10]).longValue() : 0L)
            .build();
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.time.LocalDateTime dt) {
            return dt.toLocalDate();
        }
        throw new IllegalArgumentException("Unsupported measurement date type: " + value.getClass().getName());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
