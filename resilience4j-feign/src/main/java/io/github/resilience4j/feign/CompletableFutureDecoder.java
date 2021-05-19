package io.github.resilience4j.feign;

import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CompletableFutureDecoder implements Decoder {

    private final Decoder delegate;

    public CompletableFutureDecoder(Decoder delegate) {
        Objects.requireNonNull(delegate, "Decoder must not be null.");
        this.delegate = delegate;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, DecodeException, FeignException {
        if (!accept(type)) {
            return delegate.decode(response, type);
        }
        if (response.status() == 404 || response.status() == 204) {
            return CompletableFuture.completedFuture(null);
        }
        Type enclosedType = Util.resolveLastTypeParameter(type, CompletableFuture.class);
        Object decoded = delegate.decode(response, enclosedType);
        return CompletableFuture.completedFuture(decoded);
    }

    static boolean accept(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        return parameterizedType.getRawType().equals(CompletableFuture.class);
    }

}
