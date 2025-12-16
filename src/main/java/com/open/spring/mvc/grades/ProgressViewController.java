package com.open.spring.mvc.grades;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/mvc/progress")
public class ProgressViewController {

    @Autowired
    private ProgressRepository progressRepository;

    @GetMapping("/read")
    public String read(Model model) {
        List<Progress> progressList = progressRepository.findAll();
        model.addAttribute("list", progressList);
        return "progress/read";
    }
}
