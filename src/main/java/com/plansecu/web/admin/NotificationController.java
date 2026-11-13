package com.plansecu.web.admin;

import com.plansecu.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("notifications", notificationRepository.findTop200ByOrderByEnvoyeLeDesc());
        return "admin/notifications";
    }
}
