package com.deb.generator;

import com.deb.util.Base62Math;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counter-based Base62 key generator.
 * Uses atomic sequence range allocations in memory for collision-free, ultra-fast generation.
 */
@Component
@ConditionalOnProperty(name = "app.keygen.strategy", havingValue = "counter", matchIfMissing = true)
public class CounterBase62KeyGenerator implements KeyGenerator {

    // Base offset to ensure minimum length (e.g. 62^5 = 916,132,832 produces >= 6 chars in Base62)
    private static final long DEFAULT_STARTING_OFFSET = 916_132_832L;
    private final AtomicLong counter;

    public CounterBase62KeyGenerator() {
        this(DEFAULT_STARTING_OFFSET);
    }

    public CounterBase62KeyGenerator(long initialValue) {
        this.counter = new AtomicLong(initialValue);
    }

    @Override
    public String generateKey() {
        long id = counter.getAndIncrement();
        return Base62Math.encode(id);
    }

    @Override
    public String getStrategyName() {
        return "COUNTER_BASE62";
    }

    public long getCurrentSequence() {
        return counter.get();
    }
}
