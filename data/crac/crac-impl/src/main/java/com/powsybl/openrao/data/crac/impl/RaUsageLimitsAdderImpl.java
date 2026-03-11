/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.RaUsageLimitsAdder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public class RaUsageLimitsAdderImpl implements RaUsageLimitsAdder {
    CracImpl owner;
    private final Instant instant;
    private final RaUsageLimits raUsageLimits = new RaUsageLimits();

    RaUsageLimitsAdderImpl(CracImpl owner, String instantName) {
        Objects.requireNonNull(owner);
        this.owner = owner;
        List<Instant> instants = this.owner.getSortedInstants().stream().filter(cracInstant -> cracInstant.getId().equals(instantName)).toList();
        if (instants.isEmpty()) {
            throw new OpenRaoException(String.format("The instant %s does not exist in the crac.", instantName));
        }
        this.instant = instants.get(0);
    }

    @Override
    public RaUsageLimitsAdder withMaxRa(int maxRa) {
        raUsageLimits.setMaxRa(maxRa);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxTso(int maxTso) {
        raUsageLimits.setMaxTso(maxTso);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxTopoPerTso(Map<String, Integer> maxTopoPerTso) {
        raUsageLimits.setMaxTopoPerTso(maxTopoPerTso);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxPstPerTso(Map<String, Integer> maxPstPerTso) {
        raUsageLimits.setMaxPstPerTso(maxPstPerTso);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxRaPerTso(Map<String, Integer> maxRaPerTso) {
        raUsageLimits.setMaxRaPerTso(maxRaPerTso);
        return this;
    }

    @Override
    public RaUsageLimitsAdder withMaxElementaryActionPerTso(Map<String, Integer> maxElementaryActionPerTso) {
        raUsageLimits.setMaxElementaryActionsPerTso(maxElementaryActionPerTso);
        return this;
    }

    @Override
    public RaUsageLimits add() {
        owner.addRaUsageLimits(instant, raUsageLimits);
        return raUsageLimits;
    }
}
