package com.chessping.ejb.backend.service;

import com.chessping.ejb.backend.model.GameConfig;
import com.chessping.ejb.backend.dto.GameConfigDTO;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class GameConfigBean implements GameConfigRemote {

    @PersistenceContext(unitName = "ChessPingPU")
    private EntityManager em;

    @Override
    public GameConfigDTO findByName(String configName) {
        GameConfig entity = em.find(GameConfig.class, configName);
        return entity != null ? toDTO(entity) : null;
    }

    @Override
    public List<GameConfigDTO> findAll() {
        return em.createQuery("SELECT g FROM GameConfig g", GameConfig.class)
                .getResultList()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public GameConfigDTO save(GameConfigDTO dto) {
        GameConfig entity = em.find(GameConfig.class, dto.getConfigName());
        
        if (entity == null) {
            entity = new GameConfig();
            entity.setConfigName(dto.getConfigName());
        }
        
        entity.setPieceLevel(dto.getPieceLevel());
        entity.setKingLife(dto.getKingLife());
        entity.setQueenLife(dto.getQueenLife());
        entity.setKnightLife(dto.getKnightLife());
        entity.setPawnLife(dto.getPawnLife());
        entity.setBishopLife(dto.getBishopLife());
        entity.setRookLife(dto.getRookLife());
        entity.setUpdatedAt(OffsetDateTime.now());
        
        em.merge(entity);
        return toDTO(entity);
    }

    @Override
    public GameConfigDTO update(GameConfigDTO dto) {
        if (dto == null || dto.getConfigName() == null) {
            throw new IllegalArgumentException("Config ou configName null");
        }
        return save(dto);
    }

    @Override
    public void delete(String configName) {
        GameConfig entity = em.find(GameConfig.class, configName);
        if (entity != null) {
            em.remove(entity);
        }
    }
    
    private GameConfigDTO toDTO(GameConfig entity) {
        GameConfigDTO dto = new GameConfigDTO();
        dto.setConfigName(entity.getConfigName());
        dto.setPieceLevel(entity.getPieceLevel());
        dto.setKingLife(entity.getKingLife());
        dto.setQueenLife(entity.getQueenLife());
        dto.setKnightLife(entity.getKnightLife());
        dto.setPawnLife(entity.getPawnLife());
        dto.setBishopLife(entity.getBishopLife());
        dto.setRookLife(entity.getRookLife());
        return dto;
    }
}
