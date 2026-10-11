package com.deb.repository;

import com.deb.entity.DailyLinkStats;
import com.deb.entity.DailyLinkStatsId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLinkStatsRepository extends JpaRepository<DailyLinkStats, DailyLinkStatsId> {
    Optional<DailyLinkStats> findByIdLinkIdAndIdStatDate(Long linkId, LocalDate statDate);
    List<DailyLinkStats> findByIdLinkIdOrderByIdStatDateDesc(Long linkId);
}
