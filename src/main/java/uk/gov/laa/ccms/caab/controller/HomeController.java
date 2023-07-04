package uk.gov.laa.ccms.caab.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.UserDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

@Controller
@RequiredArgsConstructor
public class HomeController {

    public static final String NO_OUTSTANDING_ACTIONS = "No Outstanding Actions";
    private final DataService dataService;

    private final SoaGatewayService soaGatewayService;

    @GetMapping("/")
    public String home(Model model){

        UserDetails user = (UserDetails) model.getAttribute("user");
        user = dataService.getUser(user.getLoginId()).block();

        model.addAttribute("user", user);

        // Retrieve a summary of the User's Notifications & Actions from the SOA Gateway
        NotificationSummary notificationSummary = soaGatewayService.getNotificationsSummary(
            user.getLoginId(), user.getUserType()).block();

        boolean showNotifications = notificationSummary != null;
        model.addAttribute("showNotifications", showNotifications);

        if ( showNotifications ) {
            /*
             * Format the display message for Overdue Actions.
             * 'x overdue', or 'none overdue'
             */
            final String overdueActionsMsg = String.format("%s overdue",
                notificationSummary.getOverdueActions() > 0 ?
                    notificationSummary.getOverdueActions().toString() : "none");

            /*
             * Format the overall display message for Actions.
             * 'x Outstanding Actions (x overdue)', or
             * 'No Outstanding Actions'
             */
            final String actionsMsg = notificationSummary.getStandardActions() > 0 ?
                String.format("%s Outstanding Actions (%s)",
                    notificationSummary.getStandardActions(),
                    overdueActionsMsg) : NO_OUTSTANDING_ACTIONS;

            /*
             * Format the display message for Notifications.
             * 'View Notifications (x outstanding)' or,
             * 'View Notifications (none outstanding)'
             */
            final String notificationsMsg = String.format("View Notifications (%s outstanding)",
                notificationSummary.getNotifications() > 0 ?
                    notificationSummary.getNotifications().toString() : "none");

            model.addAttribute("actionsMsg", actionsMsg);
            model.addAttribute("notificationsMsg", notificationsMsg);
        }

        return "home";
    }

}
