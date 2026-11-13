package com.plansecu.service;

import com.plansecu.domain.Mission;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Export Excel du récapitulatif mensuel (feuille Récap + feuille Détail des missions). */
@Service
public class ExportExcelService {

    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    public byte[] recapMensuel(YearMonth mois, List<RecapAgent> recap, List<Mission> missions) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle entete = wb.createCellStyle();
            Font gras = wb.createFont();
            gras.setBold(true);
            gras.setColor(IndexedColors.WHITE.getIndex());
            entete.setFont(gras);
            entete.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            entete.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle heures = wb.createCellStyle();
            heures.setDataFormat(wb.createDataFormat().getFormat("0.00"));

            CellStyle date = wb.createCellStyle();
            date.setDataFormat(wb.createDataFormat().getFormat("dd/mm/yyyy"));

            // --- Récap
            Sheet s = wb.createSheet("Récap " + mois);
            Row titre = s.createRow(0);
            titre.createCell(0).setCellValue("Récapitulatif " + MOIS.format(mois));
            String[] cols = {"Agent", "Téléphone", "Jours travaillés", "Missions", "Heures effectuées",
                    "dont nuit", "dont dimanche", "dont férié", "Heures restant planifiées", "Jours d'absence", "Détail absences", "Heures pointées", "Missions non pointées"};
            Row h = s.createRow(2);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(entete);
            }
            int r = 3;
            for (RecapAgent ra : recap) {
                Row row = s.createRow(r++);
                row.createCell(0).setCellValue(ra.agent().getNomComplet());
                row.createCell(1).setCellValue(ra.agent().getTelephoneFormate());
                row.createCell(2).setCellValue(ra.joursTravailles());
                row.createCell(3).setCellValue(ra.missionsEffectuees());
                cellHeures(row, 4, ra.minutesEffectuees(), heures);
                cellHeures(row, 5, ra.minutesNuit(), heures);
                cellHeures(row, 6, ra.minutesDimanche(), heures);
                cellHeures(row, 7, ra.minutesFerie(), heures);
                cellHeures(row, 8, ra.minutesPlanifiees(), heures);
                row.createCell(9).setCellValue(ra.joursAbsence());
                row.createCell(10).setCellValue(ra.getDetailAbsences());
                cellHeures(row, 11, ra.minutesPointees(), heures);
                row.createCell(12).setCellValue(ra.missionsNonPointees());
            }
            if (!recap.isEmpty()) {
                Row tot = s.createRow(r);
                tot.createCell(0).setCellValue("TOTAL");
                for (int col = 2; col <= 12; col++) {
                    if (col == 10) continue; // détail des absences : texte
                    String lettre = colonne(col);
                    Cell c = tot.createCell(col);
                    c.setCellFormula("SUM(" + lettre + "4:" + lettre + r + ")");
                    if ((col >= 4 && col <= 8) || col == 11) c.setCellStyle(heures);
                }
            }
            for (int i = 0; i < cols.length; i++) s.autoSizeColumn(i);
            s.createFreezePane(1, 3);

            // --- Détail
            Sheet d = wb.createSheet("Détail missions");
            String[] cd = {"Date", "Agent", "Téléphone", "Lieu", "Adresse", "Horaires", "Durée (h)", "Statut"};
            Row hd = d.createRow(0);
            for (int i = 0; i < cd.length; i++) {
                Cell c = hd.createCell(i);
                c.setCellValue(cd[i]);
                c.setCellStyle(entete);
            }
            int rd = 1;
            for (Mission m : missions) {
                Row row = d.createRow(rd++);
                Cell cdate = row.createCell(0);
                cdate.setCellValue(m.getDateMission());
                cdate.setCellStyle(date);
                row.createCell(1).setCellValue(m.getAgent().getNomComplet());
                row.createCell(2).setCellValue(m.getAgent().getTelephoneFormate());
                row.createCell(3).setCellValue(m.getSite().getNom());
                row.createCell(4).setCellValue(m.getSite().getAdresseComplete());
                row.createCell(5).setCellValue(m.getHorairesLibelle());
                cellHeures(row, 6, m.getDureeMinutes(), heures);
                row.createCell(7).setCellValue(m.getStatut().getLibelle());
            }
            for (int i = 0; i < cd.length; i++) d.autoSizeColumn(i);
            d.createFreezePane(0, 1);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void cellHeures(Row row, int col, long minutes, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(minutes / 60.0);
        c.setCellStyle(style);
    }

    private static String colonne(int index) {
        return org.apache.poi.ss.util.CellReference.convertNumToColString(index);
    }
}
