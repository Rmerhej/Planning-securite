package com.plansecu.config;

import com.plansecu.domain.Site;
import com.plansecu.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Donne un QR code de pointage aux lieux créés avant l'ajout du pointage. */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class QrTokenInitializer implements ApplicationRunner {

    private final SiteRepository siteRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Site> sansQr = siteRepository.findByQrTokenIsNull();
        sansQr.forEach(Site::regenererQr);
        if (!sansQr.isEmpty()) log.info("QR code de pointage généré pour {} lieu(x)", sansQr.size());
    }
}
