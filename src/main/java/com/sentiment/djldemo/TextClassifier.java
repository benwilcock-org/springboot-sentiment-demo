package com.sentiment.djldemo;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ai.djl.MalformedModelException;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;

/**
 * Implementation of TextClassificationService that delegates to ZeroShotClassificationService.
 *
 * <p>This class provides a clean separation between the general text classification
 * interface and the underlying zero-shot classification implementation. It simply
 * delegates all classification requests to the ZeroShotClassificationService.
 *
 * <p>Design rationale:
 * <ul>
 *   <li>Separation of concerns between sentiment analysis and text classification</li>
 *   <li>Allows different implementations in the future if needed</li>
 *   <li>Clean dependency injection for controllers</li>
 * </ul>
 */
@Service
public class TextClassifier implements TextClassificationService {

    private final ZeroShotClassificationService zeroShotService;

    /**
     * Constructor with dependency injection.
     *
     * @param zeroShotService the zero-shot classification service to delegate to
     */
    public TextClassifier(ZeroShotClassificationService zeroShotService) {
        this.zeroShotService = zeroShotService;
    }

    /**
     * Classifies text by delegating to ZeroShotClassificationService.
     *
     * @param text the text to classify
     * @param categories the possible categories
     * @return Classifications with probabilities, or Optional.empty() if validation fails
     */
    @Override
    public Optional<Classifications> predict(Optional<String> text, Optional<List<String>> categories)
            throws MalformedModelException, ModelNotFoundException, IOException, TranslateException {
        return zeroShotService.predict(text, categories);
    }
}
