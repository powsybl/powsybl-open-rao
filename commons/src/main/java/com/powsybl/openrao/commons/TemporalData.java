/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface TemporalData<T> {
    Map<OffsetDateTime, T> getDataPerTimestamp();

    default Optional<T> getData(OffsetDateTime timestamp) {
        return Optional.ofNullable(getDataPerTimestamp().get(timestamp));
    }

    default List<OffsetDateTime> getTimestamps() {
        return getDataPerTimestamp().keySet().stream().sorted().toList();
    }

    void add(OffsetDateTime timestamp, T data);

    <U> TemporalData<U> map(Function<T, U> function);
}
