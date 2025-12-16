package com.open.spring.mvc.quests;

import com.open.spring.mvc.quests.Quest;
import com.open.spring.mvc.quests.QuestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Controller
@RequestMapping("/mvc/quests")
@Slf4j
public class QuestViewController {

    @Autowired
    private QuestRepository questRepository;

    @GetMapping("")
    public String getQuests(Model model) {
        List<Quest> quests = questRepository.findAll();
        model.addAttribute("quests", quests);
        return "quests/questManager";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String createQuest(@RequestParam String name, @RequestParam Quest.Difficulty difficulty, @RequestParam String permalink, @RequestParam Integer totalSubmodules, @RequestParam Integer rewardPoints) {
        Quest newQuest = new Quest(name, difficulty, permalink, totalSubmodules, rewardPoints);
        questRepository.save(newQuest);
        return "redirect:/mvc/quests";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String deleteQuest(@PathVariable Long id) {
        questRepository.deleteById(id);
        return "redirect:/mvc/quests";
    }
}
