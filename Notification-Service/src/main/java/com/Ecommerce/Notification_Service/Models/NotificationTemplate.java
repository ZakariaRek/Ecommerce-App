package com.Ecommerce.Notification_Service.Models;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document(collection = "notification_templates")
public class NotificationTemplate {
    @Id
    private String id;
    private NotificationType type;
    private String subject;
    private String body;
    private List<String> variables;

    // Constructors
    public NotificationTemplate() {
    }

    public NotificationTemplate(NotificationType type, String subject, String body, List<String> variables) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.subject = subject;
        this.body = body;
        this.variables = variables;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }

    // Business logic
    public String renderWithData(Map<String, String> data) {
        String renderedBody = this.body;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            renderedBody = renderedBody.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return renderedBody;
    }
}