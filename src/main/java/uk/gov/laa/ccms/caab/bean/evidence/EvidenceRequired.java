package uk.gov.laa.ccms.caab.bean.evidence;

import java.io.Serializable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Represents a required item of evidence. */
@Data
@Slf4j
public class EvidenceRequired implements Serializable {

  /** The code for the evidence. */
  private final String code;

  /** The description of the evidence. */
  private final String description;

  /** Flag to indicate whether the currently uploaded document(s) provide this evidence. */
  private Boolean provided;
}
