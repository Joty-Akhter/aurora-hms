package com.easyops.ap.service;

import com.easyops.ap.dto.VendorRequest;
import com.easyops.ap.entity.Vendor;
import com.easyops.ap.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {
    private final VendorRepository vendorRepository;
    
    @Transactional(readOnly = true)
    public List<Vendor> getAllVendors(UUID organizationId) {
        return vendorRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Vendor> getActiveVendors(UUID organizationId) {
        return vendorRepository.findByOrganizationIdAndIsActive(organizationId, true);
    }
    
    @Transactional(readOnly = true)
    public Vendor getVendorById(UUID id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found with ID: " + id));
    }
    
    @Transactional
    public Vendor createVendor(VendorRequest request) {
        if (vendorRepository.existsByOrganizationIdAndVendorCode(
                request.getOrganizationId(), request.getVendorCode())) {
            throw new RuntimeException("Vendor code already exists: " + request.getVendorCode());
        }
        
        Vendor vendor = new Vendor();
        vendor.setOrganizationId(request.getOrganizationId());
        vendor.setVendorCode(request.getVendorCode());
        vendor.setVendorName(request.getVendorName());
        vendor.setEmail(request.getEmail());
        vendor.setPhone(request.getPhone());
        vendor.setPaymentTerms(request.getPaymentTerms());
        vendor.setTaxId(request.getTaxId());
        vendor.setIsActive(request.getIsActive());
        
        return vendorRepository.save(vendor);
    }
    
    @Transactional
    public Vendor updateVendor(UUID id, VendorRequest request) {
        Vendor vendor = getVendorById(id);
        vendor.setVendorName(request.getVendorName());
        vendor.setEmail(request.getEmail());
        vendor.setPhone(request.getPhone());
        vendor.setPaymentTerms(request.getPaymentTerms());
        vendor.setTaxId(request.getTaxId());
        vendor.setIsActive(request.getIsActive());
        return vendorRepository.save(vendor);
    }
    
    @Transactional
    public void deleteVendor(UUID id) {
        vendorRepository.deleteById(id);
    }
}

