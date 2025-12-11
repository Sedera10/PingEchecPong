package com.chessping;

import javafx.application.Application;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import com.chessping.networks.*;
import com.chessping.game.ChessPingGame;
import com.chessping.game.Ball;
import java.net.*;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import javafx.application.Platform;
import com.chessping.networks.*;
import com.chessping.models.pieces.*;
import com.chessping.models.*;

public class ChessPingApp extends Application {
    
    private Stage configStage;
    private AnimationTimer gameTimer;
    private ToggleButton playPauseButton;
    
    private static final int TILE_SIZE = 85;
    
    private Canvas canvas;
    private Set<KeyCode> pressedKeys = new HashSet<>();
    
    private GameConfig config = new GameConfig();
    private GameServer gameServer;
    private GameClient gameClient;
    private boolean isHost = false;
    private boolean isClientConnected = false;
    private String playerName;
    private boolean isWhitePlayer = true;
    // pour la service de balle
    private boolean awaitingServe = false;
    private boolean isAiming = false;
    private double aimStartX, aimStartY, aimCurrentX, aimCurrentY;
    
    private ExecutorService networkExecutor = Executors.newCachedThreadPool();
    private ChessPingGame game;
    
    @Override
    public void start(Stage primaryStage) {
        showConfigurationWindow(primaryStage);
    }

