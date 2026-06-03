package com.easyops.crm.service;

import com.easyops.crm.entity.Event;
import com.easyops.crm.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EventService {
    
    @Autowired
    private EventRepository eventRepository;
    
    public List<Event> getAllEvents(UUID organizationId) {
        return eventRepository.findByOrganizationIdOrderByStartDatetimeAsc(organizationId);
    }
    
    public Optional<Event> getEventById(UUID eventId) {
        return eventRepository.findById(eventId);
    }

    public UUID getOrganizationIdForEvent(UUID eventId) {
        return getEventById(eventId)
                .map(Event::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
    }
    
    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }
    
    public Event updateEvent(UUID eventId, Event eventDetails) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    event.setSubject(eventDetails.getSubject());
                    event.setDescription(eventDetails.getDescription());
                    event.setEventType(eventDetails.getEventType());
                    event.setStartDatetime(eventDetails.getStartDatetime());
                    event.setEndDatetime(eventDetails.getEndDatetime());
                    event.setAllDay(eventDetails.getAllDay());
                    event.setTimezone(eventDetails.getTimezone());
                    event.setLocation(eventDetails.getLocation());
                    event.setMeetingUrl(eventDetails.getMeetingUrl());
                    event.setStatus(eventDetails.getStatus());
                    event.setOrganizerId(eventDetails.getOrganizerId());
                    event.setAttendees(eventDetails.getAttendees());
                    event.setLeadId(eventDetails.getLeadId());
                    event.setContactId(eventDetails.getContactId());
                    event.setAccountId(eventDetails.getAccountId());
                    event.setOpportunityId(eventDetails.getOpportunityId());
                    event.setCampaignId(eventDetails.getCampaignId());
                    event.setOutcome(eventDetails.getOutcome());
                    event.setOutcomeNotes(eventDetails.getOutcomeNotes());
                    event.setReminderMinutes(eventDetails.getReminderMinutes());
                    event.setReminderSent(eventDetails.getReminderSent());
                    event.setIsRecurring(eventDetails.getIsRecurring());
                    event.setRecurrencePattern(eventDetails.getRecurrencePattern());
                    event.setRecurrenceEndDate(eventDetails.getRecurrenceEndDate());
                    event.setUpdatedBy(eventDetails.getUpdatedBy());
                    return eventRepository.save(event);
                })
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
    }
    
    public void deleteEvent(UUID eventId) {
        eventRepository.deleteById(eventId);
    }
    
    public List<Event> getEventsByOrganizer(UUID organizationId, UUID organizerId) {
        return eventRepository.findByOrganizationIdAndOrganizerId(organizationId, organizerId);
    }
    
    public List<Event> getEventsByStatus(UUID organizationId, String status) {
        return eventRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    public List<Event> getEventsBetweenDates(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate) {
        return eventRepository.findEventsBetweenDates(organizationId, startDate, endDate);
    }
    
    public List<Event> getEventsForUserBetweenDates(UUID organizationId, UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return eventRepository.findEventsForUserBetweenDates(organizationId, userId, startDate, endDate);
    }
    
    public Event completeEvent(UUID eventId, String outcome, String outcomeNotes) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    event.setStatus("COMPLETED");
                    event.setOutcome(outcome);
                    event.setOutcomeNotes(outcomeNotes);
                    return eventRepository.save(event);
                })
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
    }
}

