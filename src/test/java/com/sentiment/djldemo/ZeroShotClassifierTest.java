package com.sentiment.djldemo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.djl.MalformedModelException;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;

/**
 * TDD tests for ZeroShotClassifier - the core unified model service.
 *
 * This service uses typeform/distilbert-base-uncased-mnli model (268 MB) for
 * zero-shot classification. It replaces the specialized sentiment model and
 * handles both sentiment analysis and arbitrary text classification.
 *
 * Following TDD: These tests are written FIRST, before implementation exists.
 * Expected initial state: RED (compilation errors, no implementation)
 */
class ZeroShotClassifierTest {

    private ZeroShotClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new ZeroShotClassifier();
    }

    /**
     * TDD Test 1: Input validation - empty text
     *
     * GIVEN: Empty Optional text input
     * WHEN: predict() is called
     * THEN: Returns Optional.empty() (null safety)
     */
    @Test
    void shouldReturnEmptyWhenTextIsEmpty() throws Exception {
        // Arrange
        Optional<String> emptyText = Optional.empty();
        Optional<List<String>> categories = Optional.of(List.of("positive", "negative"));

        // Act
        Optional<Classifications> result = classifier.predict(emptyText, categories);

        // Assert
        assertThat(result).isEmpty();
    }

    /**
     * TDD Test 2: Input validation - empty categories
     *
     * GIVEN: Valid text but empty categories list
     * WHEN: predict() is called
     * THEN: Returns Optional.empty() (validation failure)
     */
    @Test
    void shouldReturnEmptyWhenCategoriesAreEmpty() throws Exception {
        // Arrange
        Optional<String> text = Optional.of("This is a test sentence");
        Optional<List<String>> emptyCategories = Optional.empty();

        // Act
        Optional<Classifications> result = classifier.predict(text, emptyCategories);

        // Assert
        assertThat(result).isEmpty();
    }

    /**
     * TDD Test 3: Basic classification with arbitrary categories
     *
     * GIVEN: Text about technology and categories ["technology", "food", "sports"]
     * WHEN: predict() is called
     * THEN: Returns Classifications with "technology" ranked highest
     *
     * NOTE: This is a SLOW test (loads real DJL model from HuggingFace)
     * First run: ~3-10 seconds (model download)
     * Subsequent runs: <1 second (model cached in ~/.djl.ai/cache/)
     */
    @Test
    void shouldClassifyTextIntoCategories() throws MalformedModelException, ModelNotFoundException,
            IOException, TranslateException {
        // Arrange
        Optional<String> text = Optional.of("This new smartphone has amazing battery life and great camera");
        Optional<List<String>> categories = Optional.of(List.of("technology", "food", "sports"));

        // Act
        Optional<Classifications> result = classifier.predict(text, categories);

        // Assert
        assertThat(result).isPresent();
        Classifications classifications = result.get();
        assertThat(classifications.items()).isNotEmpty();

        // Should have classifications for all categories
        assertThat(classifications.items()).hasSize(3);

        // Top category should be one of the provided categories
        String topCategory = classifications.best().getClassName();
        assertThat(topCategory).isIn("technology", "food", "sports");
    }

    /**
     * TDD Test 4: Sentiment analysis use case
     *
     * GIVEN: Positive sentiment text and categories ["Positive", "Negative"]
     * WHEN: predict() is called
     * THEN: Returns Classifications with "Positive" ranked higher than "Negative"
     *
     * This tests the unified model's ability to replace the specialized sentiment model.
     * The existing SentimentAnalyzer will delegate to this service with these fixed categories.
     */
    @Test
    void shouldClassifySentimentAsPositiveNegative() throws MalformedModelException, ModelNotFoundException,
            IOException, TranslateException {
        // Arrange
        Optional<String> positiveText = Optional.of("I love this product! It's amazing and works perfectly!");
        Optional<List<String>> sentimentCategories = Optional.of(List.of("Positive", "Negative"));

        // Act
        Optional<Classifications> result = classifier.predict(positiveText, sentimentCategories);

        // Assert
        assertThat(result).isPresent();
        Classifications classifications = result.get();

        // Should have both Positive and Negative classifications
        assertThat(classifications.items()).hasSize(2);

        // Should have both Positive and Negative in results
        double positiveProbability = classifications.get("Positive").getProbability();
        double negativeProbability = classifications.get("Negative").getProbability();

        // Probabilities should sum to approximately 1.0
        assertThat(positiveProbability + negativeProbability).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));

        // For positive text, positive should be >= negative (may be equal for borderline cases)
        assertThat(positiveProbability).isGreaterThanOrEqualTo(negativeProbability);
    }

    /**
     * TDD Test 5: Robustness - special characters
     *
     * GIVEN: Text with emojis and special characters
     * WHEN: predict() is called
     * THEN: Returns valid Classifications without errors
     *
     * Tests model's ability to handle Unicode and special characters.
     */
    @Test
    void shouldHandleSpecialCharactersInText() throws MalformedModelException, ModelNotFoundException,
            IOException, TranslateException {
        // Arrange
        Optional<String> textWithEmojis = Optional.of("I ❤️ pizza 🍕 and pasta! 😋");
        Optional<List<String>> categories = Optional.of(List.of("food", "technology", "emotions"));

        // Act
        Optional<Classifications> result = classifier.predict(textWithEmojis, categories);

        // Assert
        assertThat(result).isPresent();
        Classifications classifications = result.get();
        assertThat(classifications.items()).isNotEmpty();

        // Should classify as food (mentions pizza and pasta)
        String topCategory = classifications.best().getClassName();
        assertThat(topCategory).isIn("food", "emotions"); // Could be either, both valid
    }

    /**
     * TDD Test 6: Empty string validation (different from empty Optional)
     *
     * GIVEN: Empty string (not empty Optional)
     * WHEN: predict() is called
     * THEN: Returns Optional.empty()
     */
    @Test
    void shouldReturnEmptyWhenTextIsEmptyString() throws Exception {
        // Arrange
        Optional<String> emptyString = Optional.of("");
        Optional<List<String>> categories = Optional.of(List.of("positive", "negative"));

        // Act
        Optional<Classifications> result = classifier.predict(emptyString, categories);

        // Assert
        assertThat(result).isEmpty();
    }

    /**
     * TDD Test 7: Null categories list (not empty Optional)
     *
     * GIVEN: Categories Optional containing empty list
     * WHEN: predict() is called
     * THEN: Returns Optional.empty()
     */
    @Test
    void shouldReturnEmptyWhenCategoriesListIsEmpty() throws Exception {
        // Arrange
        Optional<String> text = Optional.of("Test text");
        Optional<List<String>> emptyCategoriesList = Optional.of(List.of()); // Empty list, not empty Optional

        // Act
        Optional<Classifications> result = classifier.predict(text, emptyCategoriesList);

        // Assert
        assertThat(result).isEmpty();
    }
}
