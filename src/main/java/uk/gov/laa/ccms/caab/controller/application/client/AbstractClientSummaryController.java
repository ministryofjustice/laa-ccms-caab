package uk.gov.laa.ccms.caab.controller.application.client;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@RequiredArgsConstructor
public abstract class AbstractClientSummaryController {

  protected final CommonLookupService commonLookupService;

  protected final ClientService clientService;

  protected final ClientBasicDetailsValidator basicValidator;

  protected final ClientContactDetailsValidator contactValidator;

  protected final ClientAddressDetailsValidator addressValidator;

  protected final ClientEqualOpportunitiesMonitoringDetailsValidator opportunitiesValidator;

  protected final ClientDetailMapper clientDetailsMapper;

  protected void validateClientDetails(ClientDetails clientDetails, BindingResult bindingResult){
    basicValidator.validate(clientDetails, bindingResult);
    contactValidator.validate(clientDetails, bindingResult);
    addressValidator.validate(clientDetails, bindingResult);
    opportunitiesValidator.validate(clientDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      throw new CaabApplicationException(
          "Client submission containing missing or invalid client details.");
    }
  }

  protected void populateSummaryListLookups(ClientDetails clientDetails, Model model) {

    // Create a list of Mono calls and their respective attribute keys
    List<Pair<String, Mono<CommonLookupValueDetail>>> lookups = List.of(
        Pair.of("contactTitle",
            commonLookupService.getContactTitle(clientDetails.getTitle())),
        Pair.of("countryOfOrigin",
            commonLookupService.getCountry(clientDetails.getCountryOfOrigin())),
        Pair.of("maritalStatus",
            commonLookupService.getMaritalStatus(clientDetails.getMaritalStatus())),
        Pair.of("gender",
            commonLookupService.getGender(clientDetails.getGender())),
        Pair.of("correspondenceMethod",
            commonLookupService.getCorrespondenceMethod(clientDetails.getCorrespondenceMethod())),
        Pair.of("ethnicity",
            commonLookupService.getEthnicOrigin(clientDetails.getEthnicOrigin())),
        Pair.of("disability",
            commonLookupService.getDisability(clientDetails.getDisability())),

        //Processed differently due to optionality
        Pair.of("country",
            StringUtils.hasText(clientDetails.getCountry())
                ? commonLookupService.getCountry(
                clientDetails.getCountry())
                : Mono.just(new CommonLookupValueDetail())),
        Pair.of("correspondenceLanguage",
            StringUtils.hasText(clientDetails.getCorrespondenceLanguage())
                ? commonLookupService.getCorrespondenceLanguage(
                clientDetails.getCorrespondenceLanguage())
                : Mono.just(new CommonLookupValueDetail()))
    );

    // Fetch all Monos asynchronously
    Mono<List<CommonLookupValueDetail>> allMonos = Flux.fromIterable(lookups)
        .flatMap(pair -> pair.getRight().map(value -> Pair.of(pair.getLeft(), value)))
        .collectList()
        .doOnNext(list -> list.forEach(pair -> model.addAttribute(pair.getLeft(), pair.getRight())))
        .map(list -> list.stream().map(Pair::getRight).collect(Collectors.toList()));

    // Block until all results are available
    allMonos.block();
  }
}
