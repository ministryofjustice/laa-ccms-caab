package uk.gov.laa.ccms.caab.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClientResultRowDisplay {

    private String firstName;
    private String surname;
    private String surnameAtBirth;
    private String postalCode;
    private String clientReferenceNumber;


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getSurnameAtBirth() {
        return surnameAtBirth;
    }

    public void setSurnameAtBirth(String surnameAtBirth) {
        this.surnameAtBirth = surnameAtBirth;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getClientReferenceNumber() {
        return clientReferenceNumber;
    }

    public void setClientReferenceNumber(String clientReferenceNumber) {
        this.clientReferenceNumber = clientReferenceNumber;
    }


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
