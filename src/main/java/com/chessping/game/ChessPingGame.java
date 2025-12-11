package com.chessping.game;

import com.chessping.models.pieces.*;
import com.chessping.models.*;
import com.chessping.networks.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ChessPingGame {
    private ChessBoard board;
    private Ball ball;
    private Player whitePlayer;
    private Player blackPlayer;
    private boolean isGameRunning;
    private GameConfig config;
    
    public ChessPingGame(GameConfig config, int tileSize) {
        this.config = config;
        this.board = new ChessBoard(config.pieceLevel, 8, tileSize);
        this.ball = new Ball(200, 300, 20, 20, 20, Color.YELLOW);
        this.isGameRunning = false;
        
        // Initialiser les joueurs avec leurs raquettes
        Paddle whitePaddle = new Paddle(
            (config.pieceLevel - 1) / 2.0 * tileSize + tileSize / 2 - tileSize * 1.5 / 2,
            2 * tileSize + 20,
            tileSize * 1.5,
            15,
            Color.RED,
            true,
            "Joueur 1"
        );
        
        Paddle blackPaddle = new Paddle(
            (config.pieceLevel - 1) / 2.0 * tileSize + tileSize / 2 - tileSize * 1.5 / 2,
            6 * tileSize - 35,
            tileSize * 1.5,
            15,
            Color.DARKBLUE,
            false,
            "Joueur 2"
        );
        
        this.whitePlayer = new Player("Joueur 1", true, whitePaddle);
        this.blackPlayer = new Player("Joueur 2", false, blackPaddle);
        
        initializeGame();
    }
    
    private void initializeGame() {
        board.initialize(
            config.pieceLevel,
            config.kingLife,
            config.queenLife,
            config.knightLife,
            config.pawnLife
        );
        ball.resetPosition(config.pieceLevel * 85, 8 * 85);
        ball.setSpeed(3, 3);
    }
    
    public void update() {
        if (!isGameRunning) return;
        
        ball.update();
        
        // Vérifier les collisions avec les bords
        ball.checkBoundaries(config.pieceLevel * 85, 8 * 85);
        
        // Vérifier les collisions avec les raquettes
        checkPaddleCollisions();
        
        // Vérifier les collisions avec les pièces
        checkPieceCollisions();
    }
    
    private void checkPaddleCollisions() {
        // Méthode utilitaire pour vérifier la collision balle/raquette
        if (ballCollidesWith(
            whitePlayer.getPaddle().getX(),
            whitePlayer.getPaddle().getY(),
            whitePlayer.getPaddle().getWidth(),
            whitePlayer.getPaddle().getHeight()
        )) {
            ball.bounceY();
        }
        
        if (ballCollidesWith(
            blackPlayer.getPaddle().getX(),
            blackPlayer.getPaddle().getY(),
            blackPlayer.getPaddle().getWidth(),
            blackPlayer.getPaddle().getHeight()
        )) {
            ball.bounceY();
        }
    }
    
    private boolean ballCollidesWith(double x, double y, double width, double height) {
        return ball.getX() + ball.getSize() >= x && 
               ball.getX() <= x + width && 
               ball.getY() + ball.getSize() >= y && 
               ball.getY() <= y + height;
    }
    
    private void checkPieceCollisions() {
        int col = (int)(ball.getX() / 85);
        int row = (int)(ball.getY() / 85);
        
        Piece piece = board.getPieceAt(row, col);
        if (piece != null && piece.isAlive()) {
            piece.takeDamage(1);
            ball.bounceWithRandomness(0.4);
            
            if (!piece.isAlive()) {
                board.removePiece(piece);
            }
        }
    }
    
    public void draw(GraphicsContext gc) {
        board.draw(gc);
        
        // Draw middle area first (white rectangle where paddles move)
        gc.setFill(Color.web("#fff"));
        gc.fillRect(0, 2 * 85, config.pieceLevel * 85, 4 * 85);
        
        // Draw paddles and ball on top of everything
        whitePlayer.getPaddle().draw(gc);
        blackPlayer.getPaddle().draw(gc);
        ball.draw(gc);
    }
    
    public void moveWhitePaddle(double deltaX, double deltaY) {
        whitePlayer.getPaddle().move(
            deltaX, deltaY, 
            config.pieceLevel, 85,
            2 * 85 - 20, 2 * 85 + 50
        );
    }
    
    public void moveBlackPaddle(double deltaX, double deltaY) {
        blackPlayer.getPaddle().move(
            deltaX, deltaY,
            config.pieceLevel, 85,
            445, 505
        );
    }
    
    public void restart() {
        initializeGame();
        whitePlayer.getPaddle().setPosition(
            (config.pieceLevel - 1) / 2.0 * 85 + 85 / 2 - 85 * 1.5 / 2,
            2 * 85 + 20
        );
        blackPlayer.getPaddle().setPosition(
            (config.pieceLevel - 1) / 2.0 * 85 + 85 / 2 - 85 * 1.5 / 2,
            6 * 85 - 35
        );
        board.resetPieces();
    }
    
    public boolean checkGameOver() {
        boolean whiteKingAlive = board.isKingAlive(true);
        boolean blackKingAlive = board.isKingAlive(false);
        
        if (!whiteKingAlive) {
            blackPlayer.incrementScore(1);
            return true;
        } else if (!blackKingAlive) {
            whitePlayer.incrementScore(1);
            return true;
        }
        return false;
    }
    
    // Getters
    public boolean isGameRunning() { return isGameRunning; }
    public Player getWhitePlayer() { return whitePlayer; }
    public Player getBlackPlayer() { return blackPlayer; }
    public Ball getBall() { return ball; }
    public ChessBoard getBoard() { return board; }
    
    // Setters
    public void setGameRunning(boolean gameRunning) { isGameRunning = gameRunning; }
    public void setWhitePlayerName(String name) { whitePlayer.setName(name); }
    public void setBlackPlayerName(String name) { blackPlayer.setName(name); }
}