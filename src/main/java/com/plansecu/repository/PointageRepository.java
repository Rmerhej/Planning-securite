package com.plansecu.repository;

import com.plansecu.domain.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PointageRepository extends JpaRepository<Pointage, Long> {

    List<Pointage> findByMissionIdOrderByHorodatageAsc(Long missionId);

    @Query("select p from Pointage p where p.mission.id in :ids order by p.horodatage")
    List<Pointage> deMissions(Collection<Long> ids);

    @Query("select p from Pointage p where p.mission.dateMission between :debut and :fin order by p.horodatage")
    List<Pointage> entre(LocalDate debut, LocalDate fin);
}
