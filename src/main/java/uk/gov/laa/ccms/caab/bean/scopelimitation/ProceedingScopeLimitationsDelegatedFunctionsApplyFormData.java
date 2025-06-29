package uk.gov.laa.ccms.caab.bean.scopelimitation;

import java.util.List;

/** Form data for storing delegated functions indicators within a proceeding's scope limitations. */
public record ProceedingScopeLimitationsDelegatedFunctionsApplyFormData(
    List<ScopeLimitationDelegatedFunctionApplyFormData> scopeLimitationDataList) {}
