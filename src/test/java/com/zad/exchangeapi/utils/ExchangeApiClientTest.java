package com.zad.exchangeapi.utils;


import com.zad.exchangeapi.config.ExchangeApiClient;
import com.zad.exchangeapi.config.ExternalApiConfig;
import com.zad.exchangeapi.dto.response.ExchangeApiResponse;
import com.zad.exchangeapi.exception.ExchangeApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class ExchangeApiClientTest {

    @Mock
    private ExternalApiConfig externalApiConfig;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExchangeApiClient exchangeApiClient;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Inject mocked RestTemplate via reflection since it's final in your class
        Field restTemplateField = ExchangeApiClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        restTemplateField.set(exchangeApiClient, restTemplate);

        when(externalApiConfig.getUrl()).thenReturn("https://mock-api.com");
        when(externalApiConfig.getApiKey()).thenReturn("test-api-key");
    }

    @Test
    void getExchangeRates_shouldReturnRates_whenApiResponseIsValid() {
        // Arrange
        ExchangeApiResponse mockResponse = new ExchangeApiResponse();
        mockResponse.setResult("success");

        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", BigDecimal.valueOf(1.0));
        rates.put("EUR", BigDecimal.valueOf(0.9));
        mockResponse.setConversionRates(rates);

        String expectedUrl = "https://mock-api.com/test-api-key/latest/USD";
        when(restTemplate.getForObject(expectedUrl, ExchangeApiResponse.class)).thenReturn(mockResponse);

        // Act
        Map<String, BigDecimal> result = exchangeApiClient.getExchangeRates("USD");

        // Assert
        assertThat(result).isEqualTo(rates);
    }

    @Test
    void getExchangeRates_shouldThrowException_onHttpClientError() {
        String expectedUrl = "https://mock-api.com/test-api-key/latest/USD";
        when(restTemplate.getForObject(expectedUrl, ExchangeApiResponse.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> exchangeApiClient.getExchangeRates("USD"))
                .isInstanceOf(ExchangeApiException.class)
                .hasMessageContaining("ExchangeRate API HTTP error");
    }

    @Test
    void getExchangeRates_shouldThrowException_onUnexpectedError() {
        String expectedUrl = "https://mock-api.com/test-api-key/latest/USD";
        when(restTemplate.getForObject(expectedUrl, ExchangeApiResponse.class))
                .thenThrow(new RuntimeException("Unexpected"));

        assertThatThrownBy(() -> exchangeApiClient.getExchangeRates("USD"))
                .isInstanceOf(ExchangeApiException.class)
                .hasMessageContaining("Unexpected error fetching exchange rates");
    }
}
