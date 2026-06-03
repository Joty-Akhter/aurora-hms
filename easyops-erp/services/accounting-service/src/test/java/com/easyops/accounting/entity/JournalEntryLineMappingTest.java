package com.easyops.accounting.entity;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class JournalEntryLineMappingTest {

    @Test
    void journalLine_mapsToJournalEntryViaManyToOne() throws NoSuchFieldException {
        Field field = JournalLine.class.getDeclaredField("journalEntry");
        ManyToOne mapping = field.getAnnotation(ManyToOne.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.optional()).isFalse();
        assertThat(field.getType()).isEqualTo(JournalEntry.class);
    }

    @Test
    void journalEntry_linesCollectionMappedByJournalEntry() throws NoSuchFieldException {
        Field field = JournalEntry.class.getDeclaredField("lines");
        OneToMany mapping = field.getAnnotation(OneToMany.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.mappedBy()).isEqualTo("journalEntry");
        assertThat(Arrays.asList(mapping.cascade()))
                .containsExactlyInAnyOrder(
                        jakarta.persistence.CascadeType.ALL);
    }

    @Test
    void journalLine_exposesJournalEntryIdAccessor() {
        JournalEntry entry = new JournalEntry();
        entry.setId(java.util.UUID.randomUUID());

        JournalLine line = new JournalLine();
        line.setJournalEntry(entry);

        assertThat(line.getJournalEntryId()).isEqualTo(entry.getId());
    }
}
