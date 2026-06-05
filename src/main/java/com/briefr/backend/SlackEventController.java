package com.briefr.backend;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackEventController {

    private final StandupEntryRepository repo;

    @PostMapping("/events")
    public ResponseEntity<?> handleEvent(@RequestBody Map<String, Object> payload) {

        // Slack URL verification challenge
        if (payload.containsKey("challenge")) {
            return ResponseEntity.ok(Map.of("challenge", payload.get("challenge")));
        }

        // Handle message events
        if ("event_callback".equals(payload.get("type"))) {
            Map<String, Object> event = (Map<String, Object>) payload.get("event");
            String eventType = (String) event.get("type");

            if ("message".equals(eventType) && !event.containsKey("bot_id")) {
                String userId = (String) event.get("user");
                String text = (String) event.get("text");

                StandupEntry entry = new StandupEntry();
                entry.setUserId(userId);
                entry.setUserName(userId); // we'll resolve names later
                entry.setMessage(text);
                entry.setStandupDate(LocalDate.now());
                entry.setPostedAt(LocalDateTime.now());
                repo.save(entry);
            }
        }

        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}