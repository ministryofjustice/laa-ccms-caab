package uk.gov.laa.ccms.caab.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
class ProviderRequestsMapperTest {

  private ProviderRequestsMapper mapper = new ProviderRequestsMapperImpl();

  @BeforeEach
  void setUp() {
    mapper = new ProviderRequestsMapperImpl();
  }



}