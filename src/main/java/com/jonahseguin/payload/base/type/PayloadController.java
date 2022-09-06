package com.jonahseguin.payload.base.type;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface PayloadController<X extends Payload> {

    Optional<X> cache();

    /**
     * Gets the payload. Caching may or may not happen.
     * @return the payload.
     */
    Optional<X> get();

    void uncache(@Nonnull X payload, boolean switchingServers);

    /**
     * Makes this controller "forget" its payload. This will ensure that dead payloads don't remain.
     */
    void forget();
}
