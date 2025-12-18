package com.chessping.ejb;

import com.chessping.ejb.backend.service.GameConfigRemote;
import com.chessping.ejb.backend.dto.GameConfigDTO;
import com.chessping.networks.GameConfig;

public class GameConfigAdapter {

    private final GameConfigRemote ejbRemote;

    public GameConfigAdapter(GameConfigRemote ejbRemote) {
        this.ejbRemote = ejbRemote;
    }

    public GameConfig loadConfig(String configName) throws Exception {
        try {
            GameConfigDTO ejbConfig = ejbRemote.findByName(configName);
            
            if (ejbConfig == null) {
                System.out.println("✓ Config non trouvée en BD, retour config par défaut");
                return new GameConfig();
            }

            GameConfig localConfig = new GameConfig();
            localConfig.pieceLevel = ejbConfig.getPieceLevel() != null ? ejbConfig.getPieceLevel() : 8;
            localConfig.kingLife = ejbConfig.getKingLife() != null ? ejbConfig.getKingLife() : 5;
            localConfig.queenLife = ejbConfig.getQueenLife() != null ? ejbConfig.getQueenLife() : 4;
            localConfig.knightLife = ejbConfig.getKnightLife() != null ? ejbConfig.getKnightLife() : 2;
            localConfig.pawnLife = ejbConfig.getPawnLife() != null ? ejbConfig.getPawnLife() : 1;
            localConfig.bishopLife = ejbConfig.getBishopLife() != null ? ejbConfig.getBishopLife() : 3;
            localConfig.rookLife = ejbConfig.getRookLife() != null ? ejbConfig.getRookLife() : 4;

            System.out.println("✓ Config chargée depuis WildFly EJB : " + configName);
            System.out.println("  - pieceLevel: " + localConfig.pieceLevel);
            System.out.println("  - kingLife: " + localConfig.kingLife);
            System.out.println("  - queenLife: " + localConfig.queenLife);
            System.out.println("  - knightLife: " + localConfig.knightLife);
            System.out.println("  - pawnLife: " + localConfig.pawnLife);
            System.out.println("  - bishopLife: " + localConfig.bishopLife);
            System.out.println("  - rookLife: " + localConfig.rookLife);

            return localConfig;
        } catch (Exception ex) {
            System.err.println("✗ Erreur chargement depuis EJB: " + ex.getMessage());
            ex.printStackTrace();
            return new GameConfig();
        }
    }

    public void saveConfig(GameConfig localConfig, String configName) throws Exception {
        try {
            GameConfigDTO dto = new GameConfigDTO();
            dto.setConfigName(configName);
            dto.setPieceLevel(localConfig.pieceLevel);
            dto.setKingLife(localConfig.kingLife);
            dto.setQueenLife(localConfig.queenLife);
            dto.setKnightLife(localConfig.knightLife);
            dto.setPawnLife(localConfig.pawnLife);
            dto.setBishopLife(localConfig.bishopLife);
            dto.setRookLife(localConfig.rookLife);

            // Sauvegarder via EJB
            ejbRemote.save(dto);

            System.out.println("✓ Config sauvegardée en BD via WildFly EJB : " + configName);
            System.out.println("  - pieceLevel: " + localConfig.pieceLevel);
            System.out.println("  - kingLife: " + localConfig.kingLife);
            System.out.println("  - queenLife: " + localConfig.queenLife);
            System.out.println("  - knightLife: " + localConfig.knightLife);
            System.out.println("  - pawnLife: " + localConfig.pawnLife);
            System.out.println("  - bishopLife: " + localConfig.bishopLife);
            System.out.println("  - rookLife: " + localConfig.rookLife);

        } catch (Exception ex) {
            System.err.println("✗ Erreur sauvegarde vers EJB: " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
    }
}
