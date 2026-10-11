package com.deb.repository;

import com.deb.dto.projection.CountryStats;
import com.deb.dto.projection.DeviceStats;
import com.deb.dto.projection.ReferrerStats;
import com.deb.entity.ClickEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortLinkId(Long linkId);

    long countByShortLinkIdAndIsBotTrue(Long linkId);

    @Query("SELECT COUNT(DISTINCT c.ipAddress) FROM ClickEvent c WHERE c.shortLink.id = :linkId")
    long countDistinctIpByShortLinkId(@Param("linkId") Long linkId);

    @Query("SELECT c.referrerDomain AS referrerDomain, COUNT(c) AS totalClicks " +
           "FROM ClickEvent c " +
           "WHERE c.shortLink.id = :linkId AND c.referrerDomain IS NOT NULL AND c.referrerDomain <> '' " +
           "GROUP BY c.referrerDomain " +
           "ORDER BY COUNT(c) DESC")
    List<ReferrerStats> findTopReferrers(@Param("linkId") Long linkId, Pageable pageable);

    @Query("SELECT c.countryCode AS countryCode, COUNT(c) AS totalClicks " +
           "FROM ClickEvent c " +
           "WHERE c.shortLink.id = :linkId AND c.countryCode IS NOT NULL AND c.countryCode <> '' " +
           "GROUP BY c.countryCode " +
           "ORDER BY COUNT(c) DESC")
    List<CountryStats> findTopCountries(@Param("linkId") Long linkId, Pageable pageable);

    @Query("SELECT c.deviceType AS deviceType, COUNT(c) AS totalClicks " +
           "FROM ClickEvent c " +
           "WHERE c.shortLink.id = :linkId AND c.deviceType IS NOT NULL " +
           "GROUP BY c.deviceType " +
           "ORDER BY COUNT(c) DESC")
    List<DeviceStats> findDeviceBreakdown(@Param("linkId") Long linkId);

    @Query("SELECT c FROM ClickEvent c WHERE c.shortLink.id = :linkId AND c.clickedAt >= :since ORDER BY c.clickedAt ASC")
    List<ClickEvent> findRecentClicks(@Param("linkId") Long linkId, @Param("since") LocalDateTime since);
}
