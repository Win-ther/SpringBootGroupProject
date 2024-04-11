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
import se.iths.springbootgroupproject.services.LibreTranslateService;
import se.iths.springbootgroupproject.services.MessageService;
import se.iths.springbootgroupproject.services.UserService;
import se.iths.springbootgroupproject.services.WordcountService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web")
public class WebController {

    private final MessageService messageService;
    private final UserService userService;
    private final LibreTranslateService libreTranslateService;
    private final WordcountService wordcountService;

    public WebController(MessageService messageService, UserService userService, LibreTranslateService libreTranslateService, WordcountService wordcountService) {
        this.messageService = messageService;
        this.userService = userService;
        this.libreTranslateService = libreTranslateService;
        this.wordcountService = wordcountService;
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

    @GetMapping("/user")
    public String messagePageUser(@RequestParam(value = "page", defaultValue = "0") String page, @RequestParam("username")String userName , Model model, HttpServletRequest httpServletRequest) {
        int p = Integer.parseInt(page);
        if (p < 0) p = 0;
        User user = userService.findByUserName(userName).get();
        List<MessageAndUsername> messages = messageService.findAllMessagesByUser(user,PageRequest.of(p, 10));
        int allMessageCount = messageService.findAllMessagesByUser(user).size();
        List<String> distinctUserNames = userService.findAll().stream().map(User::getUserName).collect(Collectors.toList());
        distinctUserNames.add("Show All");
        List<MessageAndUsername> distinctUserMessages = messages.stream()
                .filter(message -> message.userUserName().equals(userName))
                .toList();
        model.addAttribute("userList", distinctUserNames);
        model.addAttribute("messages", distinctUserMessages);
        model.addAttribute("currentPage", p);
        model.addAttribute("currentUser", userName);
        model.addAttribute("httpServletRequest", httpServletRequest);
        model.addAttribute("totalPublicMessages", allMessageCount);
        return "messagesfromuser";
    }

    @GetMapping("/messages")
    public String messagePage(@RequestParam(value = "page", defaultValue = "0") String page, Model model, HttpServletRequest httpServletRequest) {
        int p = Integer.parseInt(page);
        if (p < 0) p = 0;
        List<MessageAndUsername> messages = messageService.findAllMessages(PageRequest.of(p, 10));
        List<String> distinctUserNames = userService.findAll().stream().map(User::getUserName).toList();
        int allMessageCount = messageService.findAllMessages().size();
        model.addAttribute("userList", distinctUserNames);
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
        if (checkIfUsernameAlreadyExists(userForm.getUserName(), user)) {
            bindingResult.rejectValue("userName", "duplicate", "Username needs to be unique");
        }
        if (bindingResult.hasErrors()) {
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

    @GetMapping("/myprofile/deletemessage")
    public String deleteMessage(@RequestParam("id") Long id, @AuthenticationPrincipal OAuth2User principal) {
        Message message = messageService.findById(id);
        User currentUser = userService.findByGitHubId(principal.getAttribute("id"));

        if (!message.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/web/myprofile";
        }
        messageService.delete(message);

        return "redirect:/web/myprofile";
    }

    @PostMapping("/myprofile/editmessage")
    public String editMessage(@Valid @ModelAttribute("formData") CreateMessageFormData messageForm,
                              BindingResult bindingResult,
                              @RequestParam("id") Long id,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addAttribute("id", id);
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

    @GetMapping("/messages/translate")
    public String translateMessage(Model model, @RequestParam("id") Long id) {
        Message message = messageService.findById(id);
        String translatedTitle = libreTranslateService.translateMessage(message.getTitle());
        String translatedMessage = libreTranslateService.translateMessage(message.getMessageBody());
        model.addAttribute("title", translatedTitle);
        model.addAttribute("message", translatedMessage);
        model.addAttribute("userName", message.getUser().getUserName());
        model.addAttribute("date", message.getDate());
        model.addAttribute("lastChanged", message.getLastChanged());
        return "translatemessage";
    }
    @GetMapping("/messages/analyse")
    public String analyseMessage(Model model, @RequestParam("id") Long id) {
        Message message = messageService.findById(id);
        List<String> analysedBody = Arrays.stream(wordcountService.analyseText(message.getMessageBody())).toList();
        Map<String,String> map = analysedBody.stream().map(s -> s.replaceAll("[{\"}]","")).map(s -> s.split(":")).collect(Collectors.toMap(strings -> strings[0], strings -> strings[1]));

        model.addAttribute("title", message.getTitle());
        model.addAttribute("message", message.getMessageBody());
        model.addAttribute("userName",message.getUser().getUserName());
        model.addAttribute("date", message.getDate());
        model.addAttribute("lastChanged",message.getLastChanged());
        model.addAttribute("analysedBody", map);
        return "analysemessage";
    }

    @GetMapping("/messageshtmx")
    public String messagePageHTMX(Model model, HttpServletRequest httpServletRequest) {
        var messages = messageService.getPage(0,2);
        var length = messageService.findAllMessages().size();
        model.addAttribute("nextpage", messages.getLast().id());
        model.addAttribute("messages", messages);
        model.addAttribute("httpServletRequest", httpServletRequest);
        model.addAttribute("length", length);
        return "messageshtmx";
    }
    @GetMapping("messageshtmx/nextpage")
    public String loadMore(Model model, @RequestParam(defaultValue = "1") String page) {
        int p = Integer.parseInt(page);
        var messages = messageService.getPage(p, 2);
        var length = messageService.findAllMessages().size();
        model.addAttribute("length", length);
        model.addAttribute("nextpage", messages.getLast().id());
        model.addAttribute("messages", messages);
        return "nextpage";
    }
    @GetMapping("messageshtmx/edit/{id}")
    public String editHtmx(Model model, @PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        var message = messageService.findById(id);
        model.addAttribute("message", message);
        User currectUser = userService.findByGitHubId(principal.getAttribute("id"));
        if (!message.getUser().getId().equals(currectUser.getId())) {
            return "returnRowHtmx";
        }
        return "htmxedit";
    }
    @GetMapping("messageshtmx/{id}")
    public String returnRow(Model model, @PathVariable Long id) {
        var message = messageService.findById(id);
        model.addAttribute("message", message);
        return "returnRowHtmx";
    }
    @PostMapping("messageshtmx/{id}")
    public String saveRow(Model model, @ModelAttribute("formData") CreateMessageFormData messageForm, @PathVariable Long id) {
        Message message = messageService.findById(id);
        message.setMessageBody(messageForm.getMessageBody());
        message.setTitle(messageForm.getTitle());
        message.setPrivateMessage(messageForm.isPrivateMessage());
        message.setLastChanged(LocalDate.now());
        messageService.save(message);
        model.addAttribute("message", message);
        return "returnRowHtmx";
    }
}
