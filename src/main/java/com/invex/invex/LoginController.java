package com.invex.invex;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import com.invex.invex.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import com.invex.invex.entities.User;
import com.invex.invex.util.AlertUtil;
import com.invex.invex.util.SessionContext;


public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();

        Query<User> q = session.createQuery(
                "from User where username = :username AND password = :password", User.class);
        q.setParameter("username", username);
        q.setParameter("password", password);

        User user = q.uniqueResult();

        session.getTransaction().commit();
        session.close();

        if (user != null) {
            System.out.println("Login success: " + user.getUsername());
            SessionContext.setCurrentUser(user);

            try {
                javafx.fxml.FXMLLoader loader =
                        new javafx.fxml.FXMLLoader(getClass().getResource("main-view.fxml"));
                javafx.scene.Parent root = loader.load();

                javafx.stage.Stage stage =
                        (javafx.stage.Stage) usernameField.getScene().getWindow();

                stage.setScene(new javafx.scene.Scene(root));
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            AlertUtil.showError("Invalid username or password.");
        }
    }
}
