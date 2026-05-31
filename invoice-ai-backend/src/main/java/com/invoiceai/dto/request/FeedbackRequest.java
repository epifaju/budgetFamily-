package com.invoiceai.dto.request;

import com.invoiceai.domain.enums.CategoryType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
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
public class FeedbackRequest {

    @NotNull
    private UUID itemId;

    @NotNull
    private CategoryType correctedCategory;
}




