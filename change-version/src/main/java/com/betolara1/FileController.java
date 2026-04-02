package com.betolara1;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.util.prefs.Preferences;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;

public class FileController implements Initializable {

    private final Preferences prefs = Preferences.userNodeForPackage(FileController.class);
    private static final String KEY_SOURCE_ROOT = "source_root";
    private static final String KEY_DEST_ROOT = "dest_root";

    @FXML private Label lblSourceRoot;
    @FXML private Label lblDestination;
    @FXML private ComboBox<String> cbVersions;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;
    @FXML private Button btnCopy;

    private File sourceRoot;
    private File destinationRoot;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String savedSource = prefs.get(KEY_SOURCE_ROOT, null);
        if (savedSource != null) {
            File folder = new File(savedSource);
            if (folder.exists()) {
                sourceRoot = folder;
                lblSourceRoot.setText(savedSource);
                loadVersions();
            }
        }

        String savedDest = prefs.get(KEY_DEST_ROOT, null);
        if (savedDest != null) {
            File folder = new File(savedDest);
            if (folder.exists()) {
                destinationRoot = folder;
                lblDestination.setText(savedDest);
            }
        }
    }

    @FXML
    private void handleSelectSourceRoot() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Selecionar Pasta das Versões");
        
        Stage stage = (Stage) lblSourceRoot.getScene().getWindow();
        File selected = dc.showDialog(stage);
        
        if (selected != null) {
            sourceRoot = selected;
            lblSourceRoot.setText(selected.getAbsolutePath());
            prefs.put(KEY_SOURCE_ROOT, selected.getAbsolutePath());
            loadVersions();
        }
    }

    private void loadVersions() {
        ObservableList<String> versions = FXCollections.observableArrayList();
        File[] folders = sourceRoot.listFiles(File::isDirectory);
        
        if (folders != null) {
            for (File folder : folders) {
                versions.add(folder.getName());
            }
        }
        
        cbVersions.setItems(versions);
        if (!versions.isEmpty()) {
            cbVersions.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleSelectDestination() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Selecionar Pasta de Destino");
        
        Stage stage = (Stage) lblSourceRoot.getScene().getWindow();
        File selected = dc.showDialog(stage);
        
        if (selected != null) {
            destinationRoot = selected;
            lblDestination.setText(selected.getAbsolutePath());
            prefs.put(KEY_DEST_ROOT, selected.getAbsolutePath());
        }
    }

    @FXML
    private void handleCopyVersion() {
        String selectedVersion = cbVersions.getValue();
        
        if (sourceRoot == null || destinationRoot == null || selectedVersion == null) {
            showAlert("Erro", "Por favor, selecione todos os campos.", Alert.AlertType.ERROR);
            return;
        }

        btnCopy.setDisable(true);
        progressBar.setProgress(0);
        lblStatus.setText("Iniciando processo...");

        Task<Void> copyTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Path sourceSystem = sourceRoot.toPath().resolve(selectedVersion).resolve("System");
                Path destSystem = destinationRoot.toPath().resolve("System");
                Path structuresPath = destSystem.resolve("structures");

                // 1. Excluir pasta structures se existir
                updateMessage("Excluindo pasta structures...");
                if (Files.exists(structuresPath)) {
                    deleteDirectoryRecursively(structuresPath);
                }

                // 2. Copiar pasta System
                updateMessage("Copiando arquivos da versão...");
                updateProgress(-1, 1); // Indeterminado
                if (Files.exists(sourceSystem)) {
                    copyDirectoryRecursively(sourceSystem, destSystem, msg -> updateMessage(msg));
                } else {
                    throw new IOException("Diretório 'System' não encontrado na versão selecionada.");
                }

                updateProgress(1, 1);
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                lblStatus.textProperty().unbind();
                progressBar.progressProperty().unbind();
                
                lblStatus.setText("Concluído com sucesso!");
                progressBar.setProgress(1.0);
                btnCopy.setDisable(false);
                showAlert("Sucesso", "Versão do Promob trocada com sucesso!", Alert.AlertType.INFORMATION);
            }

            @Override
            protected void failed() {
                super.failed();
                lblStatus.textProperty().unbind();
                progressBar.progressProperty().unbind();
                
                lblStatus.setText("Erro ao copiar.");
                btnCopy.setDisable(false);
                progressBar.setProgress(0);
                showAlert("Erro", "Ocorreu um erro: " + getException().getMessage(), Alert.AlertType.ERROR);
            }
        };

        lblStatus.textProperty().bind(copyTask.messageProperty());
        progressBar.progressProperty().bind(copyTask.progressProperty());
        
        new Thread(copyTask).start();
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyDirectoryRecursively(Path source, Path target, java.util.function.Consumer<String> statusUpdater) throws IOException {
        final long[] filesCopied = {0};

        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                try {
                    if (!Files.exists(targetDir)) {
                        Files.createDirectories(targetDir);
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao criar diretório: " + targetDir + " - " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                
                try {
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    File outFile = targetFile.toFile();
                    if (outFile.exists()) {
                        outFile.setWritable(true);
                        try {
                            Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e2) {
                            System.err.println("Falha definitiva ao copiar: " + targetFile);
                        }
                    }
                }
                
                filesCopied[0]++;
                if (filesCopied[0] % 20 == 0) {
                    final String msg = "Copiando: " + filesCopied[0] + " arquivos...";
                    statusUpdater.accept(msg);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @FXML
    private void handleToggleTheme() {
        App.toggleTheme();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // Aplica o CSS do tema ao diálogo
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(App.class.getResource("style.css").toExternalForm());
        
        alert.showAndWait();
    }
}
