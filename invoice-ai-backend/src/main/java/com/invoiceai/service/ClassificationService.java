package com.invoiceai.service;

import com.invoiceai.domain.enums.CategoryType;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassificationService {

    private static final Map<CategoryType, List<String>> KEYWORDS = new EnumMap<>(CategoryType.class);

    static {
        KEYWORDS.put(CategoryType.ALIMENTAIRE, List.of("pain", "lait", "fruits", "legume", "fromage", "boisson"));
        KEYWORDS.put(CategoryType.SANTE, List.of("pharma", "medicament", "aspirine", "doliprane"));
        KEYWORDS.put(CategoryType.TRANSPORT, List.of("essence", "carburant", "uber", "metro", "bus"));
        KEYWORDS.put(CategoryType.VETEMENTS, List.of("chemise", "pantalon", "veste", "chaussure"));
        KEYWORDS.put(CategoryType.AUTRES, List.of());
    }

    public CategoryType classify(String label) {
        if (label == null) {
            return CategoryType.AUTRES;
        }
        String normalized = label.toLowerCase(Locale.FRENCH);
        return KEYWORDS.entrySet().stream()
            .filter(entry -> entry.getValue().stream().anyMatch(normalized::contains))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(CategoryType.AUTRES);
    }
}




