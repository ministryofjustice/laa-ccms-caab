package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
      ClientFlowFormData clientFlowFormData,
      BindingResult bindingResult) {
    basicValidator.validate(clientFlowFormData.getBasicDetails(), bindingResult);
    contactValidator.validate(clientFlowFormData.getContactDetails(), bindingResult);
    addressValidator.validate(clientFlowFormData.getAddressDetails(), bindingResult);
    opportunitiesValidator.validate(
        clientFlowFormData.getMonitoringDetails(), bindingResult);

    if (bindingResult.hasErrors()) {
      throw new CaabApplicationException(
          "Client submission containing missing or invalid client details.");
    }
  }

  protected void populateSummaryListLookups(ClientFlowFormData clientFlowFormData, Model model) {

    // Create a list of Mono calls and their respective attribute keys
    List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups = List.of(
        Pair.of("contactTitle",
            lookupService.getCommonValue(
                COMMON_VALUE_CONTACT_TITLE,
                clientFlowFormData.getBasicDetails().getTitle())),
        Pair.of("countryOfOrigin",
            lookupService.getCountry(
                clientFlowFormData.getBasicDetails().getCountryOfOrigin())),
        Pair.of("maritalStatus",
            lookupService.getCommonValue(COMMON_VALUE_MARITAL_STATUS,
                clientFlowFormData.getBasicDetails().getMaritalStatus())),
        Pair.of("gender",
            lookupService.getCommonValue(
                COMMON_VALUE_GENDER,
                clientFlowFormData.getBasicDetails().getGender())),
        Pair.of("correspondenceMethod",
            lookupService.getCommonValue(COMMON_VALUE_CORRESPONDENCE_METHOD,
                clientFlowFormData.getContactDetails().getCorrespondenceMethod())),
        Pair.of("ethnicity",
            lookupService.getCommonValue(COMMON_VALUE_ETHNIC_ORIGIN,
                clientFlowFormData.getMonitoringDetails().getEthnicOrigin())),
        Pair.of("disability",
            lookupService.getCommonValue(COMMON_VALUE_DISABILITY,
                clientFlowFormData.getMonitoringDetails().getDisability())),

        //Processed differently due to optionality
        Pair.of("country",
            StringUtils.hasText(clientFlowFormData.getAddressDetails().getCountry())
                ? lookupService.getCountry(
                clientFlowFormData.getAddressDetails().getCountry())
                : Mono.just(Optional.of(new CommonLookupValueDetail()))),
        Pair.of("correspondenceLanguage",
            StringUtils.hasText(clientFlowFormData.getContactDetails().getCorrespondenceLanguage())
                ? lookupService.getCommonValue(COMMON_VALUE_CORRESPONDENCE_LANGUAGE,
                clientFlowFormData.getContactDetails().getCorrespondenceLanguage())
                : Mono.just(Optional.of(new CommonLookupValueDetail())))
    );

    // Fetch all Monos asynchronously
    Mono<List<Optional<CommonLookupValueDetail>>> allMonos = Flux.fromIterable(lookups)
        .flatMap(pair -> pair.getRight().map(value -> Pair.of(pair.getLeft(), value)))
        .collectList()
        .doOnNext(list -> list.forEach(pair -> model.addAttribute(pair.getLeft(), pair.getRight())))
        .map(list -> list.stream().map(Pair::getRight).collect(Collectors.toList()));

    // Block until all results are available
    allMonos.block();
  }
}
