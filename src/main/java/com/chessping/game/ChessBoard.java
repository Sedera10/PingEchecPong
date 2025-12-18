package com.chessping.game;

import com.chessping.models.pieces.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class ChessBoard {
    private int width;
    private int height;
    private int tileSize;
    private Piece[][] board;
    private List<Piece> pieces;
    
    public ChessBoard(int width, int height, int tileSize) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.board = new Piece[height][width];
        this.pieces = new ArrayList<>();
    }
    
    public void initialize(int pieceLevel, int kingLife, int queenLife, int knightLife, int pawnLife, int bishopLife, int rookLife) {
        clearBoard();
        pieces.clear();
        
        if (pieceLevel == 2) {
            initialize2Pieces(kingLife, queenLife, pawnLife);
        } else if (pieceLevel == 4) {
            initialize4Pieces(kingLife, queenLife, bishopLife, pawnLife);
        } else if (pieceLevel == 6) {
            initialize6Pieces(kingLife, queenLife, knightLife, bishopLife, pawnLife);
        } else if (pieceLevel == 8) {
            initialize8Pieces(kingLife, queenLife, knightLife, bishopLife, rookLife, pawnLife);
        }
    }
    
    private void initialize2Pieces(int kingLife, int queenLife, int pawnLife) {
        // Pièces blanches
        addPiece(new Queen(true, 0, 0, queenLife));
        addPiece(new King(true, 0, 1, kingLife));
        addPiece(new Pawn(true, 1, 0, pawnLife));
        addPiece(new Pawn(true, 1, 1, pawnLife));
        
        // Pièces noires
        addPiece(new Queen(false, 7, 0, queenLife));
        addPiece(new King(false, 7, 1, kingLife));
        addPiece(new Pawn(false, 6, 0, pawnLife));
        addPiece(new Pawn(false, 6, 1, pawnLife));
    }
    
    private void initialize4Pieces(int kingLife, int queenLife, int bishopLife, int pawnLife) {
        for (int i = 0; i < 4; i++) {
            addPiece(new Pawn(true, 1, i, pawnLife));
            addPiece(new Pawn(false, 6, i, pawnLife));
        }
        
        addPiece(new Bishop(true, 0, 0, bishopLife));
        addPiece(new Queen(true, 0, 1, queenLife));
        addPiece(new King(true, 0, 2, kingLife));
        addPiece(new Bishop(true, 0, 3, bishopLife));
        
        addPiece(new Bishop(false, 7, 0, bishopLife));
        addPiece(new Queen(false, 7, 1, queenLife));
        addPiece(new King(false, 7, 2, kingLife));
        addPiece(new Bishop(false, 7, 3, bishopLife));
    }
    
    private void initialize6Pieces(int kingLife, int queenLife, int knightLife, int bishopLife, int pawnLife) {
        for (int i = 0; i < 6; i++) {
            addPiece(new Pawn(true, 1, i, pawnLife));
            addPiece(new Pawn(false, 6, i, pawnLife));
        }
        
        addPiece(new Knight(true, 0, 0, knightLife));
        addPiece(new Bishop(true, 0, 1, bishopLife));
        addPiece(new Queen(true, 0, 2, queenLife));
        addPiece(new King(true, 0, 3, kingLife));
        addPiece(new Bishop(true, 0, 4, bishopLife));
        addPiece(new Knight(true, 0, 5, knightLife));
        
        addPiece(new Knight(false, 7, 0, knightLife));
        addPiece(new Bishop(false, 7, 1, bishopLife));
        addPiece(new Queen(false, 7, 2, queenLife));
        addPiece(new King(false, 7, 3, kingLife));
        addPiece(new Bishop(false, 7, 4, bishopLife));
        addPiece(new Knight(false, 7, 5, knightLife));
    }
    
    private void initialize8Pieces(int kingLife, int queenLife, int knightLife, int bishopLife, int rookLife, int pawnLife) {
        for (int i = 0; i < 8; i++) {
            addPiece(new Pawn(true, 1, i, pawnLife));
            addPiece(new Pawn(false, 6, i, pawnLife));
        }
        
        // Pièces blanches
        addPiece(new Rook(true, 0, 0, rookLife));
        addPiece(new Knight(true, 0, 1, knightLife));
        addPiece(new Bishop(true, 0, 2, bishopLife));
        addPiece(new Queen(true, 0, 3, queenLife));
        addPiece(new King(true, 0, 4, kingLife));
        addPiece(new Bishop(true, 0, 5, bishopLife));
        addPiece(new Knight(true, 0, 6, knightLife));
        addPiece(new Rook(true, 0, 7, rookLife));
        
        // Pièces noires
        addPiece(new Rook(false, 7, 0, rookLife));
        addPiece(new Knight(false, 7, 1, knightLife));
        addPiece(new Bishop(false, 7, 2, bishopLife));
        addPiece(new Queen(false, 7, 3, queenLife));
        addPiece(new King(false, 7, 4, kingLife));
        addPiece(new Bishop(false, 7, 5, bishopLife));
        addPiece(new Knight(false, 7, 6, knightLife));
        addPiece(new Rook(false, 7, 7, rookLife));
    }
    
    private void addPiece(Piece piece) {
        int row = piece.getRow();
        int col = piece.getCol();
        if (row >= 0 && row < height && col >= 0 && col < width) {
            board[row][col] = piece;
            pieces.add(piece);
        }
    }
    
    public void clearBoard() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                board[i][j] = null;
            }
        }
        pieces.clear();
    }
    
    public void draw(GraphicsContext gc) {
        drawTiles(gc);
        drawPieces(gc);
    }
    
    private void drawTiles(GraphicsContext gc) {
        Color lightSquare = Color.rgb(245, 245, 220);
        Color darkSquare = Color.rgb(222, 184, 135);
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                boolean light = (row + col) % 2 == 0;
                gc.setFill(light ? lightSquare : darkSquare);
                gc.fillRect(col * tileSize, row * tileSize, tileSize, tileSize);
            }
        }
    }
    
    private void drawPieces(GraphicsContext gc) {
        for (Piece piece : pieces) {
            if (piece.isAlive()) {
                piece.draw(gc, tileSize);
            }
        }
    }
    
    public Piece getPieceAt(int row, int col) {
        if (row >= 0 && row < height && col >= 0 && col < width) {
            return board[row][col];
        }
        return null;
    }
    
    public void removePiece(Piece piece) {
        int row = piece.getRow();
        int col = piece.getCol();
        if (row >= 0 && row < height && col >= 0 && col < width) {
            board[row][col] = null;
            pieces.remove(piece);
        }
    }

    // Public helper to place a piece on the board (used when applying network state)
    public void placePiece(Piece piece) {
        int row = piece.getRow();
        int col = piece.getCol();
        if (row >= 0 && row < height && col >= 0 && col < width) {
            // remove existing piece at that location if any
            if (board[row][col] != null) {
                pieces.remove(board[row][col]);
            }
            board[row][col] = piece;
            pieces.add(piece);
        }
    }
    
    public List<Piece> getPieces() {
        return new ArrayList<>(pieces);
    }
    
    public boolean isKingAlive(boolean whiteKing) {
        for (Piece piece : pieces) {
            if (piece instanceof King && piece.isWhite() == whiteKing && piece.isAlive()) {
                return true;
            }
        }
        return false;
    }
    
    public void resetPieces() {
        for (Piece piece : pieces) {
            piece.resetLife();
        }
    }
}