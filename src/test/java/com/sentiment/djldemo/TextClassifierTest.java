package com.sentiment.djldemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.djl.modality.Classifications;

/**
 * TDD tests for TextClassifier - delegation wrapper for text classification.
 *
 * This service delegates to ZeroShotClassificationService for actual classification work.
 * It exists as a separate interface for clarity and separation of concerns between
 * sentiment analysis and general text classification use cases.
 *
 * Following TDD: These tests are written FIRST, before implementation exists.
 * Expected initial state: RED (compilation errors, no implementation)
 */
@ExtendWith(MockitoExtension.class)
class TextClassifierTest {

    @Mock
    private ZeroShotClassificationService zeroShotService;

    private TextClassifier textClassifier;

    @BeforeEach
    void setUp() {
        textClassifier = new TextClassifier(zeroShotService);
    }

    /**
     * TDD Test 1: Delegation to ZeroShotService
     *
     * GIVEN: Valid text and categories
     * WHEN: predict() is called on TextClassifier
     * THEN: Delegates to ZeroShotClassificationService and returns its result
     */
    @Test
    void shouldDelegateToZeroShotService() throws Exception {
        // Arrange
        Optional<String> text = Optional.of("Test classification text");
        Optional<List<String>> categories = Optional.of(List.of("category1", "category2"));

        @SuppressWarnings("unchecked")
        Classifications mockClassifications = org.mockito.Mockito.mock(Classifications.class);
        Optional<Classifications> expectedResult = Optional.of(mockClassifications);

        when(zeroShotService.predict(text, categories)).thenReturn(expectedResult);

        // Act
        Optional<Classifications> result = textClassifier.predict(text, categories);

        // Assert
        assertThat(result).isEqualTo(expectedResult);
        verify(zeroShotService).predict(text, categories);
    }

    /**
     * TDD Test 2: Input validation - empty text
     *
     * GIVEN: Empty text Optional
     * WHEN: predict() is called
     * THEN: Delegates to ZeroShotService which returns empty
     */
    @Test
    void shouldValidateEmptyText() throws Exception {
        // Arrange
        Optional<String> emptyText = Optional.empty();
        Optional<List<String>> categories = Optional.of(List.of("category1"));

        when(zeroShotService.predict(emptyText, categories)).thenReturn(Optional.empty());

        // Act
        Optional<Classifications> result = textClassifier.predict(emptyText, categories);

        // Assert
        assertThat(result).isEmpty();
        verify(zeroShotService).predict(emptyText, categories);
    }

    /**
     * TDD Test 3: Input validation - empty categories
     *
     * GIVEN: Empty categories Optional
     * WHEN: predict() is called
     * THEN: Delegates to ZeroShotService which returns empty
     */
    @Test
    void shouldValidateEmptyCategories() throws Exception {
        // Arrange
        Optional<String> text = Optional.of("Test text");
        Optional<List<String>> emptyCategories = Optional.empty();

        when(zeroShotService.predict(text, emptyCategories)).thenReturn(Optional.empty());

        // Act
        Optional<Classifications> result = textClassifier.predict(text, emptyCategories);

        // Assert
        assertThat(result).isEmpty();
        verify(zeroShotService).predict(text, emptyCategories);
    }

    /**
     * TDD Test 4: Exception propagation
     *
     * GIVEN: ZeroShotService throws exception
     * WHEN: predict() is called
     * THEN: Exception propagates to caller (not caught)
     */
    @Test
    void shouldPropagateExceptions() throws Exception {
        // Arrange
        Optional<String> text = Optional.of("Test text");
        Optional<List<String>> categories = Optional.of(List.of("category1"));

        when(zeroShotService.predict(any(), any()))
            .thenThrow(new ai.djl.translate.TranslateException("Model error"));

        // Act & Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            textClassifier.predict(text, categories)
        ).isInstanceOf(ai.djl.translate.TranslateException.class)
         .hasMessageContaining("Model error");
    }
}
