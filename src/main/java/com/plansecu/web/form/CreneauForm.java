package com.plansecu.web.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreneauForm {

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime heureDebut;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime heureFin;

    public boolean estRempli() {
        return heureDebut != null && heureFin != null;
    }
}
