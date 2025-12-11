package com.chessping.networks;

import java.net.*;
import java.io.*;
import com.chessping.ChessPingApp;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ChessPingApp app;
    private String serverIp;
    private int port;
    
    public GameClient(String serverIp, int port, ChessPingApp app) {
        this.serverIp = serverIp;
        this.port = port;
        this.app = app;
    }
    
    public void connect() throws IOException {
        socket = new Socket(serverIp, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        
        System.out.println("Connecté au serveur " + serverIp + ":" + port);
        
        // Démarrer un thread pour recevoir les messages
        new Thread(this::receiveMessages).start();
    }
    
    private void receiveMessages() {
        try {
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof GameState) {
                    GameState gameState = (GameState) obj;
                    System.out.println("Reçu GameState du serveur");
                    if (app != null) {
                        app.receiveNetworkUpdate(gameState);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Déconnecté du serveur: " + e.getMessage());
        }
    }
    
    public void sendGameState(GameState gameState) {
        try {
            out.writeObject(gameState);
            out.flush();
        } catch (IOException e) {
            System.out.println("Erreur d'envoi: " + e.getMessage());
        }
    }
    
    public void sendPaddleUpdate(PaddleUpdate update) {
        try {
            out.writeObject(update);
            out.flush();
            System.out.println("PaddleUpdate envoyé au serveur");
        } catch (IOException e) {
            System.out.println("Erreur d'envoi de la raquette: " + e.getMessage());
        }
    }

    public void sendServeRequest(ServeRequest req) {
        try {
            out.writeObject(req);
            out.flush();
            System.out.println("ServeRequest envoyé au serveur");
        } catch (IOException e) {
            System.out.println("Erreur d'envoi ServeRequest: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        System.out.println("Client déconnecté proprement");
    }
}