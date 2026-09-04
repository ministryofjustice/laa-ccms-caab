package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/** Form data for pre-certificate and legal help costs. */
@Data
public class PreCertificateAndLegalHelpCostsFormData {

  private String preCertificateCosts;

  private String legalHelpCosts;

  private String officeCode;

  private String uniqueFileNumber;
}
