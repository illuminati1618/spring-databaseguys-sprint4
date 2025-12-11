package com.open.spring.mvc.quests;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestsRepository extends JpaRepository<Quest, Long> {
    
}
