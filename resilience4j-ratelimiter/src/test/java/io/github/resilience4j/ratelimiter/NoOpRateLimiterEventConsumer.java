package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;

class NoOpRateLimiterEventConsumer implements
    RegistryEventConsumer<RateLimiter> {

    @Override
    public void onEntryAddedEvent(EntryAddedEvent<RateLimiter> entryAddedEvent) {
    }

    @Override
    public void onEntryRemovedEvent(EntryRemovedEvent<RateLimiter> entryRemoveEvent) {
    }

    @Override
    public void onEntryReplacedEvent(EntryReplacedEvent<RateLimiter> entryReplacedEvent) {
    }
}