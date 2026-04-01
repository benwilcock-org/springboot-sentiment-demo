package com.sentiment.djldemo;

/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

import ai.djl.MalformedModelException;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Sentiment analyzer using unified zero-shot classification.
 *
 * <p>Delegates to {@link ZeroShotClassificationService} with fixed sentiment
 * categories (Positive, Negative) for memory-efficient classification.
 *
 * <p>
 * See this <a
 * href=
 * "https://github.com/deepjavalibrary/djl/blob/master/examples/docs/sentiment_analysis.md">doc</a>
 * for information about this example.*
 */
@Service
public final class SentimentAnalyzer implements SentimentService{

    private static final Logger logger = LoggerFactory.getLogger(SentimentAnalyzer.class);
    private final ZeroShotClassificationService zeroShotService;

    public SentimentAnalyzer(ZeroShotClassificationService zeroShotService) {
        this.zeroShotService = zeroShotService;
    }

    public Optional<Classifications> predict(Optional<String> input)
            throws MalformedModelException, ModelNotFoundException, IOException,
            TranslateException {
        if (!input.isPresent()) {
            logger.warn("You must provide a sentence for analysis. You provided {}.", input);
            return Optional.empty();
        }
        logger.info("Performing a sentiment analysis on this sentence: '{}'", input.get());

        // TODO: Add Micrometer metrics here to track prediction latency and throughput
        // Delegate to unified zero-shot service with sentiment categories
        return zeroShotService.predict(input, Optional.of(List.of("Positive", "Negative")));
    }
}
