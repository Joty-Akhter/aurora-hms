package com.easyops.hr.service;

import com.easyops.hr.entity.LeaveBalance;
import com.easyops.hr.entity.LeaveRequest;
import com.easyops.hr.entity.LeaveType;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.LeaveBalanceRepository;
import com.easyops.hr.repository.LeaveRequestRepository;
import com.easyops.hr.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeaveService {
    
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveApprovalMatrixService leaveApprovalMatrixService;
    
    // Per-organization locks to prevent concurrent seeding
    private final ConcurrentHashMap<UUID, ReentrantLock> seedingLocks = new ConcurrentHashMap<>();
    
    // Leave Type Methods
    public List<LeaveType> getAllLeaveTypes(UUID organizationId) {
        List<LeaveType> types = leaveTypeRepository.findByOrganizationId(organizationId);
        if (types.isEmpty()) {
            types = seedDefaultLeaveTypesSafely(organizationId);
        }
        return types;
    }
    
    public LeaveType createLeaveType(LeaveType leaveType) {
        return leaveTypeRepository.save(leaveType);
    }

    public LeaveType getLeaveTypeById(UUID leaveTypeId) {
        return leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new RuntimeException("Leave type not found"));
    }
    
    public LeaveType updateLeaveType(UUID leaveTypeId, LeaveType leaveType) {
        LeaveType existing = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new RuntimeException("Leave type not found"));
        
        if (leaveType.getTypeName() != null) existing.setTypeName(leaveType.getTypeName());
        if (leaveType.getDescription() != null) existing.setDescription(leaveType.getDescription());
        if (leaveType.getIsPaid() != null) existing.setIsPaid(leaveType.getIsPaid());
        if (leaveType.getMaxDaysPerYear() != null) existing.setMaxDaysPerYear(leaveType.getMaxDaysPerYear());
        if (leaveType.getRequiresApproval() != null) existing.setRequiresApproval(leaveType.getRequiresApproval());
        if (leaveType.getCarryForward() != null) existing.setCarryForward(leaveType.getCarryForward());
        if (leaveType.getIsActive() != null) existing.setIsActive(leaveType.getIsActive());
        
        return leaveTypeRepository.save(existing);
    }
    
    // Leave Request Methods
    public List<LeaveRequest> getAllLeaveRequests(UUID organizationId) {
        return leaveRequestRepository.findByOrganizationId(organizationId);
    }
    
    public LeaveRequest getLeaveRequestById(UUID leaveRequestId) {
        return leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
    }
    
    public List<LeaveRequest> getEmployeeLeaveRequests(UUID employeeId, UUID organizationId) {
        return leaveRequestRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }
    
    public List<LeaveRequest> getPendingLeaveRequests(UUID organizationId) {
        return leaveRequestRepository.findByOrganizationIdAndStatus(organizationId, "pending");
    }
    
    public LeaveRequest createLeaveRequest(LeaveRequest leaveRequest) {
        log.info("Creating leave request for employee: {}", leaveRequest.getEmployeeId());
        employeeRepository.findById(leaveRequest.getEmployeeId())
                .filter(e -> e.getOrganizationId().equals(leaveRequest.getOrganizationId()))
                .orElseThrow(() -> new IllegalArgumentException("employee_not_found"));

        List<UUID> chain = leaveApprovalMatrixService.resolveApproverChain(
                leaveRequest.getOrganizationId(), leaveRequest.getEmployeeId());
        if (chain.isEmpty()) {
            throw new IllegalArgumentException("leave_approver_chain_empty_configure_matrix_or_manager");
        }

        leaveRequest.setStatus("pending");
        leaveRequest.setPendingStepIndex(1);
        leaveRequest.setVerifiedBy(null);
        leaveRequest.setVerifiedAt(null);
        leaveRequest.setRejectedBy(null);
        leaveRequest.setRejectedAt(null);
        leaveRequest.setApprovedBy(null);
        leaveRequest.setApprovedAt(null);
        leaveRequest.setRejectionReason(null);
        return leaveRequestRepository.save(leaveRequest);
    }

    /**
     * Multi-stage approval: intermediate steps advance {@link LeaveRequest#getPendingStepIndex()} without touching balances.
     * HR manage override completes approval in one step. Balance deducts only on final approval.
     */
    public LeaveRequest approveLeaveRequest(UUID leaveRequestId, UUID approvedBy, boolean hrManageOverride) {
        LeaveRequest request = getLeaveRequestById(leaveRequestId);
        if (request.getStatus() == null || !"pending".equalsIgnoreCase(request.getStatus().trim())) {
            throw new IllegalArgumentException("leave_request_not_pending");
        }

        if (approvedBy.equals(request.getEmployeeId())) {
            throw new IllegalArgumentException("leave_cannot_self_approve");
        }

        List<UUID> chain = leaveApprovalMatrixService.resolveApproverChain(
                request.getOrganizationId(), request.getEmployeeId());

        if (hrManageOverride) {
            request.setStatus("approved");
            request.setApprovedBy(approvedBy);
            request.setApprovedAt(LocalDateTime.now());
            request.setPendingStepIndex(chain.isEmpty() ? 1 : chain.size());
            request.setVerifiedBy(null);
            request.setVerifiedAt(null);
            updateLeaveBalance(request.getEmployeeId(), request.getLeaveTypeId(), request.getTotalDays());
            return leaveRequestRepository.save(request);
        }

        if (chain.isEmpty()) {
            throw new IllegalArgumentException("leave_no_approvers_configured");
        }

        int step = request.getPendingStepIndex() != null ? request.getPendingStepIndex() : 1;
        if (step < 1 || step > chain.size()) {
            throw new IllegalArgumentException("leave_approve_invalid_step");
        }
        if (!approvedBy.equals(chain.get(step - 1))) {
            throw new IllegalArgumentException("leave_wrong_approver_for_step");
        }

        if (step < chain.size()) {
            request.setVerifiedBy(approvedBy);
            request.setVerifiedAt(LocalDateTime.now());
            request.setApprovedBy(null);
            request.setApprovedAt(null);
            request.setPendingStepIndex(step + 1);
            return leaveRequestRepository.save(request);
        }

        request.setStatus("approved");
        request.setApprovedBy(approvedBy);
        request.setApprovedAt(LocalDateTime.now());
        request.setPendingStepIndex(chain.size());
        updateLeaveBalance(request.getEmployeeId(), request.getLeaveTypeId(), request.getTotalDays());
        return leaveRequestRepository.save(request);
    }

    public LeaveRequest rejectLeaveRequest(UUID leaveRequestId, UUID rejectedBy, String rejectionReason) {
        LeaveRequest request = getLeaveRequestById(leaveRequestId);
        if (request.getStatus() == null || !"pending".equalsIgnoreCase(request.getStatus().trim())) {
            throw new IllegalArgumentException("leave_request_not_pending");
        }
        if (rejectedBy.equals(request.getEmployeeId())) {
            throw new IllegalArgumentException("leave_cannot_self_reject_as_approver");
        }
        request.setStatus("rejected");
        request.setRejectedBy(rejectedBy);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectionReason(rejectionReason);
        request.setApprovedBy(null);
        request.setApprovedAt(null);
        request.setVerifiedBy(null);
        request.setVerifiedAt(null);

        return leaveRequestRepository.save(request);
    }

    public List<LeaveRequest> getPendingLeaveRequestsForApprover(UUID organizationId, UUID approverEmployeeId) {
        List<LeaveRequest> pending = leaveRequestRepository.findByOrganizationIdAndStatus(organizationId, "pending");
        List<LeaveRequest> filtered = pending.stream()
                .filter(r -> {
                    List<UUID> chain = leaveApprovalMatrixService.resolveApproverChain(organizationId, r.getEmployeeId());
                    int step = r.getPendingStepIndex() != null ? r.getPendingStepIndex() : 1;
                    if (step < 1 || step > chain.size()) {
                        return false;
                    }
                    return chain.get(step - 1).equals(approverEmployeeId);
                })
                .toList();
        if (filtered.isEmpty()) {
            return filtered;
        }
        Map<UUID, String> names = new HashMap<>();
        employeeRepository.findAllById(
                        filtered.stream().map(LeaveRequest::getEmployeeId).distinct().toList())
                .forEach(e -> names.put(e.getEmployeeId(), e.getName()));
        for (LeaveRequest r : filtered) {
            r.setEmployeeName(names.get(r.getEmployeeId()));
        }
        return filtered;
    }
    
    // Leave Balance Methods
    public List<LeaveBalance> getEmployeeLeaveBalances(UUID employeeId, UUID organizationId) {
        return leaveBalanceRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }
    
    public LeaveBalance getLeaveBalance(UUID employeeId, UUID leaveTypeId, Integer year) {
        return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveTypeId, year)
                .orElse(null);
    }
    
    public LeaveBalance createLeaveBalance(LeaveBalance balance) {
        return leaveBalanceRepository.save(balance);
    }
    
    private void updateLeaveBalance(UUID employeeId, UUID leaveTypeId, BigDecimal days) {
        int currentYear = java.time.Year.now().getValue();
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveTypeId, currentYear)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));
        
        balance.setUsedDays(balance.getUsedDays().add(days));
        leaveBalanceRepository.save(balance);
    }

    /**
     * Safely seed default leave types with proper synchronization to prevent race conditions.
     * Uses per-organization locks to ensure only one thread seeds leave types for a given organization.
     */
    private List<LeaveType> seedDefaultLeaveTypesSafely(UUID organizationId) {
        // Get or create a lock for this organization
        ReentrantLock lock = seedingLocks.computeIfAbsent(organizationId, k -> new ReentrantLock());
        
        lock.lock();
        try {
            // Double-check after acquiring lock - another thread might have seeded while we waited
            List<LeaveType> existing = leaveTypeRepository.findByOrganizationId(organizationId);
            if (!existing.isEmpty()) {
                log.debug("Leave types already seeded for organization {} (found by another thread)", organizationId);
                return existing;
            }
            
            // Attempt to seed
            return seedDefaultLeaveTypes(organizationId);
        } catch (DataIntegrityViolationException e) {
            // Another thread might have inserted between our check and insert
            // This is safe - just return what's in the database now
            log.warn("Duplicate key violation while seeding leave types for organization {} - another thread may have seeded. Retrying query.", organizationId);
            return leaveTypeRepository.findByOrganizationId(organizationId);
        } finally {
            lock.unlock();
            // Clean up lock if no longer needed (optional optimization)
            if (!lock.hasQueuedThreads()) {
                seedingLocks.remove(organizationId);
            }
        }
    }
    
    private List<LeaveType> seedDefaultLeaveTypes(UUID organizationId) {
        log.info("Seeding default leave types for organization {}", organizationId);
        List<LeaveType> defaults = List.of(
                buildLeaveType(organizationId, "Annual Leave", "Paid time off for planned vacations and personal days.", true, 20, true, true),
                buildLeaveType(organizationId, "Sick Leave", "Paid leave for illness or medical appointments.", true, 12, true, false),
                buildLeaveType(organizationId, "Casual Leave", "Short notice leave for personal errands and emergencies.", true, 7, true, false),
                buildLeaveType(organizationId, "Unpaid Leave", "Unpaid leave for special circumstances.", false, 30, true, false)
        );
        return leaveTypeRepository.saveAll(defaults);
    }

    private LeaveType buildLeaveType(UUID organizationId,
                                     String name,
                                     String description,
                                     boolean isPaid,
                                     Integer maxDays,
                                     boolean requiresApproval,
                                     boolean carryForward) {
        LeaveType leaveType = new LeaveType();
        leaveType.setOrganizationId(organizationId);
        leaveType.setTypeName(name);
        leaveType.setDescription(description);
        leaveType.setIsPaid(isPaid);
        leaveType.setMaxDaysPerYear(maxDays);
        leaveType.setRequiresApproval(requiresApproval);
        leaveType.setCarryForward(carryForward);
        leaveType.setIsActive(true);
        leaveType.setCreatedBy("system-bootstrap");
        leaveType.setUpdatedBy("system-bootstrap");
        return leaveType;
    }
}

