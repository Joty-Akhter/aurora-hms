package com.easyops.crm.controller;

import com.easyops.crm.entity.Contact;
import com.easyops.crm.security.CrmRbacService;
import com.easyops.crm.security.RbacRequestHeaders;
import com.easyops.crm.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ContactController {

    private final ContactService contactService;
    private final CrmRbacService crmRbac;

    @GetMapping
    public ResponseEntity<List<Contact>> getAllContacts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) String search) {

        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);

        log.info("GET /api/crm/contacts - organizationId: {}", organizationId);

        List<Contact> contacts;

        if (search != null && !search.isEmpty()) {
            contacts = contactService.searchContacts(organizationId, search);
        } else if (accountId != null) {
            contacts = contactService.getContactsByAccount(accountId);
        } else {
            contacts = contactService.getAllContacts(organizationId);
        }

        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contact> getContactById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        log.info("GET /api/crm/contacts/{}", id);
        Contact contact = contactService.getContactById(id);
        crmRbac.requireCrmView(actor, contact.getOrganizationId());
        return ResponseEntity.ok(contact);
    }

    @PostMapping
    public ResponseEntity<Contact> createContact(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Contact contact) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, contact.getOrganizationId());
        log.info("POST /api/crm/contacts - Creating contact");
        Contact created = contactService.createContact(contact);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contact> updateContact(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Contact contact) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, contactService.getOrganizationIdForContact(id));
        log.info("PUT /api/crm/contacts/{}", id);
        Contact updated = contactService.updateContact(id, contact);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, contactService.getOrganizationIdForContact(id));
        log.info("DELETE /api/crm/contacts/{}", id);
        contactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}
