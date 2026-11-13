package com.plansecu.web.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MotDePasseForm {

    @NotBlank(message = "Saisissez votre mot de passe actuel")
    private String actuel;

    @NotBlank(message = "Saisissez un nouveau mot de passe")
    @Size(min = 8, max = 100, message = "8 caractères minimum")
    private String nouveau;

    @NotBlank(message = "Confirmez le nouveau mot de passe")
    private String confirmation;

    @AssertTrue(message = "Les deux mots de passe ne correspondent pas")
    public boolean isConfirmationValide() {
        return nouveau == null || nouveau.equals(confirmation);
    }
}
