package com.easyops.ap.client;

import java.util.Map;
import java.util.UUID;

public interface JournalEntryPoster {
    UUID createAndPostJournal(Map<String, Object> journalEntry, UUID actorUserId);
}
