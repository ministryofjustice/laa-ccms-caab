package uk.gov.laa.ccms.caab.controller.application.client;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ChildBindingResult;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

/**
 * Abstract controller for handling Client Summary Information.
 */
@RequiredArgsConstructor
public abstract class AbstractClientSummaryController {

  protected final LookupService lookupService;

  protected final ClientService clientService;

  protected final ClientBasicDetailsValidator basicValidator;

  protected final ClientContactDetailsValidator contactValidator;

  protected final ClientAddressDetailsValidator addressValidator;

  protected final ClientEqualOpportunitiesMonitoringDetailsValidator opportunitiesValidator;

  protected final ClientDetailMapper clientDetailsMapper;

  protected void validateClientFlowFormData(
      final ClientFlowFormData clientFlowFormData,
      final BindingResult bindingResult) {
    basicValidator.validate(clientFlowFormData.getBasicDetails(),
        new ChildBindingResult(bindingResult, "basicDetails"));
    contactValidator.validate(clientFlowFormData.getContactDetails(),
        new ChildBindingResult(bindingResult, "contactDetails"));
    addressValidator.validate(clientFlowFormData.getAddressDetails(),
        new ChildBindingResult(bindingResult, "addressDetails"));
    opportunitiesValidator.validate(
        clientFlowFormData.getMonitoringDetails(),
        new ChildBindingResult(bindingResult, "monitoringDetails"));

    if (bindingResult.hasErrors()) {
      throw new CaabApplicationException(
          "Client submission containing missing or invalid client details.");
    }
  }

  protected Mono<Void> populateSummaryListLookups(
      final ClientFlowFormData clientFlowFormData, final Model model) {

    // Create a list of Mono calls and their respective attribute keys
    final List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups =
        lookupService.getClientLookups(clientFlowFormData);

    // Fetch all Monos asynchronously
    return lookupService.addCommonLookupsToModel(lookups, model);
  }
}
