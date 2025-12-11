package com.chessping.models;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Paddle {
    private double x;
    private double y;
    private double width;
    private double height;
    private Color color;
    private boolean isWhite;
    private String playerName;
    
    public Paddle(double x, double y, double width, double height, Color color, boolean isWhite, String playerName) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.isWhite = isWhite;
        this.playerName = playerName;
    }
    
    public void draw(GraphicsContext gc) {
        gc.setFill(color);
        gc.fillRoundRect(x, y, width, height, 5, 5);
    }
    
    public void move(double deltaX, double deltaY, int boardWidth, int tileSize, double minY, double maxY) {
        x = Math.max(0, Math.min(boardWidth * tileSize - width, x + deltaX));
        y = Math.max(minY, Math.min(maxY, y + deltaY));
    }
    
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public boolean collidesWith(double ballX, double ballY, double ballSize) {
        return ballX + ballSize >= x && 
               ballX <= x + width && 
               ballY + ballSize >= y && 
               ballY <= y + height;
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isWhite() { return isWhite; }
    public String getPlayerName() { return playerName; }
}