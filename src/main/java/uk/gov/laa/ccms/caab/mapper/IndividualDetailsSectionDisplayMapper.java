package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;

/**
 * Mapper interface for converting ebs application data to an IndividualDetailsSectionDisplay
 * object.
 *
 * @author Jamie Briggs
 */
@Mapper(componentModel = "spring")
public interface IndividualDetailsSectionDisplayMapper {


  @Mapping(target = "generalDetails", source = "opponentDetail")
  @Mapping(target = "employmentDetails", source = "opponentDetail")
  @Mapping(target = "addressContactDetails", source = "opponentDetail")
  IndividualDetailsSectionDisplay toIndividualDetailsSectionDisplay(OpponentDetail opponentDetail);


}
