package com.plansecu.web.admin;

import com.plansecu.service.ExportExcelService;
import com.plansecu.service.RecapAgent;
import com.plansecu.service.RecapService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/admin/recap")
@RequiredArgsConstructor
public class RecapController {

    private final RecapService recapService;
    private final ExportExcelService exportExcelService;
    private final Clock clock;

    @GetMapping
    public String recap(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois, Model model) {
        YearMonth m = mois != null ? mois : YearMonth.now(clock);
        List<RecapAgent> recap = recapService.recapMensuel(m);
        model.addAttribute("mois", m);
        model.addAttribute("moisPrecedent", m.minusMonths(1));
        model.addAttribute("moisSuivant", m.plusMonths(1));
        model.addAttribute("recap", recap);
        model.addAttribute("totalEffectuees", recap.stream().mapToLong(RecapAgent::minutesEffectuees).sum());
        model.addAttribute("totalPointees", recap.stream().mapToLong(RecapAgent::minutesPointees).sum());
        model.addAttribute("totalNuit", recap.stream().mapToLong(RecapAgent::minutesNuit).sum());
        model.addAttribute("totalDimanche", recap.stream().mapToLong(RecapAgent::minutesDimanche).sum());
        model.addAttribute("totalFerie", recap.stream().mapToLong(RecapAgent::minutesFerie).sum());
        model.addAttribute("totalPlanifiees", recap.stream().mapToLong(RecapAgent::minutesPlanifiees).sum());
        model.addAttribute("totalAbsences", recap.stream().mapToInt(RecapAgent::joursAbsence).sum());
        model.addAttribute("totalJours", recap.stream().mapToInt(RecapAgent::joursTravailles).sum());
        return "admin/recap";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois) throws IOException {
        byte[] xlsx = exportExcelService.recapMensuel(mois, recapService.recapMensuel(mois), recapService.missionsDuMois(mois));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recap-" + mois + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}
