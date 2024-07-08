package uk.gov.laa.ccms.caab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the CAAB Application.
 */

@SpringBootApplication
@EnableAsync
public class CaabApplication {

  public static void main(String[] args) {
    SpringApplication.run(CaabApplication.class, args);
  }

}
