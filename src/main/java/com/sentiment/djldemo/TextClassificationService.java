package com.sentiment.djldemo;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import ai.djl.MalformedModelException;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;

/**
 * Service interface for general text classification.
 *
 * <p>This service provides text classification capability for arbitrary categories.
 * It delegates to ZeroShotClassificationService for the actual classification work.
 *
 * <p>Separation between TextClassificationService and ZeroShotClassificationService
 * provides clarity and allows different implementations if needed in the future.
 *
 * <p>Example:
 * <pre>
 * Text: "Breaking news: Major political event"
 * Categories: ["politics", "sports", "entertainment"]
 * Result: "politics" with highest confidence
 * </pre>
 */
public interface TextClassificationService {

    /**
     * Classifies text into provided categories.
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
