package com.chessping.networks;

import java.net.*;
import java.io.*;
import java.util.*;
import com.chessping.ChessPingApp;
import javafx.application.Platform;

public class GameServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private ChessPingApp app;
    private int port;
    private volatile boolean running = false;
    
    public GameServer(int port, ChessPingApp app) {
        this.port = port;
        this.app = app;
    }
    
    public void start() throws IOException {
        // Create ServerSocket unbound so we can set SO_REUSEADDR before bind
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));
        running = true;
        System.out.println("Serveur en attente de connexions sur le port " + port);

        while (running) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
            } catch (SocketException se) {
                // Server socket closed, exit loop
                break;
            }
            if (clientSocket == null) break;
            System.out.println("Nouveau client connecté: " + clientSocket.getInetAddress());

            ClientHandler clientHandler = new ClientHandler(clientSocket, this);
            clients.add(clientHandler);
            new Thread(clientHandler).start();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            System.out.println("Erreur fermeture serveur: " + e.getMessage());
        }

        // Close client handlers and sockets
        for (ClientHandler ch : new ArrayList<>(clients)) {
            ch.close();
        }
        clients.clear();
    }
    
    public void broadcastGameState(GameState gameState) {
        for (ClientHandler client : clients) {
            client.sendGameState(gameState);
        }
    }
    
    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }
    
    class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private GameServer server;
        
        public ClientHandler(Socket socket, GameServer server) {
            this.socket = socket;
            this.server = server;
        }
        
        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                System.out.println("ClientHandler démarré pour " + socket.getInetAddress());

                // Send initial game state to this client so it can sync configuration
                try {
                    if (app != null) {
                        GameState initial = app.createGameState();
                        sendGameState(initial);
                    }
                } catch (Exception ex) {
                    System.out.println("Erreur envoi état initial: " + ex.getMessage());
                }

                // Recevoir les messages
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof PaddleUpdate) {
                        PaddleUpdate update = (PaddleUpdate) obj;
                        System.out.println("Reçu PaddleUpdate de " + update.playerName);
                        
                        // Transmettre la mise à jour au jeu principal sur le thread JavaFX
                        if (app != null) {
                            Platform.runLater(() -> {
                                if (update.isWhite) {
                                    app.setWhitePaddlePosition(update.x, update.y);
                                } else {
                                    app.setBlackPaddlePosition(update.x, update.y);
                                }
                            });
                        }
                    }
                    // Ajoutez d'autres types de messages si nécessaire
                }
            } catch (Exception e) {
                System.out.println("Client déconnecté: " + e.getMessage());
                server.removeClient(this);
            }
        }
        
        public void sendGameState(GameState gameState) {
            try {
                out.writeObject(gameState);
                out.flush();
            } catch (IOException e) {
                System.out.println("Erreur d'envoi au client: " + e.getMessage());
            }
        }

        public void close() {
            try { 
                if (in != null) in.close(); 
            } catch (IOException ignored) {}
            try { 
                if (out != null) out.close(); 
            } catch (IOException ignored) {}
            try { 
                if (socket != null && !socket.isClosed()) socket.close(); 
            } catch (IOException ignored) {}
        }
    }
}