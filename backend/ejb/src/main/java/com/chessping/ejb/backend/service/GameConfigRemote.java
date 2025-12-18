package com.chessping.ejb.backend.service;

import com.chessping.ejb.backend.dto.GameConfigDTO;
import jakarta.ejb.Remote;

import java.util.List;

@Remote
public interface GameConfigRemote {

    GameConfigDTO findByName(String configName);

    List<GameConfigDTO> findAll();

    GameConfigDTO save(GameConfigDTO config);

    GameConfigDTO update(GameConfigDTO config);

    void delete(String configName);
}
