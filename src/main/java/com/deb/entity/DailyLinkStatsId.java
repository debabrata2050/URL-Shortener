package com.deb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DailyLinkStatsId implements Serializable {

    @Column(name = "link_id")
    private Long linkId;

    @Column(name = "stat_date")
    private LocalDate statDate;
}
