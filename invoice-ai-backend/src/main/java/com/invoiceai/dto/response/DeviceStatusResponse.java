package com.invoiceai.dto.response;

import java.time.LocalDateTime;
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
public class DeviceStatusResponse {

    private String deviceId;
    private boolean registered;
    private boolean canSync;
    private boolean premium;
    private int activeDeviceCount;
    private LocalDateTime lastSeenAt;
}
