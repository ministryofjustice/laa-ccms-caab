package uk.gov.laa.ccms.caab.testutil;

import uk.gov.laa.ccms.data.model.InvoiceAssessmentAttribute;
import uk.gov.laa.ccms.data.model.InvoiceAssessmentDetails;
import uk.gov.laa.ccms.data.model.InvoiceAssessmentInstance;
import uk.gov.laa.ccms.soa.gateway.model.InvoiceDataResponse;

/** Migrates existing historical test shapes to the retained-invoice transport contract. */
public final class RetainedInvoiceFixture {

  private RetainedInvoiceFixture() {}

  /** Preserves malformed fixture structures so availability assertions remain meaningful. */
  public static InvoiceAssessmentDetails retained(
      final String caseReference, final long billingId, final InvoiceDataResponse source) {
    final InvoiceAssessmentDetails result =
        new InvoiceAssessmentDetails()
            .caseReference(caseReference)
            .billingIncidentId(billingId)
            .assessmentId(501L)
            .billingProviderId("10");
    if (source.getOpaResponse() == null) {
      return result.instances(null);
    }
    long instanceId = 1000;
    long attributeId = 100;
    for (final var type : source.getOpaResponse()) {
      if (type == null) {
        result.addInstancesItem(null);
      } else if (type.getInstances() == null) {
        result.addInstancesItem(
            new InvoiceAssessmentInstance()
                .id(++instanceId)
                .entityName(type.getEntityName())
                .attributes(null));
      } else {
        for (final var sourceInstance : type.getInstances()) {
          if (sourceInstance == null) {
            result.addInstancesItem(null);
            continue;
          }
          final InvoiceAssessmentInstance instance =
              new InvoiceAssessmentInstance()
                  .id(++instanceId)
                  .entityName(type.getEntityName())
                  .label(sourceInstance.getInstanceLabel());
          if (sourceInstance.getAttributes() == null) {
            instance.attributes(null);
          } else {
            for (final var attribute : sourceInstance.getAttributes()) {
              instance.addAttributesItem(
                  attribute == null
                      ? null
                      : new InvoiceAssessmentAttribute()
                          .id(++attributeId)
                          .name(attribute.getAttribute())
                          .responseType(attribute.getResponseType())
                          .responseValue(attribute.getResponseValue())
                          .responseText(attribute.getResponseText()));
            }
          }
          result.addInstancesItem(instance);
        }
      }
    }
    return result;
  }
}
