package com.plansecu.repository;

import com.plansecu.domain.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {

    List<Site> findAllByOrderByNomAsc();

    List<Site> findByActifTrueOrderByNomAsc();

    Optional<Site> findByQrToken(String qrToken);

    List<Site> findByQrTokenIsNull();
}
