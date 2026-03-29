package com.sentiment.djldemo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * TDD tests for TextClassificationApiController - REST endpoint tests.
 *
 * Uses @SpringBootTest for integration testing.
 * Tests the complete request/response cycle with real services.
 *
 * Endpoint: POST /api/classify
 * Request: {"text": "...", "categories": ["cat1", "cat2"]}
 * Response: {"text": "...", "classifications": [{"category": "cat1", "confidence": "85.12345%"}]}
 *
 * Following TDD: These tests are written FIRST, before implementation exists.
 * Expected initial state: RED (compilation errors, no controller)
 */
@SpringBootTest
class TextClassificationApiControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * TDD Test 1: Validation - null text
     *
     * GIVEN: Request with null text
     * WHEN: POST to /api/classify
     * THEN: Returns 500 with error (controller throws IllegalArgumentException)
     *
     * Note: Following SentimentApiController pattern which throws IllegalArgumentException
     * for invalid inputs, handled by SentimentApiControllerAdvice returning 500.
     */
    @Test
    void shouldReturn500WhenTextIsNull() throws Exception {
        // Arrange
        String requestJson = """
            {
                "text": null,
                "categories": ["technology", "food"]
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isInternalServerError());
    }

    /**
     * TDD Test 2: Validation - empty categories
     *
     * GIVEN: Request with empty categories array
     * WHEN: POST to /api/classify
     * THEN: Returns 500 with error
     */
    @Test
    void shouldReturn500WhenCategoriesAreEmpty() throws Exception {
        // Arrange
        String requestJson = """
            {
                "text": "Some text to classify",
                "categories": []
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isInternalServerError());
    }

    /**
     * TDD Test 3: Happy path - valid request
     *
     * GIVEN: Valid text and categories
     * WHEN: POST to /api/classify
     * THEN: Returns 200 OK with classification results
     */
    @Test
    void shouldReturn200WithValidRequest() throws Exception {
        String requestJson = """
            {
                "text": "I love eating pizza",
                "categories": ["food", "technology", "sports"]
            }
            """;

        mockMvc.perform(post("/api/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("I love eating pizza"))
                .andExpect(jsonPath("$.classifications").isArray())
                .andExpect(jsonPath("$.classifications.length()").value(3))
                .andExpect(jsonPath("$.classifications[0].category").exists())
                .andExpect(jsonPath("$.classifications[0].confidence").exists());
    }

    /**
     * TDD Test 4: JSON structure validation
     *
     * GIVEN: Valid request
     * WHEN: POST to /api/classify
     * THEN: Response has correct JSON structure matching DTOs
     */
    @Test
    void shouldReturnCorrectJsonStructure() throws Exception {
        String requestJson = """
            {
                "text": "Test text",
                "categories": ["cat1", "cat2"]
            }
            """;

        mockMvc.perform(post("/api/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").isString())
                .andExpect(jsonPath("$.classifications").isArray())
                .andExpect(jsonPath("$.classifications[0].category").isString())
                .andExpect(jsonPath("$.classifications[0].confidence").isString())
                .andExpect(jsonPath("$.classifications[1].category").isString())
                .andExpect(jsonPath("$.classifications[1].confidence").isString());
    }


    @Test
    void shouldReturn500WhenTextIsEmptyString() throws Exception {
        String requestJson = """
            {
                "text": "",
                "categories": ["cat1"]
            }
            """;

        mockMvc.perform(post("/api/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldReturn500WhenTextIsWhitespace() throws Exception {
        String requestJson = """
            {
                "text": "   ",
                "categories": ["cat1"]
            }
            """;

        mockMvc.perform(post("/api/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isInternalServerError());
    }
}
