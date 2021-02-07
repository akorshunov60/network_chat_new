package Practice_4.HomeWork;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Timer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class ClientHandler {

        private final MyServer myServer;
        private final Socket clientSocket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String clientUsername;

        public ClientHandler(MyServer myServer, Socket clientSocket) throws SocketException {
            this.myServer = myServer;
            this.clientSocket = clientSocket;
        }

        public void handle() throws IOException {
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
/*
Предыдущая версия кода, представленного ниже с использованием ExecutorService:
            new Thread(() -> {
            try {
                authentication();
                readMessage();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }).start();
 */
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {
                    try {
                        authentication();
                        readMessage();
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
            });
            executorService.shutdown();
        }

        private void authentication() throws IOException {

            clientSocket.setSoTimeout(120000);// Установка таймера на закрытие соединения с сервером

            while (true) {

                Command command = readCommand();
                if (command == null) {
                    try {
                        System.out.println("У вас есть 2 минуты, чтобы авторизоваться");
                        System.out.println("Введите логин и пароль:");
                        clientSocket.close();
                    }
                    catch (SocketTimeoutException s) {
                        System.out.println("Соединение будет закрыто!");
                        break;
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                if (command.getType() == CommandType.AUTH) {

                    boolean isSuccessAuth = processAuthCommand(command);
                    if (isSuccessAuth) {
                        break;
                    }
                } else {
                    sendMessage(Command.authErrorCommand("Ошибка авторизации"));
                }
            }
        }

        private boolean processAuthCommand(Command command) throws IOException {
            AuthCommandData cmdData = (AuthCommandData) command.getData();
            String login = cmdData.getLogin();
            String password = cmdData.getPassword();

            AuthService authService = myServer.getAuthService();
            this.clientUsername = authService.getUsernameByLoginAndPassword(login, password);
            if (clientUsername != null)
            {
                if (myServer.isUsernameBusy(clientUsername))
                {
                    sendMessage(Command.authErrorCommand("Логин уже используется"));
                    return false;
                }
                sendMessage(Command.authOkCommand(clientUsername));
                String message = String.format(">>> %s присоединился к чату", clientUsername);
                myServer.broadcastMessage(this, Command.messageInfoCommand(message, null));
                myServer.subscribe(this);
                return true;
            }
            else {
                sendMessage(Command.authErrorCommand("Логин или пароль не зарегистрированы"));
                return false;
            }
        }

        private Command readCommand() throws IOException {
            try {
                return (Command) in.readObject();
            }
            catch (ClassNotFoundException e)
            {
                String errorMessage = "Получен неизвестный объект";
                System.err.println(errorMessage);
                e.printStackTrace();
                return null;
            }
        }

        private void readMessage() throws IOException {
            while (true) {
                Command command = readCommand();
                if (command == null) {
                    continue;
                }

                switch (command.getType()) {
                    case END:
                        return;
                    case PUBLIC_MESSAGE: {
                        PublicMessageCommandData data = (PublicMessageCommandData) command.getData();
                        String message = data.getMessage();
                        String sender = data.getSender();
                        myServer.broadcastMessage(this, Command.messageInfoCommand(message, sender));
                        break;
                    }
                    case PRIVATE_MESSAGE:
                        PrivateMessageCommandData data = (PrivateMessageCommandData) command.getData();
                        String recipient = data.getReceiver();
                        String message = data.getMessage();
                        myServer.sendPrivateMessage(recipient, Command.messageInfoCommand(message, recipient));
                        break;
                    default:
                        String errorMessage = "Неизвестный тип команды" + command.getType();
                        System.err.println(errorMessage);
                        sendMessage(Command.errorCommand(errorMessage));
                }
            }
        }

        public String getClientUsername() {
            return clientUsername;
        }

        public void sendMessage(Command command) throws IOException {
            out.writeObject(command);
        }
    }
