/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.commons;

import com.farao_community.farao.commons.chronology.Chronology;

import java.time.Instant;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface ZonalDataChronology<I> extends Chronology<ZonalData<I>> {

    default I getData(String zone, Instant instant) {
        return selectInstant(instant).getData(zone);
    }

    default I getData(String zone, Instant instant, ReplacementStrategy replacementStrategy) {
        return selectInstant(instant, replacementStrategy).getData(zone);
    }
}
