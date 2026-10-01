package com.example.demo.conotroller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.ContactMessage;
import com.example.demo.entity.UserAccount;
import com.example.demo.repository.ContactMessageRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.SupportMailService;

@Controller
public class ContactController {

    private final CurrentUserService currentUser;
    private final ContactMessageRepository contactRepository;
    private final SupportMailService supportMailService;

    public ContactController(CurrentUserService currentUser,
            ContactMessageRepository contactRepository,
            SupportMailService supportMailService) {
        this.currentUser = currentUser;
        this.contactRepository = contactRepository;
        this.supportMailService = supportMailService;
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        UserAccount account = currentUser.account();
        model.addAttribute("contactName", safe(account.getDisplayName()));
        model.addAttribute("contactEmail", safe(account.getEmail()));
        return "contact";
    }

    @PostMapping("/contact")
    public String sendContact(
            @RequestParam String subject,
            @RequestParam String message,
            RedirectAttributes redirectAttributes) {
        String cleanSubject = safe(subject);
        String cleanMessage = safe(message);
        if (cleanSubject.isBlank() || cleanMessage.isBlank()
                || cleanSubject.length() > 160 || cleanMessage.length() > 4000) {
            redirectAttributes.addFlashAttribute("contactError",
                    "件名とお問い合わせ内容を入力してください。件名は160文字以内、内容は4000文字以内です。");
            return "redirect:/contact";
        }

        UserAccount account = currentUser.account();
        ContactMessage contact = new ContactMessage();
        contact.setFarmDataKey(currentUser.email());
        contact.setSubmitterEmail(safe(account.getEmail()));
        contact.setSubmitterName(safe(account.getDisplayName()).isBlank()
                ? currentUser.username() : safe(account.getDisplayName()));
        contact.setSubject(cleanSubject);
        contact.setMessage(cleanMessage);
        contact.setSubmittedAt(LocalDateTime.now());
        contact.setEmailSent(false);
        contact = contactRepository.save(contact);

        boolean sent = supportMailService.send(contact, account, account.getFarm());
        if (sent) {
            contact.setEmailSent(true);
            contactRepository.save(contact);
            redirectAttributes.addFlashAttribute("contactSuccess", "お問い合わせを送信しました。");
        } else {
            redirectAttributes.addFlashAttribute("contactSuccess",
                    "お問い合わせを受け付けました。内容は保存されています。");
            redirectAttributes.addFlashAttribute("contactNotice",
                    "メール通知が未設定または送信できなかったため、運営側でメール設定を確認してください。");
        }
        return "redirect:/contact";
    }

    private String safe(String value) {
        return value == null ? "" : value.strip();
    }
}
