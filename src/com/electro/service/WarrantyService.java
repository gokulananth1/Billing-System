package com.electro.service;

import com.electro.model.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Service managing warranty tracking, serial number registers, and warranty claims.
 */
public class WarrantyService {
    private final DataStore store;

    public WarrantyService() {
        this.store = DataStore.getInstance();
    }

    public List<WarrantyRecord> getAllWarranties() {
        return store.getAllWarranties();
    }

    public WarrantyRecord findBySerial(String serialNumber) {
        if (serialNumber == null) return null;
        return store.getWarrantyBySerial(serialNumber.trim());
    }

    public List<WarrantyRecord> findByCustomerPhone(String phone) {
        return store.getWarrantiesByPhone(phone);
    }

    public void registerWarranty(String serialNumber, String invoiceId, Product product,
                                 Customer customer, LocalDate purchaseDate) {
        if (serialNumber == null || serialNumber.trim().isEmpty() || product == null) {
            return;
        }

        WarrantyRecord record = new WarrantyRecord(
                serialNumber.trim().toUpperCase(),
                invoiceId,
                product.getId(),
                product.getName(),
                product.getBrand(),
                customer != null ? customer.getName() : "Customer",
                customer != null ? customer.getPhone() : "",
                purchaseDate,
                product.getWarrantyMonths()
        );

        store.addWarranty(record);
    }

    public void voidWarrantyForInvoice(String invoiceId) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) return;
        boolean updated = false;
        for (WarrantyRecord wr : store.getAllWarranties()) {
            if (invoiceId.equalsIgnoreCase(wr.getInvoiceId())) {
                wr.setStatus("VOID_RETURNED");
                updated = true;
            }
        }
        if (updated) {
            store.saveWarrantiesData();
        }
    }

    public synchronized boolean cancelWarranty(String serialNumber) {
        if (serialNumber == null || serialNumber.trim().isEmpty()) return false;
        WarrantyRecord wr = store.getWarrantyBySerial(serialNumber.trim().toUpperCase());
        if (wr == null || wr.isVoided()) return false;
        wr.setStatus("CANCELLED");
        store.saveWarrantiesData();
        return true;
    }
}
