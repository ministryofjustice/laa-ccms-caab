package uk.gov.laa.ccms.caab.logging;

import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.json.JsonWriter.ValueProcessor;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/** Replaces the ECS {@code error.message} with a marker, as an exception message can echo data. */
public class ErrorMessageRedactingJsonMembersCustomizer
    implements StructuredLoggingJsonMembersCustomizer<Object> {

  static final String REDACTED = "[exception message omitted]";

  @Override
  public void customize(Members<Object> members) {
    members.applyingValueProcessor(
        ValueProcessor.of(String.class, value -> REDACTED)
            .whenHasPath(path -> path.toString().equals("error.message")));
  }
}
