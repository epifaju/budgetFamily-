package com.invoiceai.dto.request;

import com.invoiceai.dto.request.DeviceInfoRequest;
import com.invoiceai.validation.PasswordPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String name;

    @NotBlank
    @Size(min = 8)
    @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
    private String password;

    @AssertTrue(message = "Vous devez accepter la politique de confidentialité")
    public boolean isAcceptedPrivacyPolicy() {
        return Boolean.TRUE.equals(acceptedPrivacyPolicy);
    }

    private Boolean acceptedPrivacyPolicy;

    @Valid
    private DeviceInfoRequest device;
}




