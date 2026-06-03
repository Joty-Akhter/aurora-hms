package com.easyops.hospitalscheduling.events;

import com.easyops.hospitalscheduling.api.dto.PromoteWaitlistRequest;
import com.easyops.hospitalscheduling.domain.waitlist.WaitlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for appointment cancellation and optionally auto-fills the freed slot from the waitlist
 * (Phase 5: "auto-fill from waitlist when slot freed").
 * Runs after the cancel transaction is committed so the slot is actually freed before promote.
 * Enable with hospital-scheduling.waitlist.auto-fill-on-cancel=true.
 */
@Component
public class WaitlistAutoFillOnCancelListener {

    private static final Logger log = LoggerFactory.getLogger(WaitlistAutoFillOnCancelListener.class);

    private final WaitlistService waitlistService;

    @Value("${hospital-scheduling.waitlist.auto-fill-on-cancel:false}")
    private boolean autoFillOnCancel;

    public WaitlistAutoFillOnCancelListener(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Order(0)
    public void onAppointmentCancelled(AppointmentEvent event) {
        if (event.getType() != AppointmentEvent.Type.CANCELLED || !autoFillOnCancel) {
            return;
        }
        if (event.getResourceId() == null || event.getSlotStart() == null || event.getSlotEnd() == null) {
            log.debug("Skip waitlist auto-fill: cancelled event missing resource or slot");
            return;
        }
        PromoteWaitlistRequest request = new PromoteWaitlistRequest();
        request.setResourceId(event.getResourceId());
        request.setSlotStart(event.getSlotStart());
        request.setSlotEnd(event.getSlotEnd());
        request.setMaxCandidates(1);
        try {
            var response = waitlistService.promote(request);
            int contacted = response.getCandidatesContacted() != null ? response.getCandidatesContacted() : 0;
            if (contacted > 0) {
                log.info("Waitlist auto-fill: promoted {} candidate(s) for resource {} slot {}-{}",
                        contacted, event.getResourceId(), event.getSlotStart(), event.getSlotEnd());
            }
        } catch (Exception e) {
            log.warn("Waitlist auto-fill failed for resource {} slot {}-{}: {}",
                    event.getResourceId(), event.getSlotStart(), event.getSlotEnd(), e.getMessage());
        }
    }
}
