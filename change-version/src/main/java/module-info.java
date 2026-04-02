module com.betolara1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;
    requires transitive javafx.graphics;
    
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

    opens com.betolara1 to javafx.fxml;
    exports com.betolara1;
}
