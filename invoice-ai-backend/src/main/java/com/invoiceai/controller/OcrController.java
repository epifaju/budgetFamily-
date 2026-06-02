package com.invoiceai.controller;

import com.invoiceai.dto.request.ParseOcrRequest;
import com.invoiceai.dto.response.ParsedOcrResponse;
import com.invoiceai.service.OcrAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrAiService ocrAiService;

    @PostMapping("/parse")
    public ResponseEntity<ParsedOcrResponse> parseReceipt(@Valid @RequestBody ParseOcrRequest request) {
        return ResponseEntity.ok(ocrAiService.parseReceiptText(request.getRawText()));
    }
}
