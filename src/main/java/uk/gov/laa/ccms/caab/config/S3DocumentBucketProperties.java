package uk.gov.laa.ccms.caab.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for the S3 Document Bucket. */
@Getter
@Setter
@AllArgsConstructor
@ConfigurationProperties(prefix = "laa.ccms.s3.buckets.document-bucket")
public class S3DocumentBucketProperties {

  private final String name;

  private final Long urlDuration;
}
