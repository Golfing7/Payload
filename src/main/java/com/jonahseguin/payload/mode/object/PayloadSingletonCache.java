/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.base.type.PayloadInstantiator;
import com.jonahseguin.payload.mode.object.settings.ObjectCacheSettings;
import com.jonahseguin.payload.mode.object.store.ObjectStoreLocal;
import com.jonahseguin.payload.mode.object.store.ObjectStoreMongo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a cache of a singleton value. This cache can only store one value.
 * <p />
 * Backed from a PayloadObjectCache with the object being stored under the key "0".
 * @param <X>
 */
@Getter
@Singleton
public class PayloadSingletonCache<X> extends PayloadObjectCache<PayloadSingletonCache.Wrapper> implements ObjectCache<PayloadSingletonCache.Wrapper> {
    public PayloadSingletonCache(Injector injector, String name) {
        super(injector, name, Wrapper.class);

        //Set the instantiator.
        setInstantiator((inst) -> new Wrapper(this));
    }

    /**
     * Sets the value of this payload cache.
     * @param value the value to set to. <code>null</code> if the value should be removed.
     */
    public void set(X value){
        if(value == null){
            delete("0");
            return;
        }

        Wrapper wrapped = wrap(value);
        save(wrapped);
    }

    /**
     * Sets the value without caching the new value.
     * @param value the value to set to. <code>null</code> if the value should be removed.
     */
    public void setNoCache(X value){
        if(value == null){
            delete("0");
            return;
        }

        Wrapper wrapped = wrap(value);
        saveNoCache(wrapped);
    }

    /**
     * Gets the value of this payload cache.
     * @return the value stored in this database, empty if no value is stored.
     */
    @SuppressWarnings("unchecked")
    public Optional<X> get(){
        return (Optional<X>) get("0");
    }

    /**
     * Constructs a wrapper of the value provided.
     * @param value the value to wrap.
     * @return the wrapper.
     */
    protected Wrapper wrap(X value){
        return new Wrapper(this, value);
    }

    /**
     * Wraps a value in a simple PayloadObject instance to be stored in the object cache.
     */
    protected static class Wrapper extends PayloadObject{
        private final String identifier = "0";
        @Getter @Setter
        private Object value;

        public Wrapper(ObjectCache cache) {
            super(cache);
        }

        public Wrapper(ObjectCache cache, Object value) {
            super(cache);
            this.value = value;
        }

        @Override
        public String getIdentifier() {
            return "0";
        }

        @NotNull
        @Override
        public String identifierFieldName() {
            return "identifier";
        }
    }
}
