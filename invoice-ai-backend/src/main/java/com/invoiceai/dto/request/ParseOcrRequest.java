package com.invoiceai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParseOcrRequest {

    @NotBlank(message = "Le texte OCR est obligatoire")
    @Size(max = 12000, message = "Le texte OCR est trop long")
    private String rawText;
}
