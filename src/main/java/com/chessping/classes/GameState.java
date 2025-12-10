package com.chessping.classes;

import java.io.Serializable;

public class GameState implements Serializable {
    public double whitePaddleX, whitePaddleY;
    public double blackPaddleX, blackPaddleY;
    public double ballX, ballY;
    public double ballSpeedX, ballSpeedY;
    public boolean gameRunning;
    public int whiteScore = 0;
    public int blackScore = 0;
    // Add board configuration so clients can sync initial game
    public int pieceLevel = 8;
    public int kingLife = 5;
    public int queenLife = 4;
    public int knightLife = 2;
    public int pawnLife = 1;
    public String[][] boardPieces;
    public PieceLifeInfo[][] piecesLife;
    
    public GameState() {}
    
    public static class PieceLifeInfo implements Serializable {
        public String symbol;
        public int life;
        public boolean isWhite;
        public PieceLifeInfo(String symbol, int life, boolean isWhite) {
            this.symbol = symbol;
            this.life = life;
            this.isWhite = isWhite;
        }
    }
}