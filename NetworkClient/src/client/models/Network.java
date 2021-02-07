package client.models;

import client.controllers.ChatController;
import clientserver.Command;
import clientserver.commands.*;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

public class Network {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8189;

    private final String host;
    private final int port;

    private ObjectOutputStream dataOutputStream;
    private ObjectInputStream dataInputStream;

    private Socket socket;

    private String username;

    public ObjectOutputStream getDataOutputStream() {
        return dataOutputStream;
    }

    public ObjectInputStream getDataInputStream() {
        return dataInputStream;
    }

    public Network() {
        this(SERVER_ADDRESS, SERVER_PORT);
    }

    public Network(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            dataOutputStream = new ObjectOutputStream(socket.getOutputStream());
            dataInputStream = new ObjectInputStream(socket.getInputStream());
            return true;

        } catch (IOException e) {
            System.out.println("Соединение не было установлено!");
            e.printStackTrace();
            return false;
        }

    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void waitMessage(ChatController chatController) {

       Thread thread = new Thread( () -> {
           try { while (true) {

               Command command = readCommand();
               if(command == null) {
                   chatController.showError("Ошибка сервера", "Получена неверная команда");
                   continue;
               }

               switch (command.getType()) {
                   case INFO_MESSAGE: {
                       MessageInfoCommandData data = (MessageInfoCommandData) command.getData();
                       String message = data.getMessage();
                       String sender = data.getSender();
                       String formattedMessage = sender != null ? String.format("%s: %s", sender, message) : message;
                       Platform.runLater(() -> {
                           chatController.appendMessage(formattedMessage);
                       });
                       break;
                   }
                   case ERROR: {
                       ErrorCommandData data = (ErrorCommandData) command.getData();
                       String errorMessage = data.getErrorMessage();
                       Platform.runLater(() -> {
                           chatController.showError("Server error", errorMessage);
                       });
                       break;
                   }
                   case UPDATE_USERS_LIST: {
                       UpdateUsersListCommandData data = (UpdateUsersListCommandData) command.getData();
                       Platform.runLater(() -> chatController.updateUsers(data.getUsers()));
                       break;
                   }
                   default:
                       Platform.runLater(() -> {
                           chatController.showError("Unknown command from server!", command.getType().toString());
                       });
               }

           }
           } catch (IOException e) {
               e.printStackTrace();
               System.out.println("Соединение потеряно!");
           }
       });
       thread.setDaemon(true);
       thread.start();
    }


    public String sendAuthCommand(String login, String password) {
        try {
            Command authCommand = Command.authCommand(login, password);
            dataOutputStream.writeObject(authCommand);

            Command command = readCommand();
            if (command == null) {
                return "Ошибка чтения команды с сервера";
            }

            switch (command.getType()) {
                case AUTH_OK: {
                    AuthOkCommandData data = (AuthOkCommandData) command.getData();
                    this.username = data.getUsername();
                    return null;
                }

                case AUTH_ERROR:
                case ERROR: {
                    AuthErrorCommandData data = (AuthErrorCommandData) command.getData();
                    return data.getErrorMessage();
                }
                default:
                    return "Unknown type of command: " + command.getType();

            }
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) throws IOException {
        sendMessage(Command.publicMessageCommand(username, message));
    }

    public void sendMessage(Command command) throws IOException {
        dataOutputStream.writeObject(command);
    }



    public void sendPrivateMessage(String message, String recipient) throws IOException {
        Command command = Command.privateMessageCommand(recipient, message);
        sendMessage(command);
    }

    private Command readCommand() throws IOException {
        try {
            return (Command) dataInputStream.readObject();
        } catch (ClassNotFoundException e) {
            String errorMessage = "Получен неизвестный объект";
            System.err.println(errorMessage);
            e.printStackTrace();
            sendMessage(Command.errorCommand(errorMessage));
            return null;
        }
    }
}
