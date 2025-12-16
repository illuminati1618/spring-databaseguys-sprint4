package com.open.spring.mvc.grades;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/mvc/grades")
public class GradeViewController {

    @Autowired
    private GradeRepository gradeRepository;

    @GetMapping("/read")
    public String read(Model model) {
        List<Grade> grades = gradeRepository.findAll();
        model.addAttribute("list", grades);
        return "grades/read";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("grade", new Grade());
        return "grades/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Grade grade) {
        gradeRepository.save(grade);
        return "redirect:/mvc/grades/read";
    }

    @GetMapping("/update/{id}")
    public String updateForm(@PathVariable Long id, Model model) {
        gradeRepository.findById(id).ifPresent(grade -> model.addAttribute("grade", grade));
        return "grades/update";
    }

    @PostMapping("/update")
    public String update(@ModelAttribute Grade grade) {
        gradeRepository.save(grade);
        return "redirect:/mvc/grades/read";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        gradeRepository.deleteById(id);
        return "redirect:/mvc/grades/read";
    }
}
