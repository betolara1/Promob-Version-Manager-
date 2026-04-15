package com.betolara1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private static boolean isDarkMode = false;

    @Override
    public void start(Stage stage) throws IOException {
        // Inicializa com o tema Light
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        
        scene = new Scene(loadFXML("primary"), 700, 650);
        
        // Adiciona CSS personalizado
        scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());
        
        stage.setTitle("Trocar Versão Promob");
        stage.setScene(scene);
        stage.setResizable(true); // Permitir redimensionar para melhor UX
        stage.show();

        stage.getIcons().add(new Image(App.class.getResourceAsStream("icons/icon.png")));
    }

    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }
    }

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}