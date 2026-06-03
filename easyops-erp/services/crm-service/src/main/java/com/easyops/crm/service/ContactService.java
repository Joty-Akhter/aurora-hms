package com.easyops.crm.service;

import com.easyops.crm.entity.Contact;
import com.easyops.crm.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContactService {
    
    private final ContactRepository contactRepository;
    
    public List<Contact> getAllContacts(UUID organizationId) {
        return contactRepository.findByOrganizationIdOrderByLastNameAsc(organizationId);
    }
    
    public Contact getContactById(UUID contactId) {
        return contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
    }

    public UUID getOrganizationIdForContact(UUID contactId) {
        return getContactById(contactId).getOrganizationId();
    }
    
    public List<Contact> getContactsByAccount(UUID accountId) {
        return contactRepository.findByAccountId(accountId);
    }
    
    public List<Contact> searchContacts(UUID organizationId, String searchTerm) {
        return contactRepository.searchContacts(organizationId, searchTerm);
    }
    
    public Contact createContact(Contact contact) {
        log.info("Creating contact: {} {}", contact.getFirstName(), contact.getLastName());
        return contactRepository.save(contact);
    }
    
    public Contact updateContact(UUID contactId, Contact contactUpdate) {
        Contact existing = getContactById(contactId);
        
        if (contactUpdate.getFirstName() != null) existing.setFirstName(contactUpdate.getFirstName());
        if (contactUpdate.getLastName() != null) existing.setLastName(contactUpdate.getLastName());
        if (contactUpdate.getEmail() != null) existing.setEmail(contactUpdate.getEmail());
        if (contactUpdate.getPhone() != null) existing.setPhone(contactUpdate.getPhone());
        if (contactUpdate.getMobile() != null) existing.setMobile(contactUpdate.getMobile());
        if (contactUpdate.getJobTitle() != null) existing.setJobTitle(contactUpdate.getJobTitle());
        if (contactUpdate.getDepartment() != null) existing.setDepartment(contactUpdate.getDepartment());
        if (contactUpdate.getAccountId() != null) existing.setAccountId(contactUpdate.getAccountId());
        if (contactUpdate.getIsPrimary() != null) existing.setIsPrimary(contactUpdate.getIsPrimary());
        if (contactUpdate.getNotes() != null) existing.setNotes(contactUpdate.getNotes());
        
        return contactRepository.save(existing);
    }
    
    public void deleteContact(UUID contactId) {
        contactRepository.deleteById(contactId);
    }
}







