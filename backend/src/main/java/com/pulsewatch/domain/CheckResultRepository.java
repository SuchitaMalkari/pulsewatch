package com.pulsewatch.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    List<CheckResult> findByMonitorIdOrderByCheckedAtDesc(Long monitorId, Pageable pageable);

    List<CheckResult> findByMonitorIdAndCheckedAtAfter(Long monitorId, Instant since);

    @Transactional
    long deleteByCheckedAtBefore(Instant cutoff);
}
