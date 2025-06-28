package com.zad.exchangeapi.config;

import com.zad.exchangeapi.dto.response.ExchangeApiResponse;
import com.zad.exchangeapi.exception.ExchangeApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeApiClient {

    private final ExternalApiConfig externalApiConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetches exchange rates for the given base currency using exchangerate-api.com
     * Example: https://v6.exchangerate-api.com/v6/{apiKey}/latest/{baseCurrency}
     */
    public Map<String, BigDecimal> getExchangeRates(String baseCurrency) {
        String url = String.format("%s/%s/latest/%s",
                externalApiConfig.getUrl(),
                externalApiConfig.getApiKey(),
                baseCurrency);

        try {
            ExchangeApiResponse response = restTemplate.getForObject(url, ExchangeApiResponse.class);

            if (response == null || !"success".equalsIgnoreCase(response.getResult())) {
                log.error("Failed response from ExchangeRate API: {}", response);
                throw new ExchangeApiException("Failed to fetch exchange rates", HttpStatus.SERVICE_UNAVAILABLE);
            }

            if (response.getConversionRates() == null || response.getConversionRates().isEmpty()) {
                throw new ExchangeApiException("Exchange rates missing in response", HttpStatus.BAD_GATEWAY);
            }

            return response.getConversionRates();

        } catch (HttpClientErrorException ex) {
            log.error("HTTP error calling ExchangeRate API: {}", ex.getMessage());
            throw new ExchangeApiException("ExchangeRate API HTTP error", HttpStatus.BAD_GATEWAY);
        } catch (Exception ex) {
            log.error("Unexpected error calling ExchangeRate API: {}", ex.getMessage(), ex);
            throw new ExchangeApiException("Unexpected error fetching exchange rates", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
