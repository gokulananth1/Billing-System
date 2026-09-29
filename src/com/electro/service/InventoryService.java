package com.electro.service;

import com.electro.model.Product;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing inventory operations, stock tracking, and search.
 */
public class InventoryService {
    private final DataStore store;

    public InventoryService() {
        this.store = DataStore.getInstance();
    }

    public List<Product> getAllProducts() {
        return store.getAllProducts();
    }

    public Product getProductById(String id) {
        return store.getProductById(id);
    }

    public void saveProduct(Product product) {
        if (product.getId() == null || product.getId().trim().isEmpty()) {
            product.setId("P" + (System.currentTimeMillis() % 100000));
        }
        store.saveProduct(product);
    }

    public void deleteProduct(String id) {
        store.deleteProduct(id);
    }

    public void removeAllProducts() {
        store.removeAllProducts();
    }

    public void restoreDefaultProducts() {
        store.restoreDefaultProducts();
    }

    public void restockProduct(String id, int quantityToAdd) {
        if (quantityToAdd > 0) {
            store.updateStock(id, quantityToAdd);
        }
    }

    public List<Product> getLowStockProducts() {
        return store.getAllProducts().stream()
                .filter(Product::isLowStock)
                .collect(Collectors.toList());
    }

    public List<String> getAllCategories() {
        Set<String> categories = new TreeSet<>();
        for (Product p : store.getAllProducts()) {
            if (p.getCategory() != null && !p.getCategory().isEmpty()) {
                categories.add(p.getCategory());
            }
        }
        return new ArrayList<>(categories);
    }

    public List<String> getAllBrands() {
        Set<String> brands = new TreeSet<>();
        for (Product p : store.getAllProducts()) {
            if (p.getBrand() != null && !p.getBrand().isEmpty()) {
                brands.add(p.getBrand());
            }
        }
        return new ArrayList<>(brands);
    }

    public List<Product> searchProducts(String query, String category) {
        String q = query != null ? query.trim().toLowerCase() : "";
        boolean filterCategory = category != null && !category.equalsIgnoreCase("All") && !category.isEmpty();

        return store.getAllProducts().stream()
                .filter(p -> {
                    if (filterCategory && !p.getCategory().equalsIgnoreCase(category)) {
                        return false;
                    }
                    if (q.isEmpty()) return true;
                    return (p.getName() != null && p.getName().toLowerCase().contains(q)) ||
                           (p.getBrand() != null && p.getBrand().toLowerCase().contains(q)) ||
                           (p.getModelNumber() != null && p.getModelNumber().toLowerCase().contains(q)) ||
                           (p.getSku() != null && p.getSku().toLowerCase().contains(q)) ||
                           (p.getId() != null && p.getId().toLowerCase().contains(q));
                })
                .collect(Collectors.toList());
    }
}
