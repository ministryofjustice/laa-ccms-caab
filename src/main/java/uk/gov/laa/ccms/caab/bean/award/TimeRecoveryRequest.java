package uk.gov.laa.ccms.caab.bean.award;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/** Writable fields of the time-related recovery API request. */
public record TimeRecoveryRequest(
    @JsonProperty("triggering_event") String triggeringEvent,
    @JsonProperty("effective_date") LocalDate effectiveDate,
    @JsonProperty("time_related_recovery_details") String timeRelatedRecoveryDetails) {}
