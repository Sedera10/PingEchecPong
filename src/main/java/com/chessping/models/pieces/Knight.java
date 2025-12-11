package com.chessping.models.pieces;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;


public class Knight extends Piece {
    public Knight(boolean isWhite, int row, int col, int life) {
        super(isWhite ? "♘" : "♞", life, isWhite, row, col);
    }
    
    @Override
    public void draw(GraphicsContext gc, int tileSize) {
        gc.setFont(javafx.scene.text.Font.font(60));
        gc.setFill(isWhite ? Color.BLACK : Color.GRAY);
        gc.fillText(symbol, col * tileSize + 10, row * tileSize + 60);
        drawLifeCircle(gc, tileSize);
    }
}
