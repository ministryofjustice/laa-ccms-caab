package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.EXCLUDED_APPLICATION_TYPE_CODES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;

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
                .uri(builder -> builder.path("/common-lookup-values")
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

        List<CommonLookupValueDetail> applicationTypes = commonLookupDetail.getContent().stream()
                .filter(applicationType -> {
                    String code = applicationType.getCode().toUpperCase();
                    return !EXCLUDED_APPLICATION_TYPE_CODES.contains(code);
                })
                .collect(Collectors.toList());

        return applicationTypes;
    }

    public List<CommonLookupValueDetail> getCategoriesOfLaw(List<String> codes) {
        CommonLookupDetail commonLookupDetail = getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW, null, null).block();
        List<CommonLookupValueDetail> categoriesOfLaw = commonLookupDetail.getContent().stream()
                    .filter(category -> codes.contains(category.getCode()))
                    .collect(Collectors.toList());

        return categoriesOfLaw;
    }

    public List<CommonLookupValueDetail> getAllCategoriesOfLaw() {
        CommonLookupDetail commonLookupDetail = getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW, null, null).block();
        List<CommonLookupValueDetail> categoriesOfLaw = commonLookupDetail.getContent().stream()
                .collect(Collectors.toList());

        return categoriesOfLaw;
    }

}

