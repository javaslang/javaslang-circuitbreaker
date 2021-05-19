package io.github.resilience4j.feign.test;

import feign.RequestLine;

import java.util.concurrent.CompletableFuture;

public interface CompletionStageTestService {

    @RequestLine("GET /greeting")
    CompletableFuture<String> greeting();
}
