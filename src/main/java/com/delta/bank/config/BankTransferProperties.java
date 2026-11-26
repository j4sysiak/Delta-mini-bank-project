package com.delta.bank.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

// Klasa spełnia konwencję JavaBean, więc Spring może wiązać właściwości przez settery.
// Wartości domyślne są używane, jeśli dana właściwość nie zostanie podana w konfiguracji.

@Validated
@ConfigurationProperties(prefix = "bank.transfer")
public class BankTransferProperties {

    @NotNull
    @Positive
    private BigDecimal maxAmount = new BigDecimal("5000.00");

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    private String defaultCurrency = "PLN";

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }
}