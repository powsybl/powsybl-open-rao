/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TemporalDataImpl<T> implements TemporalData<T> {
    private final Map<OffsetDateTime, T> dataPerTimestamp;

    public TemporalDataImpl() {
        this(new HashMap<>());
    }

    public TemporalDataImpl(Map<OffsetDateTime, T> dataPerTimestamp) {
        this.dataPerTimestamp = new HashMap<>(dataPerTimestamp);
    }

    public Map<OffsetDateTime, T> getDataPerTimestamp() {
        return new HashMap<>(dataPerTimestamp);
    }

    public void add(OffsetDateTime timestamp, T data) {
        dataPerTimestamp.put(timestamp, data);
    }

    public <U> TemporalData<U> map(Function<T, U> function) {
        return new TemporalDataImpl<>(dataPerTimestamp.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> function.apply(entry.getValue()))));
    }
}