    private void setupMouseControls() {
        canvas.setOnMousePressed(e -> {
            if (!awaitingServe) return;
            double mx = e.getX();
            double my = e.getY();
            Ball b = game.getBall();
            double bx = b.getX();
            double by = b.getY();
            double size = b.getSize();
            // Check if click is on the ball
            if (mx >= bx && mx <= bx + size && my >= by && my <= by + size) {
                // If we're awaiting a serve and clicked the ball, begin aiming
                isAiming = true;
                aimStartX = mx;
                aimStartY = my;
                aimCurrentX = mx;
                aimCurrentY = my;
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (!isAiming) return;
            aimCurrentX = e.getX();
            aimCurrentY = e.getY();
        });

        canvas.setOnMouseReleased(e -> {
            if (!isAiming) return;
            isAiming = false;
            // compute direction vector from ball center to release point
            Ball b = game.getBall();
            double bx = b.getX() + b.getSize() / 2;
            double by = b.getY() + b.getSize() / 2;
            double dx = aimCurrentX - bx;
            double dy = aimCurrentY - by;
            double len = Math.hypot(dx, dy);
            if (len < 1) return; // avoid zero-length
            double nx = dx / len;
            double ny = dy / len;

            // scale to desired initial speed
            double speed = 4.0; // can adjust
            double vx = nx * speed;
            double vy = ny * speed;

            // If local mode or host, perform launch authoritatively; if network client, send request to host
            if (config.networkConfig.getMode() == GameMode.LOCAL || isHost) {
                launchBall(vx, vy, isWhitePlayer);
                // broadcast immediately so clients see the launch without wait
                if (gameServer != null) {
                    GameState gs = createGameState();
                    gameServer.broadcastGameState(gs);
                }
            } else {
                // Network Client: send a ServeRequest to server
                if (gameClient != null) {
                    ServeRequest req = new ServeRequest(vx, vy, isWhitePlayer, playerName);
                    gameClient.sendServeRequest(req);
                }
            }
        });
    }
    
    private void showConfigurationWindow(Stage mainStage) {
        configStage = new Stage();
        configStage.setTitle("Configuration du ChessPing");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #f0f0f0;");
        
        int row = 0;
        
        // ========== SECTION 1: MODE DE JEU ==========
        Label modeLabel = new Label("Mode de jeu :");
        ToggleGroup modeGroup = new ToggleGroup();
        
        RadioButton localMode = new RadioButton("Local");
        RadioButton hostMode = new RadioButton("Héberger une partie");
        RadioButton joinMode = new RadioButton("Rejoindre une partie");
        
        localMode.setToggleGroup(modeGroup);
        hostMode.setToggleGroup(modeGroup);
        joinMode.setToggleGroup(modeGroup);
        localMode.setSelected(true);
        
        VBox modeOptions = new VBox(5);
        modeOptions.getChildren().addAll(localMode, hostMode, joinMode);
        
        grid.add(modeLabel, 0, row);
        grid.add(modeOptions, 1, row);
        row++;
        
        // ========== SECTION 2: CONFIGURATION RÉSEAU (masquée par défaut) ==========
        VBox networkConfigBox = new VBox(10);
        networkConfigBox.setVisible(false);
        networkConfigBox.setManaged(false);
        
        // Configuration pour héberger
        VBox hostConfig = new VBox(5);
        hostConfig.setVisible(false);
        hostConfig.setManaged(false);
        
        Label portLabel = new Label("Port serveur :");
        TextField portField = new TextField("5555");
        portField.setPrefWidth(100);
        
        Label playerNameLabel = new Label("Votre nom :");
        TextField hostNameField = new TextField("Joueur1");
        hostNameField.setPrefWidth(150);
        
        hostConfig.getChildren().addAll(portLabel, portField, playerNameLabel, hostNameField);
        
        // Configuration pour rejoindre
        VBox joinConfig = new VBox(5);
        joinConfig.setVisible(false);
        joinConfig.setManaged(false);
        
        Label ipLabel = new Label("Adresse IP :");
        TextField ipField = new TextField("localhost");
        ipField.setPrefWidth(150);
        
        Label joinPortLabel = new Label("Port :");
        TextField joinPortField = new TextField("5555");
        joinPortField.setPrefWidth(100);
        
        Label joinNameLabel = new Label("Votre nom :");
        TextField joinNameField = new TextField("Joueur2");
        joinNameField.setPrefWidth(150);
        
        joinConfig.getChildren().addAll(ipLabel, ipField, joinPortLabel, joinPortField, joinNameLabel, joinNameField);
        
        networkConfigBox.getChildren().addAll(hostConfig, joinConfig);
        
        grid.add(networkConfigBox, 1, row);
        row++;
        
        // ========== SECTION 3: NOMBRE DE PIÈCES ==========
        Label piecesLabel = new Label("Nombre de pièces :");
        ToggleGroup piecesGroup = new ToggleGroup();
        RadioButton pieces2 = new RadioButton("2");
        RadioButton pieces4 = new RadioButton("4");
        RadioButton pieces6 = new RadioButton("6");
        RadioButton pieces8 = new RadioButton("8 (défaut)");
        
        pieces2.setToggleGroup(piecesGroup);
        pieces4.setToggleGroup(piecesGroup);
        pieces6.setToggleGroup(piecesGroup);
        pieces8.setToggleGroup(piecesGroup);
        pieces8.setSelected(true);
        
        HBox piecesBox = new HBox(10, pieces2, pieces4, pieces6, pieces8);
        grid.add(piecesLabel, 0, row);
        grid.add(piecesBox, 1, row);
        row++;
        
        // ========== SECTION 4: VIE DES PIÈCES ==========
        Label lifeLabel = new Label("Vie des pièces :");
        Spinner<Integer> kingSpinner = new Spinner<>(1, 10, 5);
        Spinner<Integer> queenSpinner = new Spinner<>(1, 10, 4);
        Spinner<Integer> knightSpinner = new Spinner<>(1, 10, 2);
        Spinner<Integer> pawnSpinner = new Spinner<>(1, 10, 1);
        
        kingSpinner.setEditable(true);
        queenSpinner.setEditable(true);
        knightSpinner.setEditable(true);
        pawnSpinner.setEditable(true);
        
        VBox lifeBox = new VBox(5,
            new HBox(10, new Label("Roi:"), kingSpinner),
            new HBox(10, new Label("Reine:"), queenSpinner),
            new HBox(10, new Label("Cavalier:"), knightSpinner),
            new HBox(10, new Label("Pion:"), pawnSpinner)
        );
        
        grid.add(lifeLabel, 0, row);
        grid.add(lifeBox, 1, row);
        row++;
        
        // ========== GESTION DES ÉVÉNEMENTS ==========
        // Gestion des changements de mode
        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == localMode) {
                networkConfigBox.setVisible(false);
                networkConfigBox.setManaged(false);
                lifeBox.setDisable(false);
                piecesBox.setDisable(false);
            } else {
                networkConfigBox.setVisible(true);
                networkConfigBox.setManaged(true);
                
                if (newVal == hostMode) {
                    hostConfig.setVisible(true);
                    hostConfig.setManaged(true);
                    joinConfig.setVisible(false);
                    joinConfig.setManaged(false);
                    lifeBox.setDisable(false);
                    piecesBox.setDisable(false);
                } else if (newVal == joinMode) {
                    hostConfig.setVisible(false);
                    hostConfig.setManaged(false);
                    joinConfig.setVisible(true);
                    joinConfig.setManaged(true);
                    lifeBox.setDisable(true);
                    piecesBox.setDisable(true);
                }
            }
        });
        
        // ========== BOUTONS ==========
        HBox buttonBox = new HBox(10);
        
        Button startButton = new Button("Commencer");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        
        Button cancelButton = new Button("Annuler");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        cancelButton.setOnAction(e -> configStage.close());
        
        buttonBox.getChildren().addAll(startButton, cancelButton);
        
        grid.add(buttonBox, 1, row);
        
        // ========== ACTION DU BOUTON COMMENCER ==========
        startButton.setOnAction(e -> {
            // Récupération du mode de jeu
            GameMode selectedMode = GameMode.LOCAL;
            if (hostMode.isSelected()) {
                selectedMode = GameMode.HOST;
            } else if (joinMode.isSelected()) {
                selectedMode = GameMode.JOIN;
            }
            
            // Récupération des configurations
            NetworkConfig networkConfig = new NetworkConfig();
            networkConfig.setMode(selectedMode);
            
            if (selectedMode == GameMode.HOST) {
                try {
                    networkConfig.setPort(Integer.parseInt(portField.getText()));
                    networkConfig.setPlayerName(hostNameField.getText());
                } catch (NumberFormatException ex) {
                    showAlert("Erreur", "Port invalide", "Veuillez entrer un numéro de port valide.");
                    return;
                }
            } else if (selectedMode == GameMode.JOIN) {
                networkConfig.setServerIp(ipField.getText());
                try {
                    networkConfig.setPort(Integer.parseInt(joinPortField.getText()));
                    networkConfig.setPlayerName(joinNameField.getText());
                } catch (NumberFormatException ex) {
                    showAlert("Erreur", "Port invalide", "Veuillez entrer un numéro de port valide.");
                    return;
                }
            }
            
            // Récupération des configurations de pièces (uniquement en local/host)
            if (selectedMode != GameMode.JOIN) {
                if (pieces2.isSelected()) config.pieceLevel = 2;
                else if (pieces4.isSelected()) config.pieceLevel = 4;
                else if (pieces6.isSelected()) config.pieceLevel = 6;
                else config.pieceLevel = 8;
                
                config.kingLife = kingSpinner.getValue();
                config.queenLife = queenSpinner.getValue();
                config.knightLife = knightSpinner.getValue();
                config.pawnLife = pawnSpinner.getValue();
            }
            
            config.networkConfig = networkConfig;
            configStage.close();
            startGame(mainStage);
        });
        
        Scene configScene = new Scene(grid, 500, 500);
        configStage.setScene(configScene);
        configStage.showAndWait();
    }
    
    private void startGame(Stage stage) {
        playerName = config.networkConfig.getPlayerName();
        
        // Créer le jeu
        game = new ChessPingGame(config, TILE_SIZE);
        
        // Configurer réseau
        if (config.networkConfig.getMode() == GameMode.HOST) {
            isHost = true;
            isWhitePlayer = true;
            game.setWhitePlayerName(playerName);
            startGameServer(config.networkConfig.getPort());
        } else if (config.networkConfig.getMode() == GameMode.JOIN) {
            isHost = false;
            isWhitePlayer = false;
            game.setBlackPlayerName(playerName);
            connectToGameServer(config.networkConfig.getServerIp(), config.networkConfig.getPort());
        } else {
            // Mode local
            game.setWhitePlayerName("Joueur 1");
            game.setBlackPlayerName("Joueur 2");
        }
        
        // Configuration de l'interface
        canvas = new Canvas(config.pieceLevel * TILE_SIZE, 8 * TILE_SIZE);
        
        BorderPane root = new BorderPane();
        VBox leftPanel = createLeftPanel();
        root.setLeft(leftPanel);
        root.setCenter(canvas);
        
        Scene scene = new Scene(root, 1200, 700);
        setupKeyControls(scene);
        setupMouseControls();
        
        stage.setTitle("ChessPing Game - " + (config.isNetworkMode() ? "Mode Réseau" : "Mode Local"));
        stage.setScene(scene);
        stage.show();
        setupStageCloseHandler(stage);
        
        drawGame();
    }
    
    private void setupKeyControls(Scene scene) {
        scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
        
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            pressedKeys.add(e.getCode());
            handleKeyPress(e.getCode());
        });
        
        canvas.addEventFilter(KeyEvent.KEY_PRESSED, e -> pressedKeys.add(e.getCode()));
        canvas.addEventFilter(KeyEvent.KEY_RELEASED, e -> pressedKeys.remove(e.getCode()));
    }
    
    private void handleKeyPress(KeyCode code) {
        double speed = 0.20 * TILE_SIZE;
        
        if (config.networkConfig.getMode() == GameMode.LOCAL || isHost) {
            // Contrôles locaux ou host
            if (code == KeyCode.A || code == KeyCode.Q) {
                game.moveWhitePaddle(-speed, 0);
            } else if (code == KeyCode.D) {
                game.moveWhitePaddle(speed, 0);
            } else if (code == KeyCode.W || code == KeyCode.Z) {
                game.moveWhitePaddle(0, -speed);
            } else if (code == KeyCode.S) {
                game.moveWhitePaddle(0, speed);
            } else if (code == KeyCode.LEFT) {
                game.moveBlackPaddle(-speed, 0);
            } else if (code == KeyCode.RIGHT) {
                game.moveBlackPaddle(speed, 0);
            } else if (code == KeyCode.UP) {
                game.moveBlackPaddle(0, -speed);
            } else if (code == KeyCode.DOWN) {
                game.moveBlackPaddle(0, speed);
            }
        } else if (isClientConnected) {
            // Client réseau
            if (isWhitePlayer) {
                if (code == KeyCode.A || code == KeyCode.Q) {
                    game.moveWhitePaddle(-speed, 0);
                } else if (code == KeyCode.D) {
                    game.moveWhitePaddle(speed, 0);
                } else if (code == KeyCode.W || code == KeyCode.Z) {
                    game.moveWhitePaddle(0, -speed);
                } else if (code == KeyCode.S) {
                    game.moveWhitePaddle(0, speed);
                }
            } else {
                if (code == KeyCode.LEFT) {
                    game.moveBlackPaddle(-speed, 0);
                } else if (code == KeyCode.RIGHT) {
                    game.moveBlackPaddle(speed, 0);
                } else if (code == KeyCode.UP) {
                    game.moveBlackPaddle(0, -speed);
                } else if (code == KeyCode.DOWN) {
                    game.moveBlackPaddle(0, speed);
                }
            }
        }
        
        // Envoyer mise à jour réseau si nécessaire
        if (config.isNetworkMode() && !isHost && isClientConnected) {
            sendPaddleUpdate();
        }
    }
    
    private void startGameAnimation() {
        gameTimer = new AnimationTimer() {
            long last = 0;
            long lastNetworkUpdate = 0;
            
            @Override
            public void handle(long now) {
                // Allow animation to run either while game is running or while awaiting a serve
                if (!game.isGameRunning() && !awaitingServe) return;

                if (now - last < 16_000_000) return;
                last = now;

                if (game.isGameRunning()) {
                    // Mettre à jour le jeu
                    game.update();

                    // Vérifier fin de partie
                    if (game.checkGameOver()) {
                        game.setGameRunning(false);
                        awaitingServe = false;
                        playPauseButton.setSelected(false);
                        playPauseButton.setText("Play");
                        drawGameOver();
                    }
                }

                // Dessiner (also when awaiting serve so aim overlay appears)
                drawGame();

                // Synchronisation réseau
                if (config.isNetworkMode()) {
                    if (now - lastNetworkUpdate > 50_000_000) {
                        updateNetworkGameState();
                        lastNetworkUpdate = now;
                    }
                }
            }
        };
        gameTimer.start();
    }
    
    private void drawGame() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        game.draw(gc);
        
        // Draw aiming arrow overlay when user is aiming
        if (isAiming) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(3);
            double bx = game.getBall().getX() + game.getBall().getSize() / 2;
            double by = game.getBall().getY() + game.getBall().getSize() / 2;
            gc.strokeLine(bx, by, aimCurrentX, aimCurrentY);
            // small arrow head
            double angle = Math.atan2(aimCurrentY - by, aimCurrentX - bx);
            double arrowSize = 10;
            double ax1 = aimCurrentX - arrowSize * Math.cos(angle - Math.PI / 6);
            double ay1 = aimCurrentY - arrowSize * Math.sin(angle - Math.PI / 6);
            double ax2 = aimCurrentX - arrowSize * Math.cos(angle + Math.PI / 6);
            double ay2 = aimCurrentY - arrowSize * Math.sin(angle + Math.PI / 6);
            gc.strokeLine(aimCurrentX, aimCurrentY, ax1, ay1);
            gc.strokeLine(aimCurrentX, aimCurrentY, ax2, ay2);
        }
    }
    
    private void drawGameOver() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFont(Font.font(48));
        
        if (!game.getBoard().isKingAlive(true)) {
            gc.setFill(Color.RED);
            gc.fillText("NOIR GAGNE!", canvas.getWidth()/2 - 120, canvas.getHeight()/2);
        } else if (!game.getBoard().isKingAlive(false)) {
            gc.setFill(Color.BLUE);
            gc.fillText("BLANC GAGNE!", canvas.getWidth()/2 - 120, canvas.getHeight()/2);
        }
    }
    
    // Méthodes réseau - rendre publique pour GameServer
    public void startGameServer(int port) {
        networkExecutor.submit(() -> {
            try {
                gameServer = new GameServer(port, this);
                gameServer.start();
                System.out.println("Serveur démarré sur le port " + port);
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showAlert("Erreur", "Impossible de démarrer le serveur", e.getMessage());
                });
            }
        });
    }
    
    public void connectToGameServer(String ip, int port) {
        networkExecutor.submit(() -> {
            try {
                gameClient = new GameClient(ip, port, this);
                gameClient.connect();
                isClientConnected = true;
                System.out.println("Connecté au serveur " + ip + ":" + port);
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showAlert("Erreur", "Connexion impossible", e.getMessage());
                });
            }
        });
    }
    
    // Rendre cette méthode publique pour GameServer
    public GameState createGameState() {
        GameState gs = new GameState();
        
        // Configuration
        gs.pieceLevel = config.pieceLevel;
        gs.kingLife = config.kingLife;
        gs.queenLife = config.queenLife;
        gs.knightLife = config.knightLife;
        gs.pawnLife = config.pawnLife;
        
        // État du jeu
        gs.whitePaddleX = game.getWhitePlayer().getPaddle().getX();
        gs.whitePaddleY = game.getWhitePlayer().getPaddle().getY();
        gs.blackPaddleX = game.getBlackPlayer().getPaddle().getX();
        gs.blackPaddleY = game.getBlackPlayer().getPaddle().getY();
        gs.ballX = game.getBall().getX();
        gs.ballY = game.getBall().getY();
        gs.ballSpeedX = game.getBall().getSpeedX();
        gs.ballSpeedY = game.getBall().getSpeedY();
        gs.gameRunning = game.isGameRunning();
        
        // Sérialiser le plateau et la vie des pièces pour que les clients puissent
        // reproduire exactement l'état du plateau.
        int height = 8;
        int width = config.pieceLevel;
        gs.boardPieces = new String[height][width];
        gs.piecesLife = new GameState.PieceLifeInfo[height][width];

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                com.chessping.models.pieces.Piece p = game.getBoard().getPieceAt(r, c);
                if (p != null) {
                    gs.boardPieces[r][c] = p.getSymbol();
                    gs.piecesLife[r][c] = new GameState.PieceLifeInfo(p.getSymbol(), p.getLife(), p.isWhite());
                } else {
                    gs.boardPieces[r][c] = null;
                    gs.piecesLife[r][c] = null;
                }
            }
        }

        return gs;
    }
    
    // Méthode pour recevoir les mises à jour du serveur
    public void receiveNetworkUpdate(GameState gameState) {
        Platform.runLater(() -> {
            if (!isHost) {
                applyNetworkGameState(gameState);
            }
        });
    }
    
    private void applyNetworkGameState(GameState gameState) {
        if (gameState == null) return;
        
        // Appliquer la configuration
        config.pieceLevel = gameState.pieceLevel;
        config.kingLife = gameState.kingLife;
        config.queenLife = gameState.queenLife;
        config.knightLife = gameState.knightLife;
        config.pawnLife = gameState.pawnLife;
        
        // Appliquer l'état du jeu
        game.getWhitePlayer().getPaddle().setPosition(gameState.whitePaddleX, gameState.whitePaddleY);
        game.getBlackPlayer().getPaddle().setPosition(gameState.blackPaddleX, gameState.blackPaddleY);
        game.getBall().setPosition(gameState.ballX, gameState.ballY);
        game.getBall().setSpeed(gameState.ballSpeedX, gameState.ballSpeedY);
        game.setGameRunning(gameState.gameRunning);

        // If the server started the game and this client doesn't have the animation running, start it
        if (gameState.gameRunning && gameTimer == null) {
            startGameAnimation();
        }
        
        // Mettre à jour l'interface
        if (playPauseButton != null) {
            playPauseButton.setSelected(gameState.gameRunning);
            playPauseButton.setText(gameState.gameRunning ? "Pause" : "Play");
        }
        
        // Redessiner
            drawGame();

        // Si l'état du plateau est présent, l'appliquer
        if (gameState.boardPieces != null && gameState.piecesLife != null) {
            // Si la taille du plateau a changé, recréer le jeu et redimensionner le canvas
            double desiredWidth = gameState.pieceLevel * TILE_SIZE;
            if (canvas.getWidth() != desiredWidth) {
                config.pieceLevel = gameState.pieceLevel;
                // recreate game with new config
                game = new ChessPingGame(config, TILE_SIZE);
                // resize canvas to match new board width
                canvas.setWidth(desiredWidth);
            }

            // Clear and place pieces according to incoming state
            game.getBoard().clearBoard();
            int height = Math.min(gameState.boardPieces.length, 8);
            for (int r = 0; r < height; r++) {
                int width = Math.min(gameState.boardPieces[r].length, config.pieceLevel);
                for (int c = 0; c < width; c++) {
                    GameState.PieceLifeInfo pli = gameState.piecesLife[r][c];
                    if (pli != null && pli.symbol != null) {
                        com.chessping.models.pieces.Piece piece = createPieceFromSymbol(pli.symbol, pli.isWhite, r, c, pli.life);
                        if (piece != null) {
                            game.getBoard().placePiece(piece);
                        }
                    } else {
                        // ensure null
                        com.chessping.models.pieces.Piece existing = game.getBoard().getPieceAt(r, c);
                        if (existing != null) game.getBoard().removePiece(existing);
                    }
                }
            }

            drawGame();
        }

        // Afficher l'écran de fin de partie si le plateau indique qu'un roi est mort
        try {
            boolean whiteKingAlive = game.getBoard().isKingAlive(true);
            boolean blackKingAlive = game.getBoard().isKingAlive(false);
            if (!whiteKingAlive || !blackKingAlive) {
                // S'assurer que la partie est en pause côté client et afficher le message
                game.setGameRunning(false);
                if (playPauseButton != null) {
                    playPauseButton.setSelected(false);
                    playPauseButton.setText("Play");
                }
                drawGameOver();
            }
        } catch (Exception ex) {
            // defensive: don't let a drawing error crash the network update
            System.err.println("Error while applying game-over display: " + ex.getMessage());
        }
    }
    
    private void updateNetworkGameState() {
        if (config.networkConfig.getMode() == GameMode.LOCAL) return;
        
        if (isHost) {
            // Host envoie l'état complet
            if (gameServer != null) {
                GameState gameState = createGameState();
                gameServer.broadcastGameState(gameState);
            }
        } else if (isClientConnected) {
            // Client envoie seulement sa position de raquette
            sendPaddleUpdate();
        }
    }

    private com.chessping.models.pieces.Piece createPieceFromSymbol(String symbol, boolean isWhite, int row, int col, int life) {
        if (symbol == null || symbol.isEmpty()) return null;
        char ch = symbol.charAt(0);
        switch (ch) {
            case '♔': case '♚':
                return new com.chessping.models.pieces.King(isWhite, row, col, life);
            case '♕': case '♛':
                return new com.chessping.models.pieces.Queen(isWhite, row, col, life);
            case '♖': case '♜':
                return new com.chessping.models.pieces.Rook(isWhite, row, col, life);
            case '♗': case '♝':
                return new com.chessping.models.pieces.Bishop(isWhite, row, col, life);
            case '♘': case '♞':
                return new com.chessping.models.pieces.Knight(isWhite, row, col, life);
            case '♙': case '♟':
                return new com.chessping.models.pieces.Pawn(isWhite, row, col, life);
            default:
                return null;
        }
    }
    
    private void sendPaddleUpdate() {
        if (gameClient != null) {
            PaddleUpdate update = new PaddleUpdate(
                isWhitePlayer ? game.getWhitePlayer().getPaddle().getX() : 
                              game.getBlackPlayer().getPaddle().getX(),
                isWhitePlayer ? game.getWhitePlayer().getPaddle().getY() : 
                              game.getBlackPlayer().getPaddle().getY(),
                isWhitePlayer,
                playerName
            );
            gameClient.sendPaddleUpdate(update);
        }
    }
    
    // Getters pour GameServer (remplacent les variables publiques)
    public double getWhitePaddleX() {
        return game.getWhitePlayer().getPaddle().getX();
    }
    
    public double getWhitePaddleY() {
        return game.getWhitePlayer().getPaddle().getY();
    }
    
    public double getBlackPaddleX() {
        return game.getBlackPlayer().getPaddle().getX();
    }
    
    public double getBlackPaddleY() {
        return game.getBlackPlayer().getPaddle().getY();
    }
    
    public void setWhitePaddlePosition(double x, double y) {
        game.getWhitePlayer().getPaddle().setPosition(x, y);
    }
    
    public void setBlackPaddlePosition(double x, double y) {
        game.getBlackPlayer().getPaddle().setPosition(x, y);
    }
    
    private void setupStageCloseHandler(Stage stage) {
        stage.setOnCloseRequest(ev -> {
            if (gameTimer != null) gameTimer.stop();
            if (gameServer != null) {
                try { gameServer.stop(); } catch (Exception ignored) {}
            }
            if (gameClient != null) {
                try { gameClient.disconnect(); } catch (Exception ignored) {}
            }
            networkExecutor.shutdownNow();
        });
    }

    // Position the ball just in front of the given player's paddle
    private void positionBallForServe(boolean white) {
        double paddleX = white ? game.getWhitePlayer().getPaddle().getX() : game.getBlackPlayer().getPaddle().getX();
        double paddleY = white ? game.getWhitePlayer().getPaddle().getY() : game.getBlackPlayer().getPaddle().getY();
        double paddleW = white ? game.getWhitePlayer().getPaddle().getWidth() : game.getBlackPlayer().getPaddle().getWidth();
        double paddleH = white ? game.getWhitePlayer().getPaddle().getHeight() : game.getBlackPlayer().getPaddle().getHeight();
        // Place ball in front of paddle (into play area, toward opponent)
        // White paddle is at top (around y=170), so ball goes DOWN toward middle
        // Black paddle is at bottom (around y=480), so ball goes UP toward middle
        double bx = paddleX + paddleW / 2 - game.getBall().getSize() / 2;
        double by = white ? (paddleY + paddleH + 5) : (paddleY - game.getBall().getSize() - 5);
        game.getBall().setPosition(bx, by);
        // ensure not running
        game.setGameRunning(false);
        awaitingServe = true;
        drawGame();
    }

    // Launch the ball with velocity (vx,vy). This is authoritative when called on the host.
    public void launchBall(double vx, double vy, boolean launchedByWhite) {
        // set ball speed and start the game
        game.getBall().setSpeed(vx, vy);
        game.setGameRunning(true);
        awaitingServe = false;
        // ensure animation running
        if (gameTimer == null) startGameAnimation();
        // update play button state
        if (playPauseButton != null) {
            playPauseButton.setSelected(true);
            playPauseButton.setText("Pause");
        }
    }
    
    private VBox createLeftPanel() {
        VBox vb = new VBox(15);
        vb.setPadding(new Insets(20));
        vb.setPrefWidth(300);
        vb.setStyle("-fx-background-color: #eae3d94c;");
        
        // Configuration
        VBox configInfo = new VBox(5);
        configInfo.setStyle("-fx-background-color: #2c3e50; -fx-padding: 10; -fx-background-radius: 5;");
        
        String modeText = config.isNetworkMode() ? "Mode Réseau" : "Mode Local";
        String lifeText = String.format("Roi:%d | Reine:%d | Cavalier:%d | Pion:%d",
            config.kingLife, config.queenLife, config.knightLife, config.pawnLife);
        
        configInfo.getChildren().addAll(
            createInfoText("Configuration actuelle:"),
            createInfoText("Pièces: " + config.pieceLevel),
            createInfoText(modeText),
            createInfoText("Vies: " + lifeText),
            createInfoText("Contrôles:"),
            createInfoText("Haut: ZQSD"),
            createInfoText("Bas: Flèches")
        );
        
        // Boutons
        VBox controlButtons = createControlButtons();
        
        // Reconfigurer
        Button reconfigBtn = new Button("Reconfigurer");
        reconfigBtn.setPrefWidth(150);
        reconfigBtn.setOnAction(e -> showConfigurationWindow((Stage)canvas.getScene().getWindow()));
        
        vb.getChildren().addAll(configInfo, controlButtons, reconfigBtn);
        return vb;
    }
    
    private VBox createControlButtons() {
        VBox buttonBox = new VBox(10);
        
        playPauseButton = new ToggleButton("Play");
        playPauseButton.setPrefWidth(150);
        playPauseButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        
        playPauseButton.setOnAction(e -> {
            if (playPauseButton.isSelected()) {
                // Enter awaiting-serve state: the game will actually start when a player launches the ball
                playPauseButton.setText("Pause");
                playPauseButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
                awaitingServe = true;
                game.setGameRunning(false);
                startGameAnimation();
                // place ball in front of the local player so they can serve
                positionBallForServe(isWhitePlayer);
                canvas.requestFocus();
            } else {
                playPauseButton.setText("Play");
                playPauseButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                awaitingServe = false;
                game.setGameRunning(false);
                if (gameTimer != null) gameTimer.stop();
            }
        });
        
        Button restart = new Button("Restart");
        restart.setPrefWidth(150);
        restart.setOnAction(e -> {
            game.setGameRunning(false);
            game.restart();
            playPauseButton.setSelected(false);
            playPauseButton.setText("Play");
            drawGame();
        });
        
        buttonBox.getChildren().addAll(playPauseButton, restart);
        return buttonBox;
    }
    
    private javafx.scene.text.Text createInfoText(String s) {
        javafx.scene.text.Text t = new javafx.scene.text.Text(s);
        t.setFill(Color.WHITE);
        t.setFont(Font.font(12));
        return t;
    }
    
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch();
    }
}