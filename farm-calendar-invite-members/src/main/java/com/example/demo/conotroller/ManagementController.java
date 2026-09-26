package com.example.demo.conotroller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class ManagementController {


    @GetMapping("/management")
    public String management(Model model){

        return "management";

    }

}