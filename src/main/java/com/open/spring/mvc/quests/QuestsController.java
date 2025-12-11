package com.open.spring.mvc.quests;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/quests")
public class QuestsController {

    @Autowired
    private QuestsRepository repository;

    @GetMapping("/{id}")
    public ResponseEntity<Quest> getQuest(@PathVariable Long id) {
        Optional<Quest> quest = repository.findById(id);
        return ResponseEntity.of(quest);
    }

    @PostMapping("/create")
    public ResponseEntity<Quest> createQuest(@Valid @RequestBody Quest requestBodyQuest) {
        if (requestBodyQuest.getName() == null || requestBodyQuest.getPermalink() == null ||
            requestBodyQuest.getDifficulty() == null || requestBodyQuest.getTotalSubmodules() == null ||
            requestBodyQuest.getRewardPoints() == null || !requestBodyQuest.getPermalink().startsWith("/")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (repository.findAll().stream().anyMatch(q -> q.getPermalink().equals(requestBodyQuest.getPermalink()))) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        // For simplicity, let's assume the entity is valid and create a dummy quest
        Quest newQuest = new Quest(requestBodyQuest.getName(), requestBodyQuest.getDifficulty(), requestBodyQuest.getPermalink(), requestBodyQuest.getTotalSubmodules(), requestBodyQuest.getRewardPoints());
        repository.save(newQuest);
        return new ResponseEntity<>(newQuest, HttpStatus.CREATED);
    }
    
    @PutMapping("/update/{id}")
    public ResponseEntity<Quest> updateQuest(@PathVariable Long id, @Valid @RequestBody Quest requestBodyQuest) {
        Optional<Quest> existingQuestOpt = repository.findById(id);
        if (!existingQuestOpt.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Quest existingQuest = existingQuestOpt.get();
        if (requestBodyQuest.getName() != null) {
            existingQuest.setName(requestBodyQuest.getName());
        }
        if (requestBodyQuest.getDifficulty() != null) {
            existingQuest.setDifficulty(requestBodyQuest.getDifficulty());
        }
        if (requestBodyQuest.getPermalink() != null) {
            existingQuest.setPermalink(requestBodyQuest.getPermalink());
        }
        if (requestBodyQuest.getTotalSubmodules() != null) {
            existingQuest.setTotalSubmodules(requestBodyQuest.getTotalSubmodules());
        }
        if (requestBodyQuest.getRewardPoints() != null) {
            existingQuest.setRewardPoints(requestBodyQuest.getRewardPoints());
        }
        repository.save(existingQuest);
        return new ResponseEntity<>(existingQuest, HttpStatus.OK);
    }
}
