package se.iths.springbootgroupproject.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import se.iths.springbootgroupproject.dto.CreateMessageFormData;
import se.iths.springbootgroupproject.dto.EditUserFormData;
import se.iths.springbootgroupproject.dto.MessageAndUsername;
import se.iths.springbootgroupproject.entities.Message;
import se.iths.springbootgroupproject.entities.User;
import se.iths.springbootgroupproject.services.MessageService;
import se.iths.springbootgroupproject.services.UserService;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/web")
public class WebController {

    private final MessageService messageService;
    private final UserService userService;

    public WebController(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    @GetMapping("/welcome")
    public String guestPage(@RequestParam(value = "page", defaultValue = "0") String page, Model model, HttpServletRequest httpServletRequest) {
        int p = Integer.parseInt(page);
        if (p < 0) p = 0;
        List<MessageAndUsername> publicMessages = messageService.findAllByPrivateMessageIsFalse(PageRequest.of(p, 10));
        int allPublicMessageCount = messageService.findAllByPrivateMessageIsFalse().size();
        model.addAttribute("messages", publicMessages);
        model.addAttribute("httpServletRequest", httpServletRequest);
        model.addAttribute("currentPage", p);
        model.addAttribute("totalPublicMessages", allPublicMessageCount);
        return "welcome";
    }

    @GetMapping("/messages")
    public String messagePage(@RequestParam(value = "page", defaultValue = "0") String page, Model model, HttpServletRequest httpServletRequest) {
        int p = Integer.parseInt(page);
        if (p < 0) p = 0;
        List<MessageAndUsername> messages = messageService.findAllMessages(PageRequest.of(p, 10));
        int allMessageCount = messageService.findAllMessages().size();
        model.addAttribute("messages", messages);
        model.addAttribute("httpServletRequest", httpServletRequest);
        model.addAttribute("currentPage", p);
        model.addAttribute("totalPublicMessages", allMessageCount);
        return "messages";
    }

    @GetMapping("/myprofile")
    public String userProfile(@RequestParam(value = "page", defaultValue = "0") String page, Model model, @AuthenticationPrincipal OAuth2User principal, HttpServletRequest httpServletRequest) {
        int p = Integer.parseInt(page);
        if (p < 0) p = 0;
        User user = userService.findByGitHubId(principal.getAttribute("id"));
        List<MessageAndUsername> messages = messageService.findAllMessagesByUser(user, PageRequest.of(p, 10));
        int allMessageCount = messageService.findAllMessagesByUser(user).size();

        model.addAttribute("messages", messages);
        model.addAttribute("currentPage", p);
        model.addAttribute("totalMessages", allMessageCount);
        model.addAttribute("httpServletRequest", httpServletRequest);
        model.addAttribute("name", user.getFirstName() + " " + user.getLastName());
        model.addAttribute("userName", user.getUserName());
        model.addAttribute("profilepic", user.getImage());
        model.addAttribute("email", user.getEmail());

        return "userprofile";
    }

    @GetMapping("/myprofile/edit")
    public String editUserProfile(Model model, @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.findByGitHubId(principal.getAttribute("id"));
        model.addAttribute("formData", new EditUserFormData(user.getUserName(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getImage()));
        return "edituser";
    }

    private boolean checkIfUsernameAlreadyExists(String userName, User user) {
        return userService.findByUserName(userName).isPresent() && !userName.equals(user.getUserName());
    }

    @PostMapping("/myprofile/edit")
    public String editUserProfile(@Valid @ModelAttribute("formData") EditUserFormData userForm,
                    BindingResult bindingResult,
                    @AuthenticationPrincipal OAuth2User principal) {

        User user = userService.findByGitHubId(principal.getAttribute("id"));
        if (checkIfUsernameAlreadyExists(userForm.getUserName(),user)) {
            bindingResult.rejectValue("userName","duplicate", "Username needs to be unique");
        }
        if(bindingResult.hasErrors()) {
            return "edituser";
        }
        user.setUserName(userForm.getUserName());
        user.setFirstName(userForm.getFirstName());
        user.setLastName(userForm.getLastName());
        user.setEmail(userForm.getEmail());
        user.setImage(userForm.getImage());
        userService.save(user);
        return "redirect:/web/myprofile";
    }


    @GetMapping("/myprofile/editmessage")
    public String editMessage(Model model, @RequestParam("id") Long id, @AuthenticationPrincipal OAuth2User principal) {
        Message message = messageService.findById(id);
        User currectUser = userService.findByGitHubId(principal.getAttribute("id"));

        if (!message.getUser().getId().equals(currectUser.getId())) {
            return "redirect:/web/myprofile";
        }

        model.addAttribute("formData", new CreateMessageFormData(
                message.getTitle(),
                message.getMessageBody(),
                message.isPrivateMessage()));
        model.addAttribute("messageId", message.getId());
        return "editmessage";
    }

    @PostMapping("/myprofile/editmessage")
    public String editMessage(@Valid @ModelAttribute("formData") CreateMessageFormData messageForm,
                              BindingResult bindingResult,
                              @RequestParam("id") Long id,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addAttribute("id",id);
            return "redirect:/web/myprofile/editmessage";
        }
        Message message = messageService.findById(id);
        message.setMessageBody(messageForm.getMessageBody());
        message.setTitle(messageForm.getTitle());
        message.setPrivateMessage(messageForm.isPrivateMessage());
        message.setLastChanged(LocalDate.now());
        messageService.save(message);
        return "redirect:/web/myprofile";
    }

    @GetMapping("/myprofile/create")
    public String createMessage(Model model) {
        model.addAttribute("formData", new CreateMessageFormData());
        return "createmessage";
    }

    @PostMapping("/myprofile/create")
    public String createMessage(@Valid @ModelAttribute("formData") CreateMessageFormData messageForm,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal OAuth2User principal) {
        if (bindingResult.hasErrors()) {
            return "createmessage";
        }
        User user = userService.findByGitHubId(principal.getAttribute("id"));
        messageService.save(messageForm.toEntity(user));
        return "redirect:/web/myprofile";
    }


}
