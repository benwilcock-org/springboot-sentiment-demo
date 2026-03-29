package com.sentiment.djldemo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sentiment.djldemo.SentimentApiControllerAdvice.ErrorDescription;

import ai.djl.MalformedModelException;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api")
public class TextClassificationApiController {

    private static final Logger LOG = LoggerFactory.getLogger(TextClassificationApiController.class);

    @Autowired
    TextClassificationService textClassificationService;

    @Autowired
    PercentageService percentageService;

    @CrossOrigin
    @Operation(
        summary = "Classify text into categories",
        description = "Performs zero-shot text classification using a unified MNLI model. Returns ranked categories with confidence scores.",
        tags = {"text-classification"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TextClassification.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorDescription.class)))
    })
    @PostMapping(value = "/classify", produces = {"application/json"})
    public ResponseEntity<TextClassification> classify(@RequestBody ClassificationRequest request)
            throws MalformedModelException, ModelNotFoundException, IOException, TranslateException {
        LOG.info("A text classification request has been received: text='{}', categories={}",
                request.text(), request.categories());

        // Input validation
        if (isNullOrEmptyOrContainsNoWords(request.text())) {
            LOG.error("The check that the text contained at least a word has failed: {}", request.text());
            throw new IllegalArgumentException("The text given appears to contain no words?");
        }

        if (request.categories() == null || request.categories().isEmpty()) {
            LOG.error("The check that categories were provided has failed: {}", request.categories());
            throw new IllegalArgumentException("The categories list appears to be null or empty?");
        }

        // Perform the text classification
        Optional<Classifications> classifications = textClassificationService.predict(
                Optional.of(request.text()),
                Optional.of(request.categories())
        );

        // Perform a null check and continue
        if (classifications.isPresent()) {

            LOG.info("Classification probabilities are: {}", classifications.get().items());

            // Build the list of CategoryScore objects
            List<CategoryScore> categoryScores = new ArrayList<>();

            for (Classifications.Classification item : classifications.get().items()) {
                String category = item.getClassName();
                Double probability = item.getProbability();

                // Format the probability as a percentage
                String confidence = percentageService
                        .getPercentage(Optional.of(probability))
                        .get();

                categoryScores.add(new CategoryScore(category, confidence));
            }

            // Create the TextClassification return object
            TextClassification textClassification = new TextClassification(
                    request.text(),
                    categoryScores
            );

            // pass it back
            return ResponseEntity.ok(textClassification);

        } else {
            LOG.error("The check that the optional classification values were present has failed: {}",
                    classifications);
            throw new RuntimeException(
                    "Unable to perform the text classification. There was an unexpected issue getting the probabilities and returning the analysis.");
        }
    }

    private boolean isNullOrEmptyOrContainsNoWords(String str) {
        return str == null || str.trim().isEmpty() || !str.matches(".*\\w.*");
    }

    @Schema(description = "A text classification request")
    public record ClassificationRequest(
        @JsonProperty("text")
        @Schema(description = "The text to classify", example = "I love eating pizza")
        String text,

        @JsonProperty("categories")
        @Schema(description = "List of candidate categories", example = "[\"food\", \"technology\", \"sports\"]")
        List<String> categories
    ) {}

    @Schema(description = "The result of a text classification")
    public record TextClassification(
        @Schema(description = "The original text that was classified")
        String text,

        @JsonProperty("classifications")
        @Schema(description = "Ranked categories with confidence scores")
        List<CategoryScore> classifications
    ) {}

    @Schema(description = "A category with its confidence score")
    public record CategoryScore(
        @Schema(description = "The category label")
        String category,

        @JsonProperty("confidence")
        @Schema(description = "Confidence score as percentage (format: ##0.00000%)")
        String confidence
    ) {}
}
