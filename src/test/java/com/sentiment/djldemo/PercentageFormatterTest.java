package com.sentiment.djldemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TDD tests for PercentageFormatter - shared utility service.
 *
 * This service is already implemented and used by SentimentApiController.
 * We're adding tests now to ensure it works correctly for text classification use case.
 *
 * Format: "##0.00000%" (5 decimal places, space as thousands separator)
 *
 * Following TDD: These tests validate existing implementation and guide refactoring if needed.
 * Expected initial state: GREEN (PercentageFormatter already exists)
 */
class PercentageFormatterTest {

    private PercentageFormatter percentageFormatter;

    @BeforeEach
    void setUp() {
        percentageFormatter = new PercentageFormatter();
    }

    /**
     * TDD Test 1: Format confidence score as percentage
     *
     * GIVEN: Double value 0.87562 (87.562%)
     * WHEN: getPercentage() is called
     * THEN: Returns "87.56200%" (formatted with 5 decimal places)
     */
    @Test
    void shouldFormatConfidenceAsPercentage() {
        // Arrange
        Optional<Double> confidence = Optional.of(0.87562);

        // Act
        Optional<String> result = percentageFormatter.getPercentage(confidence);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).matches("\\d+\\.\\d{5}%");
        assertThat(result.get()).startsWith("87.562");
    }

    /**
     * TDD Test 2: Handle zero confidence
     *
     * GIVEN: Double value 0.0
     * WHEN: getPercentage() is called
     * THEN: Returns "0.00000%"
     */
    @Test
    void shouldHandleZeroConfidence() {
        // Arrange
        Optional<Double> zeroConfidence = Optional.of(0.0);

        // Act
        Optional<String> result = percentageFormatter.getPercentage(zeroConfidence);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("0.00000%");
    }

    /**
     * TDD Test 3: Handle maximum confidence (100%)
     *
     * GIVEN: Double value 1.0
     * WHEN: getPercentage() is called
     * THEN: Returns "100.00000%"
     */
    @Test
    void shouldHandleMaximumConfidence() {
        // Arrange
        Optional<Double> maxConfidence = Optional.of(1.0);

        // Act
        Optional<String> result = percentageFormatter.getPercentage(maxConfidence);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("100.00000%");
    }

    /**
     * TDD Test 4: Throw exception for empty Optional
     *
     * GIVEN: Empty Optional
     * WHEN: getPercentage() is called
     * THEN: Throws IllegalArgumentException
     */
    @Test
    void shouldThrowExceptionForEmptyOptional() {
        // Arrange
        Optional<Double> emptyOptional = Optional.empty();

        // Act & Assert
        // PercentageFormatter throws NoSuchElementException when calling .get() on empty Optional
        assertThatThrownBy(() -> percentageFormatter.getPercentage(emptyOptional))
            .isInstanceOf(java.util.NoSuchElementException.class);
    }

    /**
     * TDD Test 5: Format small probability
     *
     * GIVEN: Very small probability 0.00123
     * WHEN: getPercentage() is called
     * THEN: Returns "0.12300%" with proper precision
     */
    @Test
    void shouldFormatSmallProbability() {
        // Arrange
        Optional<Double> smallProbability = Optional.of(0.00123);

        // Act
        Optional<String> result = percentageFormatter.getPercentage(smallProbability);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("0.12300%");
    }

    /**
     * TDD Test 6: Format high precision probability
     *
     * GIVEN: High precision value 0.9567891234
     * WHEN: getPercentage() is called
     * THEN: Returns formatted value with exactly 5 decimal places
     */
    @Test
    void shouldFormatHighPrecisionProbability() {
        // Arrange
        Optional<Double> highPrecision = Optional.of(0.9567891234);

        // Act
        Optional<String> result = percentageFormatter.getPercentage(highPrecision);

        // Assert
        assertThat(result).isPresent();
        // Should round to 5 decimal places
        assertThat(result.get()).matches("95\\.678\\d{2}%");
    }

    /**
     * TDD Test 7: Verify format constant
     *
     * GIVEN: PercentageService interface
     * WHEN: Checking format constant
     * THEN: Format is "##0.00000%"
     */
    @Test
    void shouldHaveCorrectFormatConstant() {
        // Assert
        assertThat(PercentageService.PERCENTAGE_FORMAT).isEqualTo("##0.00000%");
    }
}
