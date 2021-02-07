import chat.*;

import java.io.IOException;

public class ServerApp {
    private static final int DEFAULT_PORT = 8189;


    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        if(args.length != 0) {
            port = Integer.parseInt(args[0]);
        }

        try {
            new MyServer(port).start();
        } catch (IOException e) {
            System.out.println("Ошибка!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
