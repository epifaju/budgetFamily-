package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImageUploadResponse {
    String imageUrl;
}
