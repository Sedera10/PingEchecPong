package com.chessping.classes;

public class GameConfig {
    public int pieceLevel = 8;
    public NetworkConfig networkConfig = new NetworkConfig();
    public int kingLife = 5;
    public int queenLife = 4;
    public int knightLife = 2;
    public int pawnLife = 1;
    
    // CORRIGER : ajouter le mot-clé public
    public boolean isNetworkMode() {
        return networkConfig.getMode() != GameMode.LOCAL;
    }
}