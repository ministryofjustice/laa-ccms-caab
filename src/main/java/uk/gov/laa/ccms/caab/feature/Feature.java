package uk.gov.laa.ccms.caab.feature;

import lombok.Getter;

/** Enumeration to describe available features. */
@Getter
public enum Feature {
  AMENDMENTS("amendments");

  private final String name;

  Feature(String name) {
    this.name = name;
  }
}
