package com.example.demo.conotroller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.entity.Crop;
import com.example.demo.repository.CropRepository;
import com.example.demo.service.CurrentUserService;

@Controller
public class CropController {

    private final CropRepository cropRepository;
    private final CurrentUserService currentUser;

    public CropController(CropRepository cropRepository, CurrentUserService currentUser) {
        this.cropRepository = cropRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/crop")
    public String cropList(Model model) {
        model.addAttribute("crops", cropRepository.findAllByOwnerEmailOrderByIdDesc(currentUser.email()));
        model.addAttribute("crop", new Crop());
        return "crop/list";
    }

    @GetMapping("/crop/add")
    public String cropAdd(Model model) {
        model.addAttribute("crop", new Crop());
        return "crop/add";
    }

    @GetMapping("/crop/edit/{id}")
    public String editCrop(@PathVariable Long id, Model model) {
        Crop crop = cropRepository.findByIdAndOwnerEmail(id, currentUser.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("crop", crop);
        return "crop/add";
    }

    @PostMapping("/crop/save")
    public String saveCrop(@ModelAttribute Crop crop) {
        String owner = currentUser.email();
        if (crop.getId() != null && cropRepository.findByIdAndOwnerEmail(crop.getId(), owner).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        crop.setOwnerEmail(owner);
        cropRepository.save(crop);
        return "redirect:/crop";
    }

    @PostMapping("/crop/delete/{id}")
    public String deleteCrop(@PathVariable Long id) {
        cropRepository.findByIdAndOwnerEmail(id, currentUser.email()).ifPresent(cropRepository::delete);
        return "redirect:/crop";
    }
}
