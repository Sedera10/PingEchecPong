package com.chessping.classes;

import java.io.Serializable;

public class PaddleUpdate implements Serializable {
    public double x, y;
    public boolean isWhite;
    public String playerName;
    
    public PaddleUpdate(double x, double y, boolean isWhite, String playerName) {
        this.x = x;
        this.y = y;
        this.isWhite = isWhite;
        this.playerName = playerName;
    }
}