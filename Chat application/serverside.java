

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class serverside {
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    private static final String LOG_FILE = "chatlog.txt";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler != sender) {
                clientHandler.sendMessage(message);
            }
        }
        logMessage(message);
    }

    private static void logMessage(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Enter your name:");
            clientName = in.readLine();
            String welcomeMessage = clientName + " has joined the chat.";
            System.out.println(welcomeMessage);
            serverside.broadcastMessage(welcomeMessage, this);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(clientName + ": " + message);
                serverside.broadcastMessage(clientName + ": " + message, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
           serverside.removeClient(this);
            String goodbyeMessage = clientName + " has left the chat.";
            System.out.println(goodbyeMessage);
           serverside.broadcastMessage(goodbyeMessage, this);
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
