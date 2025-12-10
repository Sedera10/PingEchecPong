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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.chessping.classes.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import javafx.application.Platform;

public class ChessPingApp extends Application {
    
    private static class PieceInfo {
        String symbol;
        int life;
        boolean isWhite;
        
        PieceInfo(String symbol, int life, boolean isWhite) {
            this.symbol = symbol;
            this.life = life;
            this.isWhite = isWhite;
        }
    }
    
    private Stage configStage;
    private AnimationTimer gameTimer;
    private boolean isGameRunning = false;
    private ToggleButton playPauseButton;
    
    private static final int TILE_SIZE = 85;
    private static final int VISIBLE_ROWS = 8;
    
    private int boardWidth;
    private Map<String, PieceInfo>[][] boardLife; // Stocke les informations de vie des pièces
    private String[][] board; // Stocke les symboles des pièces
    
    public double whitePaddleX;
    public double blackPaddleX;
    public double whitePaddleY = 2*TILE_SIZE + 20;
    public double blackPaddleY = 6*TILE_SIZE - 35;
    
    private Canvas canvas;
    private Set<KeyCode> pressedKeys = new HashSet<>();
    
    // ball variables
    private double ballX = 200;
    private double ballY = 300;
    private double ballSpeedX = 5;
    private double ballSpeedY = 5;
    private final double BALL_SIZE = 20;

    private GameConfig config = new GameConfig();
    private GameServer gameServer;
    private GameClient gameClient;
    private boolean isHost = false;
    private boolean isClientConnected = false;
    private String playerName;
    private String opponentName = "Adversaire";
    
    private double networkWhitePaddleX, networkWhitePaddleY;
    private double networkBlackPaddleX, networkBlackPaddleY;
    private double networkBallX, networkBallY;
    private double networkBallSpeedX, networkBallSpeedY;
    private boolean isWhitePlayer = true; // Le host joue les blancs par défaut
    
    private ExecutorService networkExecutor = Executors.newCachedThreadPool();
    
