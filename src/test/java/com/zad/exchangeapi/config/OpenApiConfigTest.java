package com.zad.exchangeapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
    }

    @Test
    void balanceApiOpenAPI_shouldBeConfiguredCorrectly() {
        // Act
        OpenAPI openAPI = openApiConfig.balanceApiOpenAPI();

        // Assert
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();

        Info info = openAPI.getInfo();
        assertThat(info.getTitle()).isEqualTo("Balance API");
        assertThat(info.getDescription()).contains("deposit, withdrawal, and currency exchange");
        assertThat(info.getVersion()).isEqualTo("v1.0");

        Contact contact = info.getContact();
        assertThat(contact).isNotNull();
        assertThat(contact.getName()).isEqualTo("API Support");
        assertThat(contact.getEmail()).isEqualTo("support@example.com");

        License license = info.getLicense();
        assertThat(license).isNotNull();
        assertThat(license.getName()).isEqualTo("Apache 2.0");
        assertThat(license.getUrl()).isEqualTo("http://springdoc.org");
    }
}
