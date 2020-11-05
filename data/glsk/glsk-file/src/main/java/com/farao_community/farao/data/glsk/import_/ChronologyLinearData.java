/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.chronology.DataChronology;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ChronologyLinearData<I> {

    private final Map<String, DataChronology<I>> chronologyLinearDataMap;

    public ChronologyLinearData(Map<String, DataChronology<I>> chronologyLinearDataMap) {
        this.chronologyLinearDataMap = chronologyLinearDataMap;
    }

    public Map<String, I> getLinearData(Instant instant) {
        return chronologyLinearDataMap.entrySet().stream()
            .filter(entry -> entry.getValue().getDataForInstant(instant).isPresent())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getDataForInstant(instant).<AssertionError>orElseThrow(() -> new AssertionError("Data should be available at that instant"))
            ));
    }

    public I getLinearData(Instant instant, String area) {
        Objects.requireNonNull(area);
        if (!chronologyLinearDataMap.containsKey(area)) {
            return null;
        }
        DataChronology<I> chronologyGlsk = chronologyLinearDataMap.get(area);
        return chronologyGlsk.getDataForInstant(instant).orElse(null);
    }
}
