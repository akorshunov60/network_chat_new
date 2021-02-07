package client;

import client.controllers.AuthDialogController;
import client.controllers.ChatController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import client.models.Network;

import java.io.IOException;
import java.util.List;


public class NetworkClient extends Application {

    public static final List<String> USERS_TEST_DATA = List.of("Александр", "Никита", "Сергей");
    public Stage primaryStage;
    private Stage authStage;
    private Network network;
    private ChatController chatController;

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
            this.primaryStage = primaryStage;
            network = new Network();
            if (!network.connect()) {
                showErrorMessage("", "Ошибка подключения к серверу");
                return;
            }

            openAuthDialog(primaryStage);
            createChatDialog(primaryStage);
    }

    private void createChatDialog(Stage primaryStage) throws IOException {
        FXMLLoader mainLoader = new FXMLLoader();
        mainLoader.setLocation(NetworkClient.class.getResource("views/chat-view.fxml"));

        Parent root = mainLoader.load();

        primaryStage.setTitle("Messenger");
        primaryStage.setScene(new Scene(root, 600, 400));

        chatController = mainLoader.getController();
        chatController.setNetwork(network);


        primaryStage.setOnCloseRequest(event -> network.close());
    }

    private void openAuthDialog(Stage primaryStage) throws IOException {
        FXMLLoader authLoader = new FXMLLoader();
        authLoader.setLocation(NetworkClient.class.getResource("views/auth-dialog.fxml"));
        Parent page = authLoader.load();
        authStage = new Stage();

        authStage.setTitle("Авторизация");
        authStage.initModality(Modality.WINDOW_MODAL);
        authStage.initOwner(primaryStage);
        Scene scene = new Scene(page);
        authStage.setScene(scene);
        authStage.show();

        AuthDialogController authDialogController = authLoader.getController();
        authDialogController.setNetwork(network);
        authDialogController.setNetworkClient(this);
    }

    public static void showErrorMessage(String message, String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Проблемы с соединением");
        alert.setHeaderText(errorMessage);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void openChat() {
        authStage.close();
        primaryStage.show();
        primaryStage.setTitle(network.getUsername());
        System.out.println(network.getUsername());
        chatController.setUsernameTitle(network.getUsername());
        network.waitMessage(chatController);
    }
}