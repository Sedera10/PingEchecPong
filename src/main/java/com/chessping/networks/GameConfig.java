package com.chessping.networks;

import java.io.Serializable;

public class GameConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int pieceLevel = 8;
    public NetworkConfig networkConfig = new NetworkConfig();
    public int kingLife = 5;
    public int queenLife = 4;
    public int knightLife = 2;
    public int pawnLife = 1;
    public int bishopLife = 3;
    public int rookLife = 4;
    
    public boolean isNetworkMode() {
        return networkConfig.getMode() != GameMode.LOCAL;
    }
}