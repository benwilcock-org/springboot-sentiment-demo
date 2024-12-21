## About DJL.ai

Deep Java Library (DJL) is an open-source, high-level, engine-agnostic Java framework for deep learning. DJL is designed to be easy to get started with and simple to use for Java developers. DJL provides a native Java development experience and functions like any other regular Java library.

## Project Description

This project is a Spring Boot application that demonstrates how to build a sentiment analysis microservice using DJL. The service takes any sentence and provides a sentiment analysis for that sentence. The service communicates via a REST API.

## Libraries Used

- **Spring Boot**: A framework for building production-ready applications quickly.
- **DJL (Deep Java Library)**: A high-level, engine-agnostic Java framework for deep learning.
- **Springdoc OpenAPI**: A library to automate the generation of API documentation using OpenAPI 3.
- **Micrometer Tracing**: A library for application metrics and tracing.

## How to Run the Project

To run the project, use the following Maven command:

```sh
./mvnw spring-boot:run
```

This command will start the Spring Boot application, and the API will be available at http://localhost:8080.

## How to Test the Project
You can test the API using the provided HTTP request examples in the tests.http file. Here is an example of how to test the sentiment analysis endpoint:

```text
POST http://localhost:8080/api/analyze 
content-Type: application/json

{
  "sentence": "I like DJL. DJL is the best Deep Learming library I know!"
}
```

You can also use the following command to run the tests:

```
./mvnw test
```

This will execute the unit tests and integration tests defined in the project.

## API Documentation

The API documentation is automatically generated using Springdoc OpenAPI and is available at http://localhost:8080/v3/api-docs.

For more information, see the README.md file. 