package uk.gov.laa.ccms.caab.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.UserResponse;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DataService dataService;

    private final SoaGatewayService soaGatewayService;

    @GetMapping("/")
    public String home(Model model){

        UserResponse user = (UserResponse) model.getAttribute("user");
        user = dataService.getUser(user.getLoginId()).block();

        model.addAttribute("user", user);

        NotificationSummary notificationSummary = soaGatewayService.getNotificationsSummary(user.getLoginId()).block();

        model.addAttribute("notificationCounts", notificationSummary);

        return "home";
    }

}
