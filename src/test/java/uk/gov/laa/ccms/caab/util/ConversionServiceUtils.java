package uk.gov.laa.ccms.caab.util;

import org.springframework.format.support.FormattingConversionService;
import uk.gov.laa.ccms.caab.config.StringToCaseContextConverter;

public final class ConversionServiceUtils {

  public static FormattingConversionService getConversionService() {
    FormattingConversionService conversionService = new FormattingConversionService();
    conversionService.addConverter(new StringToCaseContextConverter());
    return conversionService;
  }
}
