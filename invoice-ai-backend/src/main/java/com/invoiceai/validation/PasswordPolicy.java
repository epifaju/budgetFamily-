package com.invoiceai.validation;

import com.invoiceai.exception.ValidationException;
import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final String PATTERN =
        "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]).{8,}$";

    public static final String MESSAGE =
        "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial";

    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || !COMPILED.matcher(password).matches()) {
            throw new ValidationException(MESSAGE);
        }
    }
}
