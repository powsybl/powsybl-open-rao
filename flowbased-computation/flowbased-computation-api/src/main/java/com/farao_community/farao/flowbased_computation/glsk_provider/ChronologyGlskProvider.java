/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.glsk_provider;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class ChronologyGlskProvider implements GlskProvider {
    private Map<String, DataChronology<LinearGlsk>> chronologyGlskMap;
    private Instant selectedInstant;

    public ChronologyGlskProvider(Map<String, DataChronology<LinearGlsk>> chronologyGlskMap, Instant selectedInstant) {
        this.chronologyGlskMap = Objects.requireNonNull(chronologyGlskMap);
        this.selectedInstant = Objects.requireNonNull(selectedInstant);
    }

    public ChronologyGlskProvider(Map<String, DataChronology<LinearGlsk>> chronologyGlskMap) {
        this(chronologyGlskMap, Instant.now());
    }

    public ChronologyGlskProvider selectInstant(Instant newInstant) {
        this.selectedInstant = Objects.requireNonNull(newInstant);
        return this;
    }

    @Override
    public Map<String, LinearGlsk> getAllGlsk(Network network) {
        Objects.requireNonNull(network);
        return chronologyGlskMap.entrySet().stream()
                .filter(entry -> entry.getValue().getDataForInstant(selectedInstant).isPresent())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().getDataForInstant(selectedInstant).<AssertionError>orElseThrow(() -> new AssertionError("Data should be available at that instant"))
                ));
    }

    @Override
    public LinearGlsk getGlsk(Network network, String area) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(area);
        if (!chronologyGlskMap.containsKey(area)) {
            return null;
        }
        DataChronology<LinearGlsk> chronologyGlsk = chronologyGlskMap.get(area);
        return chronologyGlsk.getDataForInstant(selectedInstant).orElse(null);
    }
}
