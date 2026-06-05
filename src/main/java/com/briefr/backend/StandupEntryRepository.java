package com.briefr.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface StandupEntryRepository extends JpaRepository<StandupEntry, Long> {
    List<StandupEntry> findByStandupDate(LocalDate date);
}