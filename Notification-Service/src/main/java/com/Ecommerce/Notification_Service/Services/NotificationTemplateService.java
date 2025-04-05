package com.Ecommerce.Notification_Service.Services;


import com.Ecommerce.Notification_Service.Models.NotificationTemplate;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Repositories.NotificationTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationTemplateService {
    @Autowired
    private NotificationTemplateRepository templateRepository;

    public List<NotificationTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    public NotificationTemplate getTemplateById(String id) {
        return templateRepository.findById(id).orElse(null);
    }

    public NotificationTemplate getTemplateByType(NotificationType type) {
        Optional<NotificationTemplate> template = templateRepository.findByType(type);
        return template.orElse(null);
    }

    public NotificationTemplate createTemplate(NotificationType type, String subject, String body, List<String> variables) {
        NotificationTemplate template = new NotificationTemplate(type, subject, body, variables);
        return templateRepository.save(template);
    }

    public NotificationTemplate updateTemplate(String id, NotificationType type, String subject, String body, List<String> variables) {
        Optional<NotificationTemplate> existingTemplate = templateRepository.findById(id);
        if (existingTemplate.isPresent()) {
            NotificationTemplate template = existingTemplate.get();
            template.setType(type);
            template.setSubject(subject);
            template.setBody(body);
            template.setVariables(variables);
            return templateRepository.save(template);
        }
        return null;
    }

    public String renderTemplate(NotificationType type, Map<String, String> data) {
        NotificationTemplate template = getTemplateByType(type);
        if (template != null) {
            return template.renderWithData(data);
        }
        return null;
    }
}