    @Override
    public void start(Stage primaryStage) {
        showConfigurationWindow(primaryStage);
    }
    // 1- fenetre de configuration
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

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void startGame(Stage stage) {

        playerName = config.networkConfig.getPlayerName();
        if (config.networkConfig.getMode() == GameMode.HOST) {
            isHost = true;
            isWhitePlayer = true;
            startGameServer(config.networkConfig.getPort());
        } else if (config.networkConfig.getMode() == GameMode.JOIN) {
            isHost = false;
            isWhitePlayer = false; // Le client joue les noirs
            connectToGameServer(config.networkConfig.getServerIp(), config.networkConfig.getPort());
        }

        boardWidth = config.pieceLevel;
        board = new String[8][boardWidth];
        boardLife = new HashMap[8][boardWidth];
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < boardWidth; j++) {
                boardLife[i][j] = new HashMap<>();
            }
        }
        
        canvas = new Canvas(boardWidth * TILE_SIZE, 8 * TILE_SIZE);
        
        initializeBoard();
        whitePaddleX = (boardWidth - 1) / 2.0;
        blackPaddleX = (boardWidth - 1) / 2.0;
        
        BorderPane root = new BorderPane();
        
        // Panel gauche avec configurations et boutons
        VBox leftPanel = createLeftPanel();
        root.setLeft(leftPanel);
        root.setCenter(canvas);
        
        Scene scene = new Scene(root, 1200, 700);
        
        scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
        
        stage.setTitle("ChessPing Game - " + (config.isNetworkMode() ? "Mode Réseau" : "Mode Local"));
        stage.setScene(scene);
        stage.show();
        // Ensure network resources are closed when window is closed
        stage.setOnCloseRequest(ev -> {
            if (gameServer != null) {
                try { gameServer.stop(); } catch (Exception ignored) {}
            }
            if (gameClient != null) {
                try { gameClient.disconnect(); } catch (Exception ignored) {}
            }
            networkExecutor.shutdownNow();
        });
        scene.getRoot().requestFocus();
        
        drawBoard(canvas.getGraphicsContext2D());
        // Use event filters so arrow keys and other keys are captured even
            // when some controls have focus (prevents focus traversal from swallowing keys).
            scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                pressedKeys.add(e.getCode());
                // Ensure canvas has focus when any game control key is pressed
                if (canvas != null && (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.RIGHT || 
                    e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN ||
                    e.getCode() == KeyCode.W || e.getCode() == KeyCode.Z ||
                    e.getCode() == KeyCode.A || e.getCode() == KeyCode.Q ||
                    e.getCode() == KeyCode.S || e.getCode() == KeyCode.D)) {
                    canvas.requestFocus();
                    // If we're a connected client, immediately update our local paddle
                    // and send the update to the host so the other side sees it even
                    // if Play hasn't been pressed locally.
                    if (config.isNetworkMode() && !isHost && isClientConnected) {
                        double speed = 0.12;
                        KeyCode code = e.getCode();
                        if (isWhitePlayer) {
                            if (code == KeyCode.A || code == KeyCode.Q) {
                                whitePaddleX = Math.max(0, whitePaddleX - speed);
                            } else if (code == KeyCode.D) {
                                whitePaddleX = Math.min(boardWidth - 1, whitePaddleX + speed);
                            } else if (code == KeyCode.W || code == KeyCode.Z) {
                                if (whitePaddleY - speed * TILE_SIZE > (TILE_SIZE * 2) -20) {
                                    whitePaddleY -= speed * TILE_SIZE;
                                }
                            } else if (code == KeyCode.S) {
                                if (whitePaddleY + speed * TILE_SIZE + 15 < (TILE_SIZE * 2) + 50) {
                                    whitePaddleY += speed * TILE_SIZE;
                                }
                            }
                        } else {
                            if (code == KeyCode.LEFT) {
                                blackPaddleX = Math.max(0, blackPaddleX - speed);
                            } else if (code == KeyCode.RIGHT) {
                                blackPaddleX = Math.min(boardWidth - 1, blackPaddleX + speed);
                            } else if (code == KeyCode.UP) {
                                if (blackPaddleY - speed * TILE_SIZE > 445) {
                                    blackPaddleY -= speed * TILE_SIZE;
                                }
                            } else if (code == KeyCode.DOWN) {
                                if (blackPaddleY + speed * TILE_SIZE + 15 < 505) {
                                    blackPaddleY += speed * TILE_SIZE;
                                }
                            }
                        }

                        double sx = isWhitePlayer ? whitePaddleX : blackPaddleX;
                        double sy = isWhitePlayer ? whitePaddleY : blackPaddleY;
                        sendPaddleUpdate(sx, sy, isWhitePlayer);
                    }
                }
            });
            scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> pressedKeys.remove(e.getCode()));

            // Also attach the same filters to the canvas so keys are captured when
            // the canvas has focus (robust against focus changes in the left panel).
            canvas.addEventFilter(KeyEvent.KEY_PRESSED, e -> pressedKeys.add(e.getCode()));
            canvas.addEventFilter(KeyEvent.KEY_RELEASED, e -> pressedKeys.remove(e.getCode()));
    }
    
    // 1) creation de panel gauche
    private VBox createLeftPanel() {
        VBox vb = new VBox(15);
        vb.setPadding(new Insets(20));
        vb.setPrefWidth(300);
        vb.setStyle("-fx-background-color: #eae3d94c;");
        
        // Affichage des configurations
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
        
        // Infos joueurs
        VBox players = new VBox(5);
        players.setStyle("-fx-background-color: #444; -fx-padding: 10; -fx-background-radius: 5;");
        
        // Boutons de contrôle en bas du panel gauche
        VBox controlButtons = createControlButtons();
        controlButtons.setStyle("-fx-padding: 20 0 0 0;");
        
        // Bouton pour reconfigurer
        Button reconfigBtn = new Button("Reconfigurer");
        reconfigBtn.setPrefWidth(150);
        reconfigBtn.setOnAction(e -> showConfigurationWindow((Stage)canvas.getScene().getWindow()));
        
        vb.getChildren().addAll(    
            configInfo,
            javafxText("-------------------------"),
            javafxText("Statistiques joueurs :"),
            players,
            javafxText("-------------------------"),
            controlButtons,
            reconfigBtn
        );
        
        return vb;
    }
    // Les boutons de controls (play , restart , load)
    private VBox createControlButtons() {
        VBox buttonBox = new VBox(10);
        
        // Bouton Play/Pause
        playPauseButton = new ToggleButton("Play");
        playPauseButton.setPrefWidth(150);
        playPauseButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        
        playPauseButton.setOnAction(e -> {
            if (playPauseButton.isSelected()) {
                playPauseButton.setText("Pause");
                playPauseButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-size: 14px;");
                startGameAnimation();
                // Ensure the canvas has focus when the game starts so key input works
                if (canvas != null) {
                    canvas.setFocusTraversable(true);
                    canvas.requestFocus();
                }
                isGameRunning = true;
            } else {
                playPauseButton.setText("Play");
                playPauseButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
                if (gameTimer != null) {
                    gameTimer.stop();
                }
                isGameRunning = false;
            }
        });
        
        Button restart = new Button("Restart");
        restart.setPrefWidth(150);
        restart.setOnAction(e -> {
            if (gameTimer != null) {
                gameTimer.stop();
            }
            isGameRunning = false;
            playPauseButton.setSelected(false);
            playPauseButton.setText("Play");
            playPauseButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
            recreateBoard();
        });
        
        Button load = new Button("Load");
        load.setPrefWidth(150);
        load.setOnAction(e -> System.out.println("Load clicked"));
        
        buttonBox.getChildren().addAll(playPauseButton, restart, load);
        return buttonBox;
    }
    
    private void startGameAnimation() {
        gameTimer = new AnimationTimer() {
            long last = 0;
            long lastNetworkUpdate = 0;
            
            @Override
            public void handle(long now) {
                if (!isGameRunning) return;
                
                if (now - last < 16_000_000) return;
                last = now;
                
                double speed = 0.12;

                // Gestion des contrôles selon le mode
                handleControls(speed);
                
                // Si on est host ou en local, mettre à jour la balle et les collisions
                if (isHost || config.networkConfig.getMode() == GameMode.LOCAL) {
                    updateBall();
                }
                
                drawBoard(canvas.getGraphicsContext2D());
                drawBall(canvas.getGraphicsContext2D());
                
                // Vérifier si un roi est mort (uniquement pour le host)
                if (isHost || config.networkConfig.getMode() == GameMode.LOCAL) {
                    checkKingStatus(canvas.getGraphicsContext2D());
                }
                
                // Synchroniser avec le réseau (toutes les 50ms)
                if (config.networkConfig.getMode() != GameMode.LOCAL) {
                    if (now - lastNetworkUpdate > 50_000_000) {
                        updateNetworkGameState();
                        lastNetworkUpdate = now;
                    }
                }
            }
        };
        gameTimer.start();
    }

    private void handleControls(double speed) {
        if (config.networkConfig.getMode() == GameMode.LOCAL) {
            handleLocalControls(speed);
        } else if (isHost) {
            handleHostControls(speed);
        } else if (isClientConnected) {
            handleClientControls(speed);
        }
    }

    private void handleLocalControls(double speed) {
        // Raquette blanche (HAUT) - Supporte AZERTY (ZQSD) et QWERTY (WASD)
        if (pressedKeys.contains(KeyCode.A) || pressedKeys.contains(KeyCode.Q)) {
            whitePaddleX = Math.max(0, whitePaddleX - speed);
        }
        if (pressedKeys.contains(KeyCode.D)) {
            whitePaddleX = Math.min(boardWidth - 1, whitePaddleX + speed);
        }
        if (pressedKeys.contains(KeyCode.W) || pressedKeys.contains(KeyCode.Z)) {
            if(whitePaddleY - speed * TILE_SIZE > (TILE_SIZE * 2) -20){
                whitePaddleY -= speed * TILE_SIZE;
            }
        }
        if (pressedKeys.contains(KeyCode.S)) {
            if(whitePaddleY + speed * TILE_SIZE + 15 < (TILE_SIZE * 2) + 50){
                whitePaddleY += speed * TILE_SIZE;
            }
        }
        
        // Raquette noire (BAS) - Contrôles flèches
        if (pressedKeys.contains(KeyCode.LEFT)) {
            blackPaddleX = Math.max(0, blackPaddleX - speed);
        }
        if (pressedKeys.contains(KeyCode.RIGHT)) {
            blackPaddleX = Math.min(boardWidth - 1, blackPaddleX + speed);
        }
        if (pressedKeys.contains(KeyCode.UP)) {
            if(blackPaddleY - speed * TILE_SIZE > 445){
                blackPaddleY -= speed * TILE_SIZE;
            }
        }
        if (pressedKeys.contains(KeyCode.DOWN)) {
            if(blackPaddleY + speed * TILE_SIZE + 15 < 505){
                blackPaddleY += speed * TILE_SIZE;
            }
        }
    }

    private void handleHostControls(double speed) {
        // Le host contrôle seulement la raquette blanche
        if (pressedKeys.contains(KeyCode.A) || pressedKeys.contains(KeyCode.Q)) {
            whitePaddleX = Math.max(0, whitePaddleX - speed);
        }
        if (pressedKeys.contains(KeyCode.D)) {
            whitePaddleX = Math.min(boardWidth - 1, whitePaddleX + speed);
        }
        if (pressedKeys.contains(KeyCode.W) || pressedKeys.contains(KeyCode.Z)) {
            if(whitePaddleY - speed * TILE_SIZE > (TILE_SIZE * 2) -20){
                whitePaddleY -= speed * TILE_SIZE;
            }
        }
        if (pressedKeys.contains(KeyCode.S)) {
            if(whitePaddleY + speed * TILE_SIZE + 15 < (TILE_SIZE * 2) + 50){
                whitePaddleY += speed * TILE_SIZE;
            }
        }
    }

    private void handleClientControls(double speed) {
        // Le client contrôle seulement sa raquette assignée
        if (isWhitePlayer) {
            // Contrôle raquette bl
            if (pressedKeys.contains(KeyCode.A) || pressedKeys.contains(KeyCode.Q)) {
                whitePaddleX = Math.max(0, whitePaddleX - speed);
            }
            if (pressedKeys.contains(KeyCode.D)) {
                whitePaddleX = Math.min(boardWidth - 1, whitePaddleX + speed);
            }
            if (pressedKeys.contains(KeyCode.W) || pressedKeys.contains(KeyCode.Z)) {
                if(whitePaddleY - speed * TILE_SIZE > (TILE_SIZE * 2) -20){
                    whitePaddleY -= speed * TILE_SIZE;
                }
            }
            if (pressedKeys.contains(KeyCode.S)) {
                if(whitePaddleY + speed * TILE_SIZE + 15 < (TILE_SIZE * 2) + 50){
                    whitePaddleY += speed * TILE_SIZE;
                }
            }
        } else {
            // Contrôle raquette noire
            if (pressedKeys.contains(KeyCode.LEFT)) {
                blackPaddleX = Math.max(0, blackPaddleX - speed);
            }
            if (pressedKeys.contains(KeyCode.RIGHT)) {
                blackPaddleX = Math.min(boardWidth - 1, blackPaddleX + speed);
            }
            if (pressedKeys.contains(KeyCode.UP)) {
                if(blackPaddleY - speed * TILE_SIZE > 445){
                    blackPaddleY -= speed * TILE_SIZE;
                }
            }
            if (pressedKeys.contains(KeyCode.DOWN)) {
                if(blackPaddleY + speed * TILE_SIZE + 15 < 505){
                    blackPaddleY += speed * TILE_SIZE;
                }
            }
        }
    }
        
    // RECRÉATION DU PLATEAU
    private void recreateBoard() {
        boardWidth = config.pieceLevel;
        board = new String[8][boardWidth];
        boardLife = new HashMap[8][boardWidth];
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < boardWidth; j++) {
                boardLife[i][j] = new HashMap<>();
            }
        }
        
        canvas.setWidth(boardWidth * TILE_SIZE);
        
        // Réinitialiser la position de la balle
        ballX = canvas.getWidth() / 2;
        ballY = canvas.getHeight() / 2;
        ballSpeedX = 3;
        ballSpeedY = 3;
        
        whitePaddleX = (boardWidth - 1) / 2.0;
        blackPaddleX = (boardWidth - 1) / 2.0;
        
        initializeBoard();
        drawBoard(canvas.getGraphicsContext2D());
        // After drawing, if we're a host and server is running, send initial state to any clients
        // (GameServer will also broadcast periodically). If we are client, nothing to do here.
    }

    // Create a snapshot GameState representing current game and configuration
    public GameState createGameState() {
        GameState gs = new GameState();
        gs.whitePaddleX = this.whitePaddleX;
        gs.whitePaddleY = this.whitePaddleY;
        gs.blackPaddleX = this.blackPaddleX;
        gs.blackPaddleY = this.blackPaddleY;
        gs.ballX = this.ballX;
        gs.ballY = this.ballY;
        gs.ballSpeedX = this.ballSpeedX;
        gs.ballSpeedY = this.ballSpeedY;
        gs.gameRunning = this.isGameRunning;
        gs.whiteScore = 0;
        gs.blackScore = 0;
        // configuration
        gs.pieceLevel = this.config.pieceLevel;
        gs.kingLife = this.config.kingLife;
        gs.queenLife = this.config.queenLife;
        gs.knightLife = this.config.knightLife;
        gs.pawnLife = this.config.pawnLife;
        
        // Serialize board state
        if (this.board != null) {
            gs.boardPieces = new String[this.board.length][];
            for (int i = 0; i < this.board.length; i++) {
                gs.boardPieces[i] = new String[this.board[i].length];
                System.arraycopy(this.board[i], 0, gs.boardPieces[i], 0, this.board[i].length);
            }
        }
        
        // Serialize piece life information
        if (this.boardLife != null) {
            gs.piecesLife = new GameState.PieceLifeInfo[this.boardLife.length][];
            for (int i = 0; i < this.boardLife.length; i++) {
                gs.piecesLife[i] = new GameState.PieceLifeInfo[this.boardLife[i].length];
                for (int j = 0; j < this.boardLife[i].length; j++) {
                    if (this.boardLife[i][j] != null && this.boardLife[i][j].containsKey("symbol")) {
                        PieceInfo p = this.boardLife[i][j].get("symbol");
                        gs.piecesLife[i][j] = new GameState.PieceLifeInfo(p.symbol, p.life, p.isWhite);
                    }
                }
            }
        }
        
        return gs;
    }
    // INITIALISATION DES PIÈCES AVEC VIE
    private void initializeBoard() {
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < boardWidth; j++) {
                board[i][j] = null;
                boardLife[i][j].clear();
            }
        
        if (config.pieceLevel == 2) {
            board[0][0] = "♕"; boardLife[0][0].put("symbol", new PieceInfo("♕", config.queenLife, true));
            board[0][1] = "♔"; boardLife[0][1].put("symbol", new PieceInfo("♔", config.kingLife, true));
            board[7][0] = "♛"; boardLife[7][0].put("symbol", new PieceInfo("♛", config.queenLife, false));
            board[7][1] = "♚"; boardLife[7][1].put("symbol", new PieceInfo("♚", config.kingLife, false));
            board[1][0] = board[1][1] = "♙"; 
            boardLife[1][0].put("symbol", new PieceInfo("♙", config.pawnLife, true));
            boardLife[1][1].put("symbol", new PieceInfo("♙", config.pawnLife, true));
            board[6][0] = board[6][1] = "♟";
            boardLife[6][0].put("symbol", new PieceInfo("♟", config.pawnLife, false));
            boardLife[6][1].put("symbol", new PieceInfo("♟", config.pawnLife, false));
            
        } else if (config.pieceLevel == 4) {
            for (int i = 0; i < 4; i++) {
                board[1][i] = "♙";
                boardLife[1][i].put("symbol", new PieceInfo("♙", config.pawnLife, true));
                board[6][i] = "♟";
                boardLife[6][i].put("symbol", new PieceInfo("♟", config.pawnLife, false));
            }
            board[0][0] = "♗"; boardLife[0][0].put("symbol", new PieceInfo("♗", 3, true));
            board[0][1] = "♕"; boardLife[0][1].put("symbol", new PieceInfo("♕", config.queenLife, true));
            board[0][2] = "♔"; boardLife[0][2].put("symbol", new PieceInfo("♔", config.kingLife, true));
            board[0][3] = "♗"; boardLife[0][3].put("symbol", new PieceInfo("♗", 3, true));
            board[7][0] = "♝"; boardLife[7][0].put("symbol", new PieceInfo("♝", 3, false));
            board[7][1] = "♛"; boardLife[7][1].put("symbol", new PieceInfo("♛", config.queenLife, false));
            board[7][2] = "♚"; boardLife[7][2].put("symbol", new PieceInfo("♚", config.kingLife, false));
            board[7][3] = "♝"; boardLife[7][3].put("symbol", new PieceInfo("♝", 3, false));
            
        } else if (config.pieceLevel == 6) {
            for (int i = 0; i < 6; i++) {
                board[1][i] = "♙";
                boardLife[1][i].put("symbol", new PieceInfo("♙", config.pawnLife, true));
                board[6][i] = "♟";
                boardLife[6][i].put("symbol", new PieceInfo("♟", config.pawnLife, false));
            }
            board[0][0] = "♘"; boardLife[0][0].put("symbol", new PieceInfo("♘", config.knightLife, true));
            board[0][1] = "♗"; boardLife[0][1].put("symbol", new PieceInfo("♗", 3, true));
            board[0][2] = "♕"; boardLife[0][2].put("symbol", new PieceInfo("♕", config.queenLife, true));
            board[0][3] = "♔"; boardLife[0][3].put("symbol", new PieceInfo("♔", config.kingLife, true));
            board[0][4] = "♗"; boardLife[0][4].put("symbol", new PieceInfo("♗", 3, true));
            board[0][5] = "♘"; boardLife[0][5].put("symbol", new PieceInfo("♘", config.knightLife, true));
            board[7][0] = "♞"; boardLife[7][0].put("symbol", new PieceInfo("♞", config.knightLife, false));
            board[7][1] = "♝"; boardLife[7][1].put("symbol", new PieceInfo("♝", 3, false));
            board[7][2] = "♛"; boardLife[7][2].put("symbol", new PieceInfo("♛", config.queenLife, false));
            board[7][3] = "♚"; boardLife[7][3].put("symbol", new PieceInfo("♚", config.kingLife, false));
            board[7][4] = "♝"; boardLife[7][4].put("symbol", new PieceInfo("♝", 3, false));
            board[7][5] = "♞"; boardLife[7][5].put("symbol", new PieceInfo("♞", config.knightLife, false));
            
        } else if (config.pieceLevel == 8) {
            for (int i = 0; i < 8; i++) {
                board[1][i] = "♙";
                boardLife[1][i].put("symbol", new PieceInfo("♙", config.pawnLife, true));
                board[6][i] = "♟";
                boardLife[6][i].put("symbol", new PieceInfo("♟", config.pawnLife, false));
            }
            board[0] = new String[]{"♖","♘","♗","♕","♔","♗","♘","♖"};
            boardLife[0][0].put("symbol", new PieceInfo("♖", 3, true));
            boardLife[0][1].put("symbol", new PieceInfo("♘", config.knightLife, true));
            boardLife[0][2].put("symbol", new PieceInfo("♗", 3, true));
            boardLife[0][3].put("symbol", new PieceInfo("♕", config.queenLife, true));
            boardLife[0][4].put("symbol", new PieceInfo("♔", config.kingLife, true));
            boardLife[0][5].put("symbol", new PieceInfo("♗", 3, true));
            boardLife[0][6].put("symbol", new PieceInfo("♘", config.knightLife, true));
            boardLife[0][7].put("symbol", new PieceInfo("♖", 3, true));
            
            board[7] = new String[]{"♜","♞","♝","♛","♚","♝","♞","♜"};
            boardLife[7][0].put("symbol", new PieceInfo("♜", 3, false));
            boardLife[7][1].put("symbol", new PieceInfo("♞", config.knightLife, false));
            boardLife[7][2].put("symbol", new PieceInfo("♝", 3, false));
            boardLife[7][3].put("symbol", new PieceInfo("♛", config.queenLife, false));
            boardLife[7][4].put("symbol", new PieceInfo("♚", config.kingLife, false));
            boardLife[7][5].put("symbol", new PieceInfo("♝", 3, false));
            boardLife[7][6].put("symbol", new PieceInfo("♞", config.knightLife, false));
            boardLife[7][7].put("symbol", new PieceInfo("♜", 3, false));
        }
    }
    
    //RENDU DU PLATEAU AVEC VIE
    private void drawBoard(GraphicsContext gc) {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawTiles(gc, 0, 2);
        drawMiddle(gc);
        drawTiles(gc, 6, 8);
        drawPaddles(gc);
        drawCoordinates(gc);
        drawPieces(gc);
        drawBall(gc);
    }
    
    private void drawTiles(GraphicsContext gc, int startRow, int endRow) {
        Color lightSquare = Color.rgb(245, 245, 220);
        Color darkSquare  = Color.rgb(222, 184, 135);
        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < boardWidth; col++) {
                boolean light = (row + col) % 2 == 0;
                gc.setFill(light ? lightSquare : darkSquare);
                gc.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }
    
    private void drawMiddle(GraphicsContext gc) {
        gc.setFill(Color.web("#fff"));
        gc.fillRect(0, TILE_SIZE * 2, boardWidth * TILE_SIZE, TILE_SIZE * 4);
    }
    
    private void drawPaddles(GraphicsContext gc) {
        double paddleWidth = TILE_SIZE * 1.5;
        double paddleHeight = 15;
        
        double pxW = whitePaddleX * TILE_SIZE + TILE_SIZE / 2 - paddleWidth / 2;
        gc.setFill(Color.RED);
        gc.fillRoundRect(pxW, whitePaddleY, paddleWidth, paddleHeight, 5, 5);
        
        double pxB = blackPaddleX * TILE_SIZE + TILE_SIZE / 2 - paddleWidth / 2;
        gc.setFill(Color.DARKBLUE);
        gc.fillRoundRect(pxB, blackPaddleY, paddleWidth, paddleHeight, 5, 5);
    }
    
    private void drawCoordinates(GraphicsContext gc) {
        gc.setFont(Font.font(14));
        gc.setFill(Color.GREY);
        
        for (int i = 0; i < boardWidth; i++)
            gc.fillText("" + (char)('a'+i), i*TILE_SIZE + 5, 8*TILE_SIZE - 5);
        
        gc.setFill(Color.BLACK);
        for (int i = 0; i < 8; i++)
            gc.fillText("" + (8-i), 5, i*TILE_SIZE + 15);
    }
    
    private void drawPieces(GraphicsContext gc) {
        gc.setFont(Font.font(60));
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < boardWidth; col++) {
                if (board[row][col] != null) {
                    // Déterminer la couleur de la pièce
                    if (boardLife[row][col].containsKey("symbol")) {
                        PieceInfo piece = boardLife[row][col].get("symbol");
                        gc.setFill(piece.isWhite ? Color.BLACK : Color.GRAY);
                    } else {
                        gc.setFill(Color.BLACK);
                    }
                    
                    gc.fillText(board[row][col], col*TILE_SIZE + 10, row*TILE_SIZE + 60);
                    
                    // Afficher la vie de la pièce dans un cercle
                    if (boardLife[row][col].containsKey("symbol")) {
                        PieceInfo piece = boardLife[row][col].get("symbol");
                        if (piece.life > 0) {
                            // Position du cercle en bas à droite
                            double circleX = col * TILE_SIZE + TILE_SIZE - 20;
                            double circleY = row * TILE_SIZE + TILE_SIZE - 20;
                            double radius = 8;
                            
                            if (piece.life <= 1) {
                                gc.setFill(Color.RED);
                            } else {
                                gc.setFill(Color.GREEN);
                            }
                            
                            // Dessiner le cercle
                            gc.fillOval(circleX, circleY, radius * 2, radius * 2);
                            
                            // Texte de la vie au centre du cercle
                            gc.setFont(Font.font(10));
                            gc.setFill(Color.WHITE);
                            String lifeText = "" + piece.life;
                            gc.fillText(lifeText, circleX + radius - 4, circleY + radius + 4);
                            
                            gc.setFont(Font.font(40)); // Réinitialiser
                        }
                    }
                }
            }
        }
    }
    
    private void drawBall(GraphicsContext gc) {
        gc.setFill(Color.YELLOW);
        gc.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);
    }
    
    private void updateBall() {
        if (!isGameRunning) return;
        
        ballX += ballSpeedX;
        ballY += ballSpeedY;
        
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        
        if (ballX <= 0 || ballX + BALL_SIZE >= width) ballSpeedX *= -1;
        
        double pw = TILE_SIZE * 1.5;
        double ph = 15;
        double paddleTopX = whitePaddleX * TILE_SIZE + TILE_SIZE/2 - pw/2;
        
        if (ballY <= whitePaddleY + ph &&
            ballY + BALL_SIZE >= whitePaddleY &&
            ballX + BALL_SIZE >= paddleTopX &&
            ballX <= paddleTopX + pw) {
            
            if (ballSpeedY < 0) {
                ballSpeedY = Math.abs(ballSpeedY);
                ballY = whitePaddleY + ph + 1;
            }
        }
        
        double paddleBotX = blackPaddleX * TILE_SIZE + TILE_SIZE/2 - pw/2;
        
        if (ballY + BALL_SIZE >= blackPaddleY &&
            ballY <= blackPaddleY + ph &&
            ballX + BALL_SIZE >= paddleBotX &&
            ballX <= paddleBotX + pw) {
            
            if (ballSpeedY > 0) {
                ballSpeedY = -Math.abs(ballSpeedY);
                ballY = blackPaddleY - BALL_SIZE - 1;
            }
        }
        
        // Collision avec les pièces
        int col = (int)(ballX / TILE_SIZE);
        int row = (int)(ballY / TILE_SIZE);
        
        if (row >= 0 && row < 8 && col >= 0 && col < boardWidth) {
            if (board[row][col] != null && boardLife[row][col].containsKey("symbol")) {
                PieceInfo piece = boardLife[row][col].get("symbol");
                piece.life--;
                
                if (piece.life <= 0) {
                    // Pièce détruite
                    board[row][col] = null;
                    boardLife[row][col].clear();
                }
                
                ballSpeedY *= -1;
                ballSpeedX += (Math.random() - 0.5) * 0.4;
                ballY += ballSpeedY * 2;
            }
        }
        
        if (ballY <= 0) ballSpeedY = Math.abs(ballSpeedY);
        if (ballY + BALL_SIZE >= height) ballSpeedY = -Math.abs(ballSpeedY);
        // Appeler checkKingStatus à la fin
        GraphicsContext gc = canvas.getGraphicsContext2D();
        checkKingStatus(gc);
    }

    private void checkKingStatus(GraphicsContext gc) {
        boolean whiteKingAlive = false;
        boolean blackKingAlive = false;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < boardWidth; col++) {
                if (boardLife[row][col].containsKey("symbol")) {
                    PieceInfo piece = boardLife[row][col].get("symbol");
                    if (piece.symbol.equals("♔") || piece.symbol.equals("♚")) {
                        if (piece.symbol.equals("♔") && piece.life > 0) whiteKingAlive = true;
                        if (piece.symbol.equals("♚") && piece.life > 0) blackKingAlive = true;
                    }
                }
            }
        }
        
        // Afficher le message de victoire
        if (!whiteKingAlive) {
            gc.setFont(Font.font(48));
            gc.setFill(Color.RED);
            gc.fillText("NOIR GAGNE!", canvas.getWidth()/2 - 120, canvas.getHeight()/2);
            isGameRunning = false;
            if (gameTimer != null) gameTimer.stop();
        } else if (!blackKingAlive) {
            gc.setFont(Font.font(48));
            gc.setFill(Color.BLUE);
            gc.fillText("BLANC GAGNE!", canvas.getWidth()/2 - 120, canvas.getHeight()/2);
            isGameRunning = false;
            if (gameTimer != null) gameTimer.stop();
        }
    }

    // Configuration du reseau
    private void startGameServer(int port) {
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

    private void connectToGameServer(String ip, int port) {
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

    // Méthode pour recevoir les mises à jour du serveur (pour le client)
    public void receiveNetworkUpdate(GameState gameState) {
        Platform.runLater(() -> {
            if (!isHost) {
                // Apply configuration sent by host (piece counts, lives)
                applyNetworkConfiguration(gameState);
                
                // Apply board state (pieces and their lives)
                applyBoardState(gameState);
                
                whitePaddleX = gameState.whitePaddleX;
                whitePaddleY = gameState.whitePaddleY;
                blackPaddleX = gameState.blackPaddleX;
                blackPaddleY = gameState.blackPaddleY;
                ballX = gameState.ballX;
                ballY = gameState.ballY;
                ballSpeedX = gameState.ballSpeedX;
                ballSpeedY = gameState.ballSpeedY;
                
                // Synchroniser l'état d'exécution : démarrer/arrêter l'animation locale selon l'hôte
                if (gameState.gameRunning) {
                    if (gameTimer == null) {
                        startGameAnimation();
                    } else {
                        isGameRunning = true;
                        gameTimer.start();
                    }
                } else {
                    isGameRunning = false;
                    if (gameTimer != null) gameTimer.stop();
                }

                // Mettre à jour l'interface
                if (playPauseButton != null) {
                    playPauseButton.setSelected(isGameRunning);
                    playPauseButton.setText(isGameRunning ? "Pause" : "Play");
                }
            }
        });
    }
    
    // Apply board state received from host
    private void applyBoardState(GameState gs) {
        if (gs.boardPieces != null && gs.piecesLife != null) {
            // Copy board pieces
            for (int i = 0; i < Math.min(gs.boardPieces.length, board.length); i++) {
                for (int j = 0; j < Math.min(gs.boardPieces[i].length, board[i].length); j++) {
                    board[i][j] = gs.boardPieces[i][j];
                }
            }
            
            // Copy piece life info
            for (int i = 0; i < Math.min(gs.piecesLife.length, boardLife.length); i++) {
                for (int j = 0; j < Math.min(gs.piecesLife[i].length, boardLife[i].length); j++) {
                    boardLife[i][j].clear();
                    if (gs.piecesLife[i][j] != null) {
                        PieceInfo p = new PieceInfo(
                            gs.piecesLife[i][j].symbol,
                            gs.piecesLife[i][j].life,
                            gs.piecesLife[i][j].isWhite
                        );
                        boardLife[i][j].put("symbol", p);
                    }
                }
            }
        }
    }

    // When receiving a GameState from host, also ensure configuration is applied
    private void applyNetworkConfiguration(GameState gs) {
        // If piece level or life values differ, apply them and recreate board
        boolean needRecreate = false;
        if (gs.pieceLevel != config.pieceLevel) {
            config.pieceLevel = gs.pieceLevel;
            needRecreate = true;
        }
        if (gs.kingLife != config.kingLife || gs.queenLife != config.queenLife ||
            gs.knightLife != config.knightLife || gs.pawnLife != config.pawnLife) {
            config.kingLife = gs.kingLife;
            config.queenLife = gs.queenLife;
            config.knightLife = gs.knightLife;
            config.pawnLife = gs.pawnLife;
            needRecreate = true;
        }

        if (needRecreate) {
            // Recreate board and redraw
            recreateBoard();
            drawBoard(canvas.getGraphicsContext2D());
        }
    }

    // Méthode pour envoyer les mises à jour au serveur
    private void sendGameUpdate() {
        if (gameClient != null && isClientConnected) {
            GameState gameState = new GameState();
            gameState.whitePaddleX = whitePaddleX;
            gameState.whitePaddleY = whitePaddleY;
            gameState.blackPaddleX = blackPaddleX;
            gameState.blackPaddleY = blackPaddleY;
            gameState.ballX = ballX;
            gameState.ballY = ballY;
            gameState.ballSpeedX = ballSpeedX;
            gameState.ballSpeedY = ballSpeedY;
            gameState.gameRunning = isGameRunning;
            
            gameClient.sendGameState(gameState);
        }
    }

    // Mettre à jour l'état du jeu pour le réseau
    private void updateNetworkGameState() {
        if (config.networkConfig.getMode() == GameMode.LOCAL) return;
        
        if (isHost) {
            // Le host envoie l'état complet aux clients
            if (gameServer != null) {
                GameState gameState = createGameState();
                gameServer.broadcastGameState(gameState);
            }
        } else if (isClientConnected) {
            // Le client envoie seulement sa position de raquette
            PaddleUpdate update = new PaddleUpdate(
                isWhitePlayer ? whitePaddleX : blackPaddleX,
                isWhitePlayer ? whitePaddleY : blackPaddleY,
                isWhitePlayer,
                playerName
            );
            gameClient.sendPaddleUpdate(update);
        }
    }

    private void sendPaddleUpdate(double x, double y, boolean isWhite) {
        if (gameClient != null) {
            PaddleUpdate update = new PaddleUpdate(x, y, isWhite, playerName);
            gameClient.sendPaddleUpdate(update);
        }
    }
    
    private javafx.scene.text.Text createInfoText(String s) {
        javafx.scene.text.Text t = new javafx.scene.text.Text(s);
        t.setFill(Color.WHITE);
        t.setFont(Font.font(12));
        return t;
    }
    
    private javafx.scene.text.Text javafxText(String s) {
        javafx.scene.text.Text t = new javafx.scene.text.Text(s);
        t.setFill(Color.BLACK);
        return t;
    }
    
    public static void main(String[] args) {
        launch();
    }
}