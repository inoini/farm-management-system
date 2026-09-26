
package com.example.demo.conotroller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Schedule;

@Controller
public class ScheduleController {

    @GetMapping("/schedule/new")
    public String showNewScheduleForm(
            @RequestParam("date") String date,
            Model model) {

        Schedule schedule = new Schedule();

        schedule.setDate(date);

        model.addAttribute("schedule", schedule);

        return "schedule-form";
    }
}