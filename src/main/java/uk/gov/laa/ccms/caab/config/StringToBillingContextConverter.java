package uk.gov.laa.ccms.caab.config;

import org.springframework.core.convert.converter.Converter;
import uk.gov.laa.ccms.caab.constants.BillingContext;

/**
 * A converter that transforms a String value into a corresponding BillingContext enum, so the
 * billing submission routes can take {@code bill} or {@code poa} as a path variable.
 */
public class StringToBillingContextConverter implements Converter<String, BillingContext> {

  @Override
  public BillingContext convert(final String path) {
    return BillingContext.fromPathValue(path);
  }
}
