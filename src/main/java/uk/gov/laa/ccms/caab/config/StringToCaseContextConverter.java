package uk.gov.laa.ccms.caab.config;

import org.springframework.core.convert.converter.Converter;
import uk.gov.laa.ccms.caab.constants.CaseContext;

/**
 * A converter that transforms a String value into a corresponding CaseContext enum. Implements the
 * Spring Framework's {@code Converter} interface for type conversion.
 *
 * @author Jamie Briggs
 */
public class StringToCaseContextConverter implements Converter<String, CaseContext> {
  @Override
  public CaseContext convert(String path) {
    return CaseContext.fromPathValue(path);
  }
}
