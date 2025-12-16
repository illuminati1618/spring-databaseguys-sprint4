package com.open.spring.mvc.games;

import com.open.spring.mvc.rpg.games.Game;
import com.open.spring.mvc.rpg.games.UnifiedGameRepository;
import com.open.spring.mvc.table.TableConfig;
import com.open.spring.mvc.table.TableConfigBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Auto-generated controller for Games (displays Game entity data)
 * Uses reflection to discover all fields automatically
 */
@Controller
@RequestMapping("/mvc/games")
public class GamesMvcController {

    @Autowired
    private UnifiedGameRepository unifiedGameRepository;

    @GetMapping("/read")
    public String readView(Model model) {
        // AUTO-MAGIC: Points at Game.class, discovers ALL fields automatically!
        TableConfig tableConfig = TableConfigBuilder.fromEntity(Game.class)
                .withEntityName("games")
                .withDisplayNames("Game", "Games")
                .withTableId("gamesTable")
                .withPaths("/mvc/games/edit", "/mvc/games/delete")
                .withCreateNew("/mvc/games/new", "Create New Game")
                .withMaxVisibleColumns(6)
                .build();

        model.addAttribute("tableConfig", tableConfig);
        model.addAttribute("list", unifiedGameRepository.findAll());
        return "games/read";
    }
}
