package com.easyops.hospitalscheduling.domain.appointment;

import com.easyops.hospitalscheduling.api.dto.QueueUpdateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class QueueEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(QueueEventBroadcaster.class);
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String key(UUID resourceId, LocalDate date) {
        return resourceId + ":" + date;
    }

    public SseEmitter subscribe(UUID resourceId, LocalDate date) {
        SseEmitter emitter = new SseEmitter(30L * 60 * 1000); // 30-minute timeout
        String k = key(resourceId, date);
        emitters.computeIfAbsent(k, x -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(k, emitter));
        emitter.onTimeout(() -> removeEmitter(k, emitter));
        emitter.onError(e -> removeEmitter(k, emitter));
        return emitter;
    }

    public void broadcast(UUID resourceId, LocalDate date, QueueUpdateEvent event) {
        String k = key(resourceId, date);
        List<SseEmitter> targets = emitters.getOrDefault(k, new CopyOnWriteArrayList<>());
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : targets) {
            try {
                String json = mapper.writeValueAsString(event);
                emitter.send(SseEmitter.event()
                        .id(event.getEventId())
                        .name(event.getEventType())
                        .data(json));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize QueueUpdateEvent", e);
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        targets.removeAll(dead);
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(key);
        if (list != null) list.remove(emitter);
    }
}
