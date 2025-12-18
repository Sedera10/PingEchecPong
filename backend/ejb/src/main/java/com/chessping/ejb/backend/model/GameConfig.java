package com.chessping.ejb.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "game_configs")
public class GameConfig {

    @Id
    @Column(name = "config_name", nullable = false)
    private String configName;

    @Column(name = "piece_level")
    private Integer pieceLevel;

    @Column(name = "king_life")
    private Integer kingLife;

    @Column(name = "queen_life")
    private Integer queenLife;

    @Column(name = "knight_life")
    private Integer knightLife;

    @Column(name = "pawn_life")
    private Integer pawnLife;

    @Column(name = "bishop_life")
    private Integer bishopLife;

    @Column(name = "rook_life")
    private Integer rookLife;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // 🔹 obligatoire JPA
    public GameConfig() {}

    // getters & setters
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }

    public Integer getPieceLevel() { return pieceLevel; }
    public void setPieceLevel(Integer pieceLevel) { this.pieceLevel = pieceLevel; }

    public Integer getKingLife() { return kingLife; }
    public void setKingLife(Integer kingLife) { this.kingLife = kingLife; }

    public Integer getQueenLife() { return queenLife; }
    public void setQueenLife(Integer queenLife) { this.queenLife = queenLife; }

    public Integer getKnightLife() { return knightLife; }
    public void setKnightLife(Integer knightLife) { this.knightLife = knightLife; }

    public Integer getPawnLife() { return pawnLife; }
    public void setPawnLife(Integer pawnLife) { this.pawnLife = pawnLife; }

    public Integer getBishopLife() { return bishopLife; }
    public void setBishopLife(Integer bishopLife) { this.bishopLife = bishopLife; }

    public Integer getRookLife() { return rookLife; }
    public void setRookLife(Integer rookLife) { this.rookLife = rookLife; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
