package com.sentiment.djldemo;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import ai.djl.MalformedModelException;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;

/**
 * Service interface for zero-shot text classification using DJL.
 *
 * <p>Zero-shot classification allows categorizing text into arbitrary categories
 * without training on those specific categories. Uses MNLI (Multi-Genre Natural
 * Language Inference) model to determine which category best matches the text.
 *
 * <p>Example:
 * <pre>
 * Text: "This smartphone has great battery life"
 * Categories: ["technology", "food", "sports"]
 * Result: "technology" with highest probability
 * </pre>
 */
public interface ZeroShotClassificationService {

    /**
     * Predicts which categories the text belongs to.
     *
     * @param text the text to classify (must not be empty)
     * @param categories the possible categories to classify into (must not be empty)
     * @return Classifications with probabilities for each category, or Optional.empty() if validation fails
     * @throws MalformedModelException if the model format is invalid
     * @throws ModelNotFoundException if the model cannot be found
     * @throws IOException if there's an I/O error loading the model
     * @throws TranslateException if there's an error during inference
     */
    Optional<Classifications> predict(Optional<String> text, Optional<List<String>> categories)
            throws MalformedModelException, ModelNotFoundException, IOException, TranslateException;
}
