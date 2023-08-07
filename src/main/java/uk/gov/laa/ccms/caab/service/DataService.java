package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.EXCLUDED_APPLICATION_TYPE_CODES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.*;

@Service
@Slf4j
public class DataService {
    private final WebClient dataWebClient;

    private final DataServiceErrorHandler dataServiceErrorHandler;

    public DataService(@Qualifier("dataWebClient") WebClient dataWebClient,
        DataServiceErrorHandler dataServiceErrorHandler) {
        this.dataWebClient = dataWebClient;
        this.dataServiceErrorHandler = dataServiceErrorHandler;
    }

    public Mono<UserDetail> getUser(String loginId){

        return dataWebClient
                .get()
                .uri("/users/{loginId}", loginId)
                .retrieve()
                .bodyToMono(UserDetail.class)
                .onErrorResume(e -> dataServiceErrorHandler.handleUserError(loginId, e));
    }

    public Mono<CommonLookupDetail> getCommonValues(String type, String code, String sort) {

        return dataWebClient
                .get()
                .uri(builder -> builder.path("/lookup/common")
                        .queryParamIfPresent("type", Optional.ofNullable(type))
                        .queryParamIfPresent("code", Optional.ofNullable(code))
                        .queryParamIfPresent("sort", Optional.ofNullable(sort))
                        .build())
                .retrieve()
                .bodyToMono(CommonLookupDetail.class)
                .onErrorResume(e -> dataServiceErrorHandler.handleCommonValuesError(type, code, sort, e));
    }

    public List<CommonLookupValueDetail> getApplicationTypes() {
        CommonLookupDetail commonLookupDetail = getCommonValues(COMMON_VALUE_APPLICATION_TYPE, null, null).block();

        return Optional.ofNullable(commonLookupDetail)
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList())
            .stream()
            .filter(applicationType -> {
                String code = applicationType.getCode().toUpperCase();
                return !EXCLUDED_APPLICATION_TYPE_CODES.contains(code);
            })
            .collect(Collectors.toList());
    }

    public List<CommonLookupValueDetail> getGenders() {
        CommonLookupDetail commonLookupValues = getCommonValues(COMMON_VALUE_GENDER, null, null).block();
        return Optional.ofNullable(commonLookupValues)
                .map(CommonLookupDetail::getContent)
                .orElse(Collections.emptyList());
    }

    public List<CommonLookupValueDetail> getUniqueIdentifierTypes() {
        CommonLookupDetail commonLookupValues = getCommonValues(COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE, null, null).block();
        return Optional.ofNullable(commonLookupValues)
                .map(CommonLookupDetail::getContent)
                .orElse(Collections.emptyList());
    }

    public List<CommonLookupValueDetail> getCategoriesOfLaw(List<String> codes) {
        CommonLookupDetail commonLookupDetail = getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW, null, null).block();

        return Optional.ofNullable(commonLookupDetail)
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList())
            .stream()
            .filter(category -> codes.contains(category.getCode()))
            .collect(Collectors.toList());
    }

    public List<CommonLookupValueDetail> getAllCategoriesOfLaw() {
        CommonLookupDetail commonLookupDetail = getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW, null, null).block();

        return Optional.ofNullable(commonLookupDetail)
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList());
    }

    public Mono<FeeEarnerDetail> getFeeEarners(Integer providerId) {
        return dataWebClient
            .get()
            .uri(builder -> builder.path("/fee-earners")
                .queryParam("provider-id", providerId)
                .build())
            .retrieve()
            .bodyToMono(FeeEarnerDetail.class)
            .onErrorResume(e -> dataServiceErrorHandler.handleFeeEarnersError(providerId, e));
    }

    public Mono<AmendmentTypeLookupDetail> getAmendmentTypes(String applicationType, String sort) {
        return dataWebClient
                .get()
                .uri(builder -> builder.path("/lookup/common")
                        .queryParamIfPresent("'application-type'", Optional.ofNullable(applicationType))
                        .queryParamIfPresent("sort", Optional.ofNullable(sort))
                        .build())
                .retrieve()
                .bodyToMono(AmendmentTypeLookupDetail.class)
                .onErrorResume(e -> dataServiceErrorHandler.handleAmendmentTypeLookupError(applicationType, sort, e));
    }

}

