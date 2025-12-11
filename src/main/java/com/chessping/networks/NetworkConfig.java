package com.chessping.networks;

public class NetworkConfig {
    private GameMode mode = GameMode.LOCAL;
    private String serverIp = "localhost";
    private int port = 5555;
    private String playerName = "Joueur";
    
    // Getters et setters
    public GameMode getMode() { return mode; }
    public void setMode(GameMode mode) { this.mode = mode; }
    
    public String getServerIp() { return serverIp; }
    public void setServerIp(String serverIp) { this.serverIp = serverIp; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
}