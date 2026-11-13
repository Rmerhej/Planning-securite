package com.plansecu.config;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.NoSuchElementException;

/** Attributs communs à toutes les vues + page 404 pour les entités introuvables. */
@ControllerAdvice
@RequiredArgsConstructor
public class WebAdvice {

    private final AppProperties props;

    @ModelAttribute("uri")
    public String uri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("appNom")
    public String appNom() {
        return props.getNom();
    }

    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String introuvable(Exception ex, Model model) {
        model.addAttribute("message", "L'élément demandé n'existe pas ou a été supprimé.");
        return "error";
    }
}
