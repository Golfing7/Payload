package com.jonahseguin.payload.base.store;

import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.mode.object.PayloadObject;
import dev.morphia.query.Query;
import org.bson.Document;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface PayloadRemoteStore<K, X extends Payload> extends PayloadStore<K, X> {
    /**
     * Queries all the payloads with the given filter.
     *
     * @param filter the document filter.
     * @return the queried objects.
     */
    @Nonnull
    Collection<X> queryPayloads(Query<X> filter);

    /**
     * Creates a query for data on this store.
     *
     * @return the query.
     */
    Query<X> createQuery();
}
