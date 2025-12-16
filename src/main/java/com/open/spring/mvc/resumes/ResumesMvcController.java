package com.open.spring.mvc.resumes;

import com.open.spring.mvc.resume.Resume;
import com.open.spring.mvc.resume.ResumeJpaRepository;
import com.open.spring.mvc.table.TableConfig;
import com.open.spring.mvc.table.TableConfigBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Auto-generated controller for Resumes (displays Resume entity data)
 * Uses reflection to discover all fields automatically
 */
@Controller
@RequestMapping("/mvc/resumes")
public class ResumesMvcController {

    @Autowired
    private ResumeJpaRepository resumeJpaRepository;

    @GetMapping("/read")
    public String readView(Model model) {
        // AUTO-MAGIC: Points at Resume.class, discovers ALL fields automatically!
        TableConfig tableConfig = TableConfigBuilder.fromEntity(Resume.class)
                .withEntityName("resumes")
                .withDisplayNames("Resume", "Resumes")
                .withTableId("resumesTable")
                .withPaths("/mvc/resumes/edit", "/mvc/resumes/delete")
                .withCreateNew("/mvc/resumes/new", "Create New Resume")
                .withMaxVisibleColumns(6)
                .build();

        model.addAttribute("tableConfig", tableConfig);
        model.addAttribute("list", resumeJpaRepository.findAll());
        return "resumes/read";
    }
}
