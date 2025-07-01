package com.Ecommerce.Product_Service.Payload.Supplier;



import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ContractDetailsHelper {

    /**
     * Create a contract details map with common fields
     */
    public static Map<String, Object> createContractDetails(
            String contractType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal contractValue,
            String paymentTerms) {

        Map<String, Object> contractDetails = new HashMap<>();
        contractDetails.put("contractType", contractType);
        contractDetails.put("startDate", startDate);
        contractDetails.put("endDate", endDate);
        contractDetails.put("contractValue", contractValue);
        contractDetails.put("paymentTerms", paymentTerms);

        return contractDetails;
    }

    /**
     * Create a detailed contract details map
     */
    public static Map<String, Object> createDetailedContractDetails(
            String contractType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal contractValue,
            String paymentTerms,
            String deliveryTerms,
            String contactPersonName,
            String contactPersonEmail,
            String contactPersonPhone,
            String notes) {

        Map<String, Object> contractDetails = new HashMap<>();
        contractDetails.put("contractType", contractType);
        contractDetails.put("startDate", startDate);
        contractDetails.put("endDate", endDate);
        contractDetails.put("contractValue", contractValue);
        contractDetails.put("paymentTerms", paymentTerms);
        contractDetails.put("deliveryTerms", deliveryTerms);
        contractDetails.put("contactPersonName", contactPersonName);
        contractDetails.put("contactPersonEmail", contactPersonEmail);
        contractDetails.put("contactPersonPhone", contactPersonPhone);
        contractDetails.put("notes", notes);

        return contractDetails;
    }

    /**
     * Extract a value from contract details with type safety
     */
    @SuppressWarnings("unchecked")
    public static <T> T getContractDetail(Map<String, Object> contractDetails, String key, Class<T> type) {
        if (contractDetails == null || !contractDetails.containsKey(key)) {
            return null;
        }

        Object value = contractDetails.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }

        return null;
    }

    /**
     * Check if contract is currently active
     */
    public static boolean isContractActive(Map<String, Object> contractDetails) {
        if (contractDetails == null) {
            return false;
        }

        LocalDateTime startDate = getContractDetail(contractDetails, "startDate", LocalDateTime.class);
        LocalDateTime endDate = getContractDetail(contractDetails, "endDate", LocalDateTime.class);
        LocalDateTime now = LocalDateTime.now();

        if (startDate == null || endDate == null) {
            return false;
        }

        return now.isAfter(startDate) && now.isBefore(endDate);
    }
}

// Example usage in service layer
/*
@Service
public class SupplierService {

    public Supplier createSupplierWithContract(SupplierRequestDTO request) {
        Supplier supplier = new Supplier();
        supplier.setName(request.getName());
        supplier.setContactInfo(request.getContactInfo());
        supplier.setAddress(request.getAddress());

        // Example of creating contract details
        Map<String, Object> contractDetails = ContractDetailsHelper.createContractDetails(
            "Standard Supply Agreement",
            LocalDateTime.now(),
            LocalDateTime.now().plusYears(1),
            new BigDecimal("50000.00"),
            "Net 30 days"
        );

        supplier.setContractDetails(contractDetails);

        return supplierRepository.save(supplier);
    }

    public List<Supplier> findSuppliersWithActiveContracts() {
        return supplierRepository.findAll()
            .stream()
            .filter(supplier -> ContractDetailsHelper.isContractActive(supplier.getContractDetails()))
            .collect(Collectors.toList());
    }
}
*/

// Example JSON structure for contractDetails field:
/*
{
  "contractType": "Standard Supply Agreement",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-12-31T23:59:59",
  "contractValue": 50000.00,
  "paymentTerms": "Net 30 days",
  "deliveryTerms": "FOB Destination",
  "contactPersonName": "John Smith",
  "contactPersonEmail": "john.smith@supplier.com",
  "contactPersonPhone": "+1-555-123-4567",
  "notes": "Preferred supplier for electronics components"
}
*/

// Sample request for creating a supplier with contract details:
/*
POST /suppliers
{
  "name": "Tech Components Ltd",
  "contactInfo": "Email: contact@techcomponents.com, Phone: +1-555-987-6543",
  "address": "123 Industrial Park, Tech City, TC 12345",
  "rating": 4.5,
  "contractDetails": {
    "contractType": "Exclusive Supply Agreement",
    "startDate": "2024-06-01T00:00:00",
    "endDate": "2025-05-31T23:59:59",
    "contractValue": 75000.00,
    "paymentTerms": "Net 45 days",
    "deliveryTerms": "FOB Origin",
    "contactPersonName": "Sarah Johnson",
    "contactPersonEmail": "sarah.johnson@techcomponents.com",
    "contactPersonPhone": "+1-555-987-6544",
    "notes": "Primary supplier for all electronic components. Volume discounts apply."
  }
}
*/