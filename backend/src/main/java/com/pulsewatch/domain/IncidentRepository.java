package com.pulsewatch.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findFirstByMonitorIdAndResolvedAtIsNull(Long monitorId);

    List<Incident> findByResolvedAtIsNullOrderByStartedAtDesc();

    List<Incident> findAllByOrderByStartedAtDesc(Pageable pageable);
}
