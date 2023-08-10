package uk.gov.laa.ccms.caab.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ClientResultRowDisplay {

    private String firstName;
    private String surname;
    private String surnameAtBirth;
    private String postalCode;
    private String clientReferenceNumber;

    public String getPostCodeDistrict() {
        String postCodeDistrict = null;
        if (this.postalCode != null) {
            if (this.postalCode.trim().length() > 2 && this.postalCode.trim().contains(" ")) {
                int spacePos = this.postalCode.indexOf(" ");
                postCodeDistrict = this.postalCode.substring(0, spacePos);
            } else {
                postCodeDistrict = this.postalCode;
            }
        }
        return postCodeDistrict;
    }
}
