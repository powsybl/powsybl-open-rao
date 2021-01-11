/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.commons;

import java.util.Map;
import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface ZonalData<I> {

    Map<String, I> getDataPerZone();

    default I getData(String zone) {
        Objects.requireNonNull(zone, "Zone has to be specified to query zonal data.");
        return getDataPerZone().get(zone);
    }

    void addAll(ZonalData<I> otherData);
}
