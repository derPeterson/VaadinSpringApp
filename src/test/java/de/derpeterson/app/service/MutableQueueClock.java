package de.derpeterson.app.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/** Controlled time, also safe for the existing parallel worker fixtures. */
class MutableQueueClock extends Clock {
    private final AtomicReference<Instant> time = new AtomicReference<>(Instant.now());
    private final ZoneId zone;
    MutableQueueClock() { this(ZoneId.systemDefault()); }
    MutableQueueClock(ZoneId zone) { this.zone = zone; }
    void set(Instant instant) { time.set(instant); }
    void advance(Duration duration) { time.updateAndGet(value -> value.plus(duration)); }
    @Override public ZoneId getZone() { return zone; }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
    @Override public Instant instant() { return time.get(); }
}
