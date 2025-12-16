package com.open.spring.mvc.groups;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonDetailsService;

@Controller
@RequestMapping("/mvc/groups")
public class GroupsViewController {
    @Autowired
    private GroupsDetailsService repository;

    @Autowired
    private PersonDetailsService personRepository;

    // ===== "Read" Get mappings =====
    
    @GetMapping("/read")
    @Transactional(readOnly = true)
    public String groups(Authentication authentication, Model model) {
        // Optional authentication; fall back to public view if not logged in
        boolean isAdmin = false;
        Person person = null;

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            for (GrantedAuthority authority : userDetails.getAuthorities()) {
                if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                    isAdmin = true;
                    break;
                }
            }
            person = personRepository.getByUid(userDetails.getUsername());
        }

        List<Groups> list = isAdmin
                ? repository.listAllWithMembers()
                : (person != null
                        ? repository.findGroupsByPersonIdWithMembers(person.getId())
                        : repository.listAllWithMembers());

        model.addAttribute("list", list);  // Add the list to the model for the view
        return "group/read";  // Return the template for displaying groups
    }

    @GetMapping("/read/{id}")
    @Transactional(readOnly = true)
    public String group(Authentication authentication, @PathVariable("id") long id, Model model) {
        //check user authority
        UserDetails userDetails = (UserDetails)authentication.getPrincipal(); 
        boolean isAdmin = false;
        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            if(String.valueOf("ROLE_ADMIN").equals(authority.getAuthority())){
                isAdmin = true;
                break;
            }
        }
        if (isAdmin == true){
            Groups group = repository.get(id);  // Fetch the group by ID
            List<Groups> list = java.util.Arrays.asList(group);  // Convert the single group into a list for consistency
            model.addAttribute("list", list);  // Add the list to the model for the view 
        }
        else {
            // Non-admin users can only see groups they are members of
            Person person = personRepository.getByUid(userDetails.getUsername());
            Groups group = repository.get(id);
            if (group != null && group.getGroupMembers().contains(person)) {
                List<Groups> list = java.util.Collections.singletonList(group);
                model.addAttribute("list", list);
            } else {
                return "redirect:/error/401";  // Unauthorized
            }
        }
        return "group/read";  // Return the template for displaying the group
    }

    // ===== "Create" Get and Post mappings =====

    @GetMapping("/create")
    public String groupAdd(Groups group) {
        return "group/create";
    }

    // Note: POST /create is handled by JavaScript calling /api/groups endpoint
    // This method is here for consistency with person pattern, but may not be used
    @GetMapping("/create/redirect")
    public String groupCreateRedirect() {
        return "redirect:/mvc/groups/read";
    }

    // ===== "Update" Get mappings =====

    @GetMapping("/update/{id}")
    public String groupUpdate(@PathVariable("id") long id, Model model) {
        model.addAttribute("group", repository.get(id));
        return "group/update";
    }

    // ===== "Delete" Get mappings =====

    @GetMapping("/delete/{id}")
    public String groupDelete(Authentication authentication, @PathVariable("id") long id) { 
        // Only admins can delete groups
        UserDetails userDetails = (UserDetails)authentication.getPrincipal(); 
        boolean isAdmin = false;
        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            if(String.valueOf("ROLE_ADMIN").equals(authority.getAuthority())){
                isAdmin = true;
                break;
            }
        }
        if (!isAdmin) {
            return "redirect:/error/401";  // Unauthorized
        }
        
        repository.delete(id);  // Delete the group by ID
        return "redirect:/mvc/groups/read";  // Redirect to the read page after deletion
    }

    @GetMapping("/group-tracker")
    @Transactional(readOnly = true)
    public String groupTracker(Authentication authentication, Model model) {
        boolean isAdmin = false;
        Person person = null;

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            person = personRepository.getByUid(userDetails.getUsername());
        }

        List<Groups> list = isAdmin
                ? repository.listAllWithMembers()
                : (person != null
                        ? repository.findGroupsByPersonIdWithMembers(person.getId())
                        : repository.listAllWithMembers());

        model.addAttribute("groups", list);
        return "group/group";
    }
}