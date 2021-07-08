package io.github.resilience4j.grpc.circuitbreaker.server;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;

public class ServerCircuitBreakerCallListener<ReqT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

    private final CircuitBreaker circuitBreaker;

    public ServerCircuitBreakerCallListener(ServerCall.Listener<ReqT> delegate, CircuitBreaker circuitBreaker) {
        super(delegate);
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public void onCancel() {
        circuitBreaker.releasePermission();
        super.onCancel();
    }
}
