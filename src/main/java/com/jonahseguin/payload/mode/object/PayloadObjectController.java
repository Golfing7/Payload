/*
 * Copyright (c) 2020 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.base.type.PayloadController;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Optional;

@Getter
@Setter
public class PayloadObjectController<X extends PayloadObject> implements PayloadController<X> {

    private final PayloadObjectCache<X> cache;
    private final String identifier;

    private WeakReference<X> payload = null;
    private boolean loadedFromLocal = false;

    PayloadObjectController(@Nonnull PayloadObjectCache<X> cache, String identifier) {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(identifier);
        this.cache = cache;
        this.identifier = identifier;
    }

    private void load(boolean fromLocal) {
        if (fromLocal) {
            Optional<X> local = cache.getFromCache(identifier);
            if (local.isPresent()) {
                payload = new WeakReference<>(local.get());
                loadedFromLocal = true;
                return;
            }
        }
        Optional<X> db = cache.getFromDatabase(identifier);
        db.ifPresent(x -> payload = new WeakReference<>(x));
    }

    @Override
    public Optional<X> get() {
        load(true);

        if (payload != null) {
            X p = payload.get();
            return Optional.ofNullable(p);
        }
        return Optional.empty();
    }

    @Override
    public Optional<X> cache() {
        load(true);

        if (payload != null) {
            X p = payload.get();
            if (p != null && !loadedFromLocal) {
                this.cache.cache(p);
                this.cache.getErrorService().debug("Cached payload " + p.getIdentifier());
            }
            return Optional.ofNullable(p);
        }
        return Optional.empty();
    }

    @Override
    public void forget() {
        this.payload = null;
    }

    @Override
    public void uncache(@Nonnull X payload, boolean switchingServers) {
        if (cache.isCached(payload.getIdentifier())) {
            cache.uncache(payload);
        }
    }
}
