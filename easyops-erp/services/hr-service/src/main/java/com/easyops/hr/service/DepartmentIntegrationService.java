package com.easyops.hr.service;

import com.easyops.hr.dto.DepartmentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${services.organization.base-url:lb://organization-service}")
    private String organizationServiceBaseUrl;

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String HEADER_USER_ID = "X-User-Id";

    public List<DepartmentDto> getDepartments(UUID actorUserId, UUID organizationId, Boolean activeOnly, UUID parentDepartmentId) {
        DepartmentResponse[] payload = fetchDepartmentsFromOrganizationService(actorUserId, organizationId);
        List<DepartmentDto> departments = Arrays.stream(payload == null ? new DepartmentResponse[0] : payload)
                .map(this::mapToDepartmentDto)
                .filter(dto -> filterByActive(dto, activeOnly))
                .filter(dto -> filterByParent(dto, parentDepartmentId))
                .collect(Collectors.toList());
        return departments;
    }

    public DepartmentDto getDepartmentById(UUID actorUserId, UUID organizationId, UUID departmentId) {
        String url = organizationServiceBaseUrl + "/api/organizations/{organizationId}/departments/{departmentId}";
        HttpEntity<Void> entity = new HttpEntity<>(organizationHeaders(actorUserId));
        ResponseEntity<DepartmentResponse> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                DepartmentResponse.class,
                organizationId,
                departmentId
        );
        DepartmentResponse response = responseEntity.getBody();
        if (response == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Department not found");
        }
        return mapToDepartmentDto(response);
    }

    public DepartmentDto createDepartment(UUID actorUserId, UUID organizationId, DepartmentDto departmentDto) {
        DepartmentRequest request = buildRequestForCreate(departmentDto);
        String url = organizationServiceBaseUrl + "/api/organizations/{organizationId}/departments";
        HttpEntity<DepartmentRequest> entity = new HttpEntity<>(request, organizationHeaders(actorUserId));
        ResponseEntity<DepartmentResponse> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                DepartmentResponse.class,
                organizationId
        );
        DepartmentResponse response = responseEntity.getBody();
        return mapToDepartmentDto(response);
    }

    @SuppressWarnings("null")
    public DepartmentDto updateDepartment(UUID actorUserId, UUID organizationId, UUID departmentId, DepartmentDto departmentDto) {
        DepartmentDto existing = getDepartmentById(actorUserId, organizationId, departmentId);
        DepartmentRequest request = buildRequestForUpdate(existing, departmentDto);
        String url = organizationServiceBaseUrl + "/api/organizations/{organizationId}/departments/{departmentId}";
        HttpHeaders headers = organizationHeaders(actorUserId);
        HttpEntity<DepartmentRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<DepartmentResponse> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                DepartmentResponse.class,
                organizationId,
                departmentId
        );
        return mapToDepartmentDto(Objects.requireNonNull(response.getBody()));
    }

    public void deleteDepartment(UUID actorUserId, UUID organizationId, UUID departmentId) {
        String url = organizationServiceBaseUrl + "/api/organizations/{organizationId}/departments/{departmentId}";
        HttpEntity<Void> entity = new HttpEntity<>(organizationHeaders(actorUserId));
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class, organizationId, departmentId);
    }

    private DepartmentResponse[] fetchDepartmentsFromOrganizationService(UUID actorUserId, UUID organizationId) {
        String url = organizationServiceBaseUrl + "/api/organizations/{organizationId}/departments";
        HttpEntity<Void> entity = new HttpEntity<>(organizationHeaders(actorUserId));
        ResponseEntity<DepartmentResponse[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                DepartmentResponse[].class,
                organizationId
        );
        return response.getBody();
    }

    private HttpHeaders organizationHeaders(UUID actorUserId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USER_ID, actorUserId.toString());
        return headers;
    }

    private DepartmentDto mapToDepartmentDto(DepartmentResponse response) {
        DepartmentDto dto = new DepartmentDto();
        dto.setDepartmentId(response.getId());
        dto.setOrganizationId(response.getOrganizationId());
        dto.setParentDepartmentId(response.getParentDepartmentId());
        dto.setManagerId(response.getManagerId());
        dto.setName(response.getName());
        dto.setDescription(response.getDescription());
        dto.setCode(response.getCode());
        dto.setStatus(response.getStatus());
        dto.setIsActive(isActive(response.getStatus()));
        dto.setCreatedAt(response.getCreatedAt());
        dto.setUpdatedAt(response.getUpdatedAt());
        return dto;
    }

    private boolean filterByActive(DepartmentDto dto, Boolean activeOnly) {
        if (activeOnly == null || !activeOnly) {
            return true;
        }
        return Boolean.TRUE.equals(dto.getIsActive());
    }

    private boolean filterByParent(DepartmentDto dto, UUID parentDepartmentId) {
        if (parentDepartmentId == null) {
            return true;
        }
        return Objects.equals(parentDepartmentId, dto.getParentDepartmentId());
    }

    private boolean isActive(String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        return ACTIVE_STATUS.equalsIgnoreCase(status.trim());
    }

    private DepartmentRequest buildRequestForCreate(DepartmentDto dto) {
        DepartmentRequest request = new DepartmentRequest();
        request.setName(dto.getName());
        request.setDescription(dto.getDescription());
        request.setManagerId(dto.getManagerId());
        request.setParentDepartmentId(dto.getParentDepartmentId());
        request.setCode(resolveDepartmentCode(dto.getCode(), dto.getName()));
        String status = dto.getStatus();
        if (status == null && dto.getIsActive() != null) {
            status = Boolean.TRUE.equals(dto.getIsActive()) ? ACTIVE_STATUS : "INACTIVE";
        }
        request.setStatus(status != null ? status : ACTIVE_STATUS);
        return request;
    }

    private DepartmentRequest buildRequestForUpdate(DepartmentDto existing, DepartmentDto incoming) {
        DepartmentRequest request = new DepartmentRequest();
        request.setName(firstNonNull(incoming.getName(), existing.getName()));
        request.setDescription(firstNonNull(incoming.getDescription(), existing.getDescription()));
        request.setManagerId(firstNonNull(incoming.getManagerId(), existing.getManagerId()));
        request.setParentDepartmentId(firstNonNull(incoming.getParentDepartmentId(), existing.getParentDepartmentId()));
        request.setCode(resolveDepartmentCode(firstNonNull(incoming.getCode(), existing.getCode()), request.getName()));
        if (incoming.getIsActive() != null) {
            request.setStatus(Boolean.TRUE.equals(incoming.getIsActive()) ? ACTIVE_STATUS : "INACTIVE");
        } else if (StringUtils.hasText(incoming.getStatus())) {
            request.setStatus(incoming.getStatus());
        } else {
            request.setStatus(existing.getStatus());
        }
        return request;
    }

    private String resolveDepartmentCode(String providedCode, String name) {
        if (StringUtils.hasText(providedCode)) {
            return providedCode.trim();
        }
        if (!StringUtils.hasText(name)) {
            return "DEPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "-")
                .replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("(^-)|(-$)", "");
        if (normalized.length() < 3) {
            normalized = normalized + "-" + UUID.randomUUID().toString().substring(0, 3).toUpperCase(Locale.ROOT);
        }
        return normalized.substring(0, Math.min(50, normalized.length()));
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    @SuppressWarnings("unused")
    private static class DepartmentRequest {
        private UUID parentDepartmentId;
        private String code;
        private String name;
        private String description;
        private UUID managerId;
        private String status;

        public UUID getParentDepartmentId() {
            return parentDepartmentId;
        }

        public void setParentDepartmentId(UUID parentDepartmentId) {
            this.parentDepartmentId = parentDepartmentId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public UUID getManagerId() {
            return managerId;
        }

        public void setManagerId(UUID managerId) {
            this.managerId = managerId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @SuppressWarnings("unused")
    private static class DepartmentResponse {
        private UUID id;
        private UUID organizationId;
        private UUID parentDepartmentId;
        private UUID managerId;
        private String code;
        private String name;
        private String description;
        private String status;
        private java.time.OffsetDateTime createdAt;
        private java.time.OffsetDateTime updatedAt;

        public UUID getId() {
            return id;
        }

        public UUID getOrganizationId() {
            return organizationId;
        }

        public UUID getParentDepartmentId() {
            return parentDepartmentId;
        }

        public UUID getManagerId() {
            return managerId;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }

        public java.time.OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public java.time.OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
}

