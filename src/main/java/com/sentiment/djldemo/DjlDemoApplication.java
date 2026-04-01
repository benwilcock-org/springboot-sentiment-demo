package com.sentiment.djldemo;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import ai.djl.ModelException;
import ai.djl.modality.Classifications;
import ai.djl.translate.TranslateException;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIDefinition(
	tags = {
		@Tag(name = "sentiment-analysis",
             description = "API operations used for obtaining a sentiment analysis."),
		@Tag(name = "text-classification",
             description = "API operations for zero-shot text classification.")
	},
    info = @Info(title = "NLP Analysis API", version = "0.1-SNAPSHOT",
                 description = "REST API for NLP operations including sentiment analysis and zero-shot text classification using DJL. Both capabilities are powered by a unified DistilBERT model."),
    servers = {
       @Server(url = "https://localhost:8080", description = "NLP analysis service running locally.")
    }
) 


@SpringBootApplication
public class DjlDemoApplication {

	@Autowired
	SentimentService sentiments;

	private static final Logger logger = LoggerFactory.getLogger(DjlDemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DjlDemoApplication.class, args);
	}

	// TODO: Add Actuator health indicator for DJL model availability
	// TODO: Expose model version and metadata via /actuator/info endpoint

	@EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws IOException, TranslateException, ModelException {
        // TODO: Move warm-up sentence to application.yml so it's configurable per environment
		Classifications classifications = sentiments.predict(Optional.of("I like DJL. DJL is the best DL framework!")).get();
        logger.info(classifications.toString());
    }

}
