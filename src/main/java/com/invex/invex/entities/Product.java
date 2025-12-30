package com.invex.invex.entities;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inventory_id", nullable = false, updatable = false)
    private Inventory inventory;

    private String name;

    // Using BigDecimal to avoid precision problems in float or double
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    // initialize the list to return empty list instead of null
    // for a product that has no transactions
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductTransaction> transactions = new ArrayList<>();

    private String category;
    private int quantity=0;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null.");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price cannot be 0 or negative.");
        }

        // max 2 decimal places
        if (price.scale() > 2) {
            throw new IllegalArgumentException("Price cannot have more than 2 decimal places.");
        }

        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category.toLowerCase();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if(quantity<0){
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        this.quantity = quantity;
    }

    public BigDecimal calculateTotalValue() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        if (inventory == null) {
            throw new IllegalArgumentException("Inventory cannot be null.");
        }
        this.inventory = inventory;
    }
    public List<ProductTransaction> getTransactions() {
        return transactions;
    }
}