package com.pulsewatch.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    List<Monitor> findByPausedFalse();

    List<Monitor> findAllByOrderByNameAsc();
}
