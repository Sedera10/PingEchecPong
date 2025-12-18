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
                out.flush();  // IMPORTANT: flush pour éviter le deadlock avec ObjectInputStream
                in = new ObjectInputStream(socket.getInputStream());
                
                System.out.println("ClientHandler démarré pour " + socket.getInetAddress());

                // Attendre un peu pour que le CLIENT initialise son ObjectInputStream
                try {
                    Thread.sleep(100);  // 100ms devrait suffire
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Envoyer l'état initial au client pour synchroniser la configuration
                if (app != null) {
                    try {
                        GameState initial = app.createGameState();
                        System.out.println("=== Envoi état initial au nouveau client ===");
                        System.out.println("  pieceLevel: " + initial.pieceLevel);
                        System.out.println("  kingLife: " + initial.kingLife);
                        System.out.println("  queenLife: " + initial.queenLife);
                        System.out.println("  knightLife: " + initial.knightLife);
                        System.out.println("  pawnLife: " + initial.pawnLife);
                        sendGameState(initial);
                        System.out.println("État initial envoyé avec succès");
                    } catch (Exception ex) {
                        System.err.println("ERREUR CRITIQUE envoi état initial: " + ex.getMessage());
                        ex.printStackTrace();
                    }
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
                    } else if (obj instanceof ServeRequest) {
                        ServeRequest req = (ServeRequest) obj;
                        System.out.println("Reçu ServeRequest de " + req.playerName);
                        
                        // Le client veut servir la balle
                        if (app != null) {
                            Platform.runLater(() -> {
                                app.launchBall(req.vx, req.vy, req.isWhite);
                                // Broadcast immédiatement pour synchroniser tous les clients
                                GameState gs = app.createGameState();
                                server.broadcastGameState(gs);
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