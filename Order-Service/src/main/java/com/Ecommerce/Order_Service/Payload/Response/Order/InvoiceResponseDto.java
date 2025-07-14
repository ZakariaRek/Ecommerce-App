package com.Ecommerce.Order_Service.Payload.Response.Order;

import lombok.Data;
import java.util.UUID;

@Data
public class InvoiceResponseDto {
    private UUID orderId;
    private String invoiceData;
    private String downloadUrl;
}