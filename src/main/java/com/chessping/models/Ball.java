package com.chessping.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Ball {
    private double x;
    private double y;
    private double speedX;
    private double speedY;
    private double size;
    private Color color;
    
    public Ball(double x, double y, double speedX, double speedY, double size, Color color) {
        this.x = x;
        this.y = y;
        this.speedX = speedX;
        this.speedY = speedY;
        this.size = size;
        this.color = color;
    }
    
    public void draw(GraphicsContext gc) {
        gc.setFill(color);
        gc.fillOval(x, y, size, size);
    }
    
    public void update() {
        x += speedX;
        y += speedY;
    }
    
    public void bounceX() {
        speedX *= -1;
    }
    
    public void bounceY() {
        speedY *= -1;
    }
    
    public void bounceWithRandomness(double randomness) {
        speedY *= -1;
        speedX += (Math.random() - 0.5) * randomness;
    }
    
    public void checkBoundaries(double width, double height) {
        if (x <= 0 || x + size >= width) {
            bounceX();
        }
        if (y <= 0 || y + size >= height) {
            bounceY();
        }
    }
    
    public void resetPosition(double width, double height) {
        x = width / 2;
        y = height / 2;
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeedX() { return speedX; }
    public double getSpeedY() { return speedY; }
    public double getSize() { return size; }
    
    // Setters
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public void setSpeed(double speedX, double speedY) {
        this.speedX = speedX;
        this.speedY = speedY;
    }

    public boolean collidesWith(double rectX, double rectY, double rectWidth, double rectHeight) {
        return x + size >= rectX && 
               x <= rectX + rectWidth && 
               y + size >= rectY && 
               y <= rectY + rectHeight;
    }
}