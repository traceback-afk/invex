package com.invex.invex;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import com.invex.invex.entities.User;
import com.invex.invex.config.HibernateUtil;
import com.invex.invex.entities.Inventory;

import java.io.IOException;

public class InvexApplication extends Application {
    private static void createDefaultAdminUser(SessionFactory sessionFactory) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            User admin = session.createQuery(
                            "FROM User WHERE username = :username", User.class
                    ).setParameter("username", "admin")
                    .uniqueResult();

            if (admin == null) {
                // create admin user
                admin = new User();
                admin.setUsername("admin");
                admin.setPassword("admin");
                session.persist(admin);

                // create inventory for user
                Inventory inventory = new Inventory();
                inventory.setUser(admin);
                session.persist(inventory);

                System.out.println("Default admin user and inventory created.");
            } else {
                // ensure inventory exists
                Inventory inventory = session.createQuery(
                                "FROM Inventory WHERE user = :user", Inventory.class
                        ).setParameter("user", admin)
                        .uniqueResult();

                if (inventory == null) {
                    inventory = new Inventory();
                    inventory.setUser(admin);
                    session.persist(inventory);
                    System.out.println("Inventory created for existing admin.");
                } else {
                    System.out.println("Admin and inventory already exist.");
                }
            }

            tx.commit();
        }
    }

    @Override
    public void init() {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        createDefaultAdminUser(sessionFactory);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(InvexApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 500);
        stage.setTitle("Invex | Advanced Inventory Management");
        stage.setScene(scene);
        stage.show();
    }
}
