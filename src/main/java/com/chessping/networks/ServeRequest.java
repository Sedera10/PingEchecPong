package com.chessping.networks;

import java.io.Serializable;

public class ServeRequest implements Serializable {
    public double vx, vy;
    public boolean isWhite;
    public String playerName;

    public ServeRequest(double vx, double vy, boolean isWhite, String playerName) {
        this.vx = vx;
        this.vy = vy;
        this.isWhite = isWhite;
        this.playerName = playerName;
    }
}
