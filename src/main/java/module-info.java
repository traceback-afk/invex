module com.invex.invex {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.naming;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires java.sql;

    opens com.invex.invex.entities to org.hibernate.orm.core;
    exports com.invex.invex;
    opens com.invex.invex to javafx.fxml, org.hibernate.orm.core;
}