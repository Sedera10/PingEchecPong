package com.chessping.ejb.backend.dto;

import java.io.Serializable;

/**
 * DTO (Data Transfer Object) pour transférer les configurations entre client et serveur EJB.
 * Pas d'annotations JPA pour éviter les problèmes de sérialisation.
 */
public class GameConfigDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String configName;
    private Integer pieceLevel;
    private Integer kingLife;
    private Integer queenLife;
    private Integer knightLife;
    private Integer pawnLife;
    private Integer bishopLife;
    private Integer rookLife;

    public GameConfigDTO() {}

    // Getters & Setters
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
}
