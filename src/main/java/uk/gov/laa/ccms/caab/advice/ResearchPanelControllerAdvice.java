package uk.gov.laa.ccms.caab.advice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller advice class responsible for adding the research panel link to the model.
 */
@ControllerAdvice
public class ResearchPanelControllerAdvice {

  private static final String RESEARCH_PANEL_ATTRIBUTE = "researchPanelLink";

  private final String researchPanelLink;

  public ResearchPanelControllerAdvice(
      @Value("${laa.ccms.footer.research-panel-link}") String researchPanelLink) {
    this.researchPanelLink = researchPanelLink;
  }

  /**
   * Adds the user research panel link to the model.
   *
   * @param model The Model object to which attributes will be added.
   */
  @ModelAttribute
  public void addResearchPanelLink(Model model) {
    model.addAttribute(RESEARCH_PANEL_ATTRIBUTE, researchPanelLink);
  }
}
