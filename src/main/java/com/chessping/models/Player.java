package com.chessping.models;

public class Player {
    private String name;
    private int score;
    private boolean isWhite;
    private Paddle paddle;
    
    public Player(String name, boolean isWhite, Paddle paddle) {
        this.name = name;
        this.score = 0;
        this.isWhite = isWhite;
        this.paddle = paddle;
    }
    
    public void incrementScore(int points) {
        score += points;
    }
    
    public void resetScore() {
        score = 0;
    }
    
    // Getters
    public String getName() { return name; }
    public int getScore() { return score; }
    public boolean isWhite() { return isWhite; }
    public Paddle getPaddle() { return paddle; }
    
    // Setters
    public void setName(String name) { this.name = name; }
}