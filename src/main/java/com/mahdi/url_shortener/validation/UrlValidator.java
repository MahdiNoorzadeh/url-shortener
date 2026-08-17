package com.mahdi.url_shortener.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

public class UrlValidator implements ConstraintValidator<ValidUrl, String> {


         @Override
    public boolean isValid(
        String value,
        ConstraintValidatorContext context
    ) {

        if (value == null || value.isBlank()) {
            return true;
        }

        try {
            URI uri = URI.create(value);

            return (
                uri.getScheme() != null &&
                (
                    uri.getScheme().equalsIgnoreCase("http") ||
                    uri.getScheme().equalsIgnoreCase("https")
                ) &&
                uri.getHost() != null
            );

        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
