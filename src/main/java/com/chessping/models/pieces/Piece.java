package com.chessping.models.pieces;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class Piece {
    protected String symbol;
    protected int life;
    protected int maxLife;
    protected boolean isWhite;
    protected int row;
    protected int col;
    
    public Piece(String symbol, int life, boolean isWhite, int row, int col) {
        this.symbol = symbol;
        this.life = life;
        this.maxLife = life;
        this.isWhite = isWhite;
        this.row = row;
        this.col = col;
    }
    
    public abstract void draw(GraphicsContext gc, int tileSize);
    
    public void takeDamage(int damage) {
        life = Math.max(0, life - damage);
    }
    
    public boolean isAlive() {
        return life > 0;
    }
    
    public void resetLife() {
        life = maxLife;
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public int getLife() { return life; }
    public boolean isWhite() { return isWhite; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    
    // Setters
    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }
    
    protected void drawLifeCircle(GraphicsContext gc, int tileSize) {
        if (life > 0) {
            double circleX = col * tileSize + tileSize - 20;
            double circleY = row * tileSize + tileSize - 20;
            double radius = 8;
            
            gc.setFill(life <= 1 ? Color.RED : Color.GREEN);
            gc.fillOval(circleX, circleY, radius * 2, radius * 2);
            
            gc.setFont(javafx.scene.text.Font.font(10));
            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(life), circleX + radius - 4, circleY + radius + 4);
        }
    }
}