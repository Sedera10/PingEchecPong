package com.chessping.networks;

import java.io.Serializable;

public class Message implements Serializable {
    public enum Type {
        CONNECT, DISCONNECT, 
        PADDLE_MOVE, BALL_UPDATE,
        PIECE_HIT, GAME_STATE,
        PLAYER_INFO, CHAT
    }
    
    private Type type;
    private Object data;
    private String playerId;
    private long timestamp;
}