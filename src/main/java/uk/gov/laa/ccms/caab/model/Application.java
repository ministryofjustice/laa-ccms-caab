package uk.gov.laa.ccms.caab.model;

import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Extension of Caab-Api's ApplicationDetail to add extra display and processing attributes.
 */
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
public class Application extends ApplicationDetail {

  private String certificateType;

  private Date dateCreated;

  /**
   * Construct a new Application object.
   *
   * @param caseReferenceNumber - the case reference
   * @param provider - the provider
   * @param categoryOfLaw = the category of law
   * @param client - the client
   */
  public Application(String caseReferenceNumber, ApplicationDetailProvider provider,
      StringDisplayValue categoryOfLaw, ApplicationDetailClient client) {
    super(caseReferenceNumber, provider, categoryOfLaw, client);
  }

}
