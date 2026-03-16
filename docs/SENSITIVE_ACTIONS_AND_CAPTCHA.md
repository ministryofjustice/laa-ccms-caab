# Sensitive Actions and CAPTCHA Implementation Recommendation

## 1. Introduction
This document outlines the sensitive actions identified within the modernized Provider User Interface (PUI) and provides a comprehensive evaluation of CAPTCHA solutions to mitigate the risk of automated attacks, such as bot submissions and credential stuffing.

## 2. Identified Sensitive Actions
The following actions within the `laa-ccms-caab` application have been identified as sensitive, requiring additional protection due to their impact on legal aid applications, case management, or user data.

| Action Category | Description | Target Controllers | Risk Level |
|-----------------|-------------|--------------------|------------|
| **Application Submission** | The final submission of a legal aid application or amendment. | `ApplicationSubmissionController` | High |
| **Case Amendments** | Initiating or submitting changes to an existing active case. | `AmendCaseController` | High |
| **Provider Switching** | Changing the firm on whose behalf the user is acting. | `ProviderController` | Medium |
| **Evidence Management** | Uploading or removing documents/evidence related to a case. | `EvidenceSectionController` | Medium/High |
| **Notification Responses** | Submitting responses to notifications or providing additional documents requested by LAA. | `ActionsAndNotificationsController` | Medium/High |
| **User Authentication** | Login and session management (handled by SAML/External Identity Provider). | N/A (External) | High |

## 3. CAPTCHA Solution Options
Three main CAPTCHA providers were evaluated based on security, privacy (GDPR compliance), user experience (UX), and accessibility.

### A. hCaptcha (Recommended)
*   **Pros:** Strong focus on privacy, GDPR/ePrivacy compliant, high security, accessible options.
*   **Cons:** Some "hard" challenges can be time-consuming for users.
*   **Best For:** Balancing high security with privacy requirements.

### B. Friendly CAPTCHA
*   **Pros:** Best user experience (non-interactive, proof-of-work based), GDPR compliant, excellent accessibility.
*   **Cons:** May be less effective against highly sophisticated/distributed bot farms compared to hCaptcha.
*   **Best For:** Optimal user experience while maintaining a baseline of protection.

### C. Google reCAPTCHA (v2 / v3 / Enterprise)
*   **Pros:** Industry standard, very robust security, seamless UX in v3.
*   **Cons:** Privacy concerns (data shared with Google), potential GDPR issues in strictly regulated environments.
*   **Best For:** Maximum security where Google data processing is acceptable.

## 4. Recommendation
**Primary Recommendation: hCaptcha**

hCaptcha is recommended as the primary solution for the LAA PUI due to:
1.  **Privacy:** It offers superior privacy protection compared to reCAPTCHA, which is critical for government services handling sensitive legal data.
2.  **Security:** It provides robust protection against both simple bots and sophisticated human-aided solving services.
3.  **Flexibility:** It supports "Passive" modes (similar to reCAPTCHA v3) for low-risk actions and "Interactive" modes for high-risk submissions.

## 5. Configuration and Implementation

### Recommended Configuration
| Parameter | Recommended Value | Description |
|-----------|-------------------|-------------|
| **Site Key** | Provided by hCaptcha | Public key used in the frontend. |
| **Secret Key** | Provided by hCaptcha | Private key used for backend verification. |
| **Difficulty** | "Always On" or "Auto" | Set to "Always On" for high-risk submission endpoints. |
| **Score Threshold** | 0.7 (if using Enterprise/Passive) | Threshold for considering a request "human". |

### Integration Points
1.  **Frontend:** Include hCaptcha JavaScript in `src/main/resources/templates/partials/head.html` and the widget in relevant forms (e.g., submission buttons).
2.  **Backend:**
    - Create a `CaptchaService` to verify the `h-captcha-response` token against the hCaptcha API.
    - Implement a `@CaptchaProtected` annotation or a Spring Security Filter to intercept requests to sensitive controllers.
    - Update `ApplicationSubmissionController.applicationDeclarationPost` to verify the CAPTCHA token before proceeding with the final submission.

## 6. Test Scenarios

| Scenario ID | Scenario Description | Expected Result |
|-------------|----------------------|-----------------|
| **TS-01** | Submit application with valid CAPTCHA token. | Application is successfully submitted. |
| **TS-02** | Submit application with missing CAPTCHA token. | Submission is blocked; user sees an error message. |
| **TS-03** | Submit application with expired or invalid CAPTCHA token. | Submission is blocked; user is prompted to try again. |
| **TS-04** | Verify CAPTCHA appears on the "Provider Switch" page. | CAPTCHA widget is visible and interactive. |
| **TS-05** | Verify accessibility (Keyboard navigation/Screen reader) of the CAPTCHA widget. | Users with disabilities can complete the challenge. |
| **TS-06** | Passive CAPTCHA verification for search actions. | Request is allowed if score > threshold, otherwise prompts for challenge. |

## 7. Approval
- **Product Owner Approval:** @Dora Tihanyi (Pending)
- **Technical Lead Approval:** @Michael Farrell (Pending)
