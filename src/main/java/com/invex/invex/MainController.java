package com.invex.invex;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.beans.property.ReadOnlyObjectWrapper;

import com.invex.invex.entities.*;
import com.invex.invex.config.HibernateUtil;
import com.invex.invex.util.AlertUtil;
import com.invex.invex.util.SessionContext;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;


public class MainController {
    // ------------------ HEADER-------------------
    @FXML private Label inventoryTotalValueLabel;

    // ---------------- PRODUCTS TABLE ----------------
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Long> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colPrice;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Integer> colQuantity;
    @FXML private TableColumn<Product, String> colTotalValue;

    // ---------------- TRANSACTIONS TABLE ----------------
    @FXML private TableView<ProductTransaction> transactionsTable;
    @FXML private TableColumn<ProductTransaction, Long> colTransId;
    @FXML private TableColumn<ProductTransaction, String> colTransProductName;
    @FXML private TableColumn<ProductTransaction, String> colTransType;
    @FXML private TableColumn<ProductTransaction, Integer> colTransQuantity;
    @FXML private TableColumn<ProductTransaction, String> colTransDateTime;
    @FXML private TableColumn<ProductTransaction, Void> colUndo;

    // ---------------- INIT ----------------
    @FXML
    private void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colPrice.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(
                        cd.getValue().getPrice()
                                .setScale(2, RoundingMode.HALF_UP)
                                .toString()
                ));

        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        colTotalValue.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(
                        cd.getValue().calculateTotalValue()
                                .setScale(2, RoundingMode.HALF_UP)
                                .toString()
                ));

        colTransId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTransProductName.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getProduct().getName()));
        colTransType.setCellValueFactory(new PropertyValueFactory<>("transactionCategory"));
        colTransQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        final DateTimeFormatter FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        colTransDateTime.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(
                        cd.getValue().getTimestamp().format(FORMATTER)
                )
        );

        addUndoButtonColumn();
        loadProducts();
        loadTransactions();
        updateInventoryTotalValue();
    }

    private void updateInventoryTotalValue() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Inventory inv = getCurrentInventory(session);

            // force products initialization (LAZY safe)
            inv.getProducts().size();

            BigDecimal total = inv.getTotalValue()
                    .setScale(2, RoundingMode.HALF_UP);

            inventoryTotalValueLabel.setText(total.toString());
        }
    }

    // ---------------- INVENTORY HELPER ----------------
    private Inventory getCurrentInventory(Session session) {
        return session.createQuery(
                        "FROM Inventory WHERE user = :user", Inventory.class
                ).setParameter("user", SessionContext.getCurrentUser())
                .uniqueResult();
    }

    // ---------------- LOAD DATA ----------------
    private void loadProducts() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Inventory inv = getCurrentInventory(session);
            List<Product> products = session.createQuery(
                    "FROM Product WHERE inventory = :inv", Product.class
            ).setParameter("inv", inv).list();

            productsTable.getItems().setAll(products);
        }
    }

    private void loadTransactions() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Inventory inv = getCurrentInventory(session);
            List<ProductTransaction> trx = session.createQuery(
                    "FROM ProductTransaction WHERE inventory = :inv ORDER BY timestamp DESC",
                    ProductTransaction.class
            ).setParameter("inv", inv).list();

            transactionsTable.getItems().setAll(trx);
        }
    }

    // ---------------- CREATE TRANSACTION ----------------
    @FXML
    private void onCreateTransaction() {
        Product product = productsTable.getSelectionModel().getSelectedItem();
        if (product == null) {
            AlertUtil.showAlert("Select a product first.");
            return;
        }

        Dialog<ProductTransaction> dialog = createTransactionDialog();

        dialog.showAndWait().ifPresent(trx -> {

            if ("SELL".equals(trx.getTransactionCategory())
                    && product.getQuantity() < trx.getQuantity()) {
                AlertUtil.showError("Not enough stock.");
                return;
            }

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction tx = session.beginTransaction();
                Inventory inv = getCurrentInventory(session);

                Product managedProduct =
                        session.find(Product.class, product.getId());

                if ("SELL".equals(trx.getTransactionCategory())) {
                    managedProduct.setQuantity(
                            managedProduct.getQuantity() - trx.getQuantity()
                    );
                } else if ("RESTOCK".equals(trx.getTransactionCategory())) {
                    managedProduct.setQuantity(
                            managedProduct.getQuantity() + trx.getQuantity()
                    );
                }

                trx.setProduct(managedProduct);
                trx.setInventory(inv);

                session.persist(trx);
                session.merge(managedProduct);

                tx.commit();
            }

            loadProducts();
            loadTransactions();
            updateInventoryTotalValue();
        });
    }

    // ---------------- UNDO TRANSACTION ----------------
    private void undoTransaction(ProductTransaction trx) {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Undo");
        confirm.setHeaderText("Undo transaction");
        confirm.setContentText(
                "Are you sure you want to undo this transaction?\n" +
                        "This will update the product quantity."
        );

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return; // user cancelled
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            Product product =
                    session.find(Product.class, trx.getProduct().getId());

            if ("SELL".equals(trx.getTransactionCategory())) {
                // Undo SELL → add back
                product.setQuantity(product.getQuantity() + trx.getQuantity());
            } else if ("RESTOCK".equals(trx.getTransactionCategory())) {
                // Undo RESTOCK → subtract
                product.setQuantity(product.getQuantity() - trx.getQuantity());
            }

            session.merge(product);
            session.remove(session.contains(trx) ? trx : session.merge(trx));

            tx.commit();
        } catch (Exception e) {
            AlertUtil.showError(e.getMessage());
            return;
        }

        loadProducts();
        loadTransactions();
        updateInventoryTotalValue();
    }

    private void addUndoButtonColumn() {
        colUndo.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Undo");

            {
                btn.setOnAction(e ->
                        undoTransaction(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    // ---------------- ADD PRODUCT ----------------
    @FXML
    private void onAddProduct() {
        Dialog<Product> dialog = createProductDialog(null);

        dialog.showAndWait().ifPresent(product -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction tx = session.beginTransaction();

                product.setInventory(getCurrentInventory(session));
                session.persist(product);

                tx.commit();
            }

            loadProducts();
            updateInventoryTotalValue();
        });
    }

    // ---------------- DELETE PRODUCT ----------------
    @FXML
    private void onDeleteProduct() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            AlertUtil.showAlert("Select a product to delete.");
            return;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Long trxCount = session.createQuery(
                            "select count(t.id) from ProductTransaction t where t.product.id = :pid",
                            Long.class
                    ).setParameter("pid", selected.getId())
                    .uniqueResult();

            if (trxCount != null && trxCount > 0) {
                AlertUtil.showError("Cannot delete product that has transactions.");
                return;
            }

        } catch (Exception e) {
            AlertUtil.showError(e.getMessage());
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete product");
        confirm.setContentText("Are you sure you want to delete this product?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            Product managed = session.find(Product.class, selected.getId());
            session.remove(managed);

            tx.commit();
        } catch (Exception e){
            AlertUtil.showError(e.getMessage());
        }

        loadProducts();
        updateInventoryTotalValue();
    }

    // ---------------- EDIT PRODUCT ----------------
    @FXML
    private void onEditProduct() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            AlertUtil.showAlert("Select a product to edit.");
            return;
        }

        Dialog<Product> dialog = createProductDialog(selected);

        dialog.showAndWait().ifPresent(updated -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction tx = session.beginTransaction();

                Product managed = session.find(Product.class, selected.getId());

                managed.setName(updated.getName());
                managed.setPrice(updated.getPrice());
                managed.setCategory(updated.getCategory());
                managed.setQuantity(updated.getQuantity());

                tx.commit();
            } catch (Exception e){
                AlertUtil.showError(e.getMessage());
            }

            loadProducts();
            updateInventoryTotalValue();
        });
    }

    // ---------------- DIALOGS ----------------
    private Dialog<ProductTransaction> createTransactionDialog() {
        Dialog<ProductTransaction> dialog = new Dialog<>();
        dialog.setTitle("Create Transaction");

        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("SELL", "RESTOCK");

        TextField qty = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Type:"), 0, 0);
        grid.add(type, 1, 0);
        grid.add(new Label("Quantity:"), 0, 1);
        grid.add(qty, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                ProductTransaction t = new ProductTransaction();
                t.setTransactionCategory(type.getValue());
                t.setQuantity(Integer.parseInt(qty.getText()));
                return t;
            }
            return null;
        });

        return dialog;
    }

    private Dialog<Product> createProductDialog(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "Add Product" : "Edit Product");

        TextField name = new TextField();
        TextField price = new TextField();
        TextField category = new TextField();
        TextField qty = new TextField();

        if (product != null) {
            name.setText(product.getName());
            price.setText(product.getPrice().toString());
            category.setText(product.getCategory());
            qty.setText(String.valueOf(product.getQuantity()));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Price:"), 0, 1);
        grid.add(price, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(category, 1, 2);
        grid.add(new Label("Quantity:"), 0, 3);
        grid.add(qty, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // BLOCK dialog close if validation fails
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                Product test = new Product();
                test.setName(name.getText());
                test.setPrice(new BigDecimal(price.getText())); // catches 8.999
                test.setCategory(category.getText());
                test.setQuantity(Integer.parseInt(qty.getText()));
            } catch (Exception e) {
                AlertUtil.showError(e.getMessage());
                event.consume(); // dialog stays open
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Product p = new Product();
                p.setName(name.getText());
                p.setPrice(new BigDecimal(price.getText()));
                p.setCategory(category.getText());
                p.setQuantity(Integer.parseInt(qty.getText()));
                return p;
            }
            return null;
        });

        return dialog;
    }

}
