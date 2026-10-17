package com.example.offers.repository;

import com.example.offers.entity.OutboxEntry;
import com.example.offers.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEntry, UUID> {

    @Query("SELECT o FROM OutboxEntry o " +
            "WHERE o.status IN :statuses " +
            "AND o.nextAttemptAt <= :now " +
            "ORDER BY o.createdAt ASC")
    List<OutboxEntry> findReadyForProcessing(@Param("statuses") List<OutboxStatus> statuses,
                                             @Param("now") Instant now,
                                             Pageable pageable);
}