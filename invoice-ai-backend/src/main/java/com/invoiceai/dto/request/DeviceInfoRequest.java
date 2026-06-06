package com.invoiceai.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class DeviceInfoRequest {

    @NotBlank
    @Size(max = 128)
    private String deviceId;

    @Size(max = 255)
    private String deviceName;

    @Size(max = 32)
    private String platform;
}
