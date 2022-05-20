/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import com.farao_community.farao.data.crac_creation.creator.api.parameters.AbstractAlignedRaCracCreationParameters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CimCracCreationParameters extends AbstractAlignedRaCracCreationParameters {
    private Map<String, RangeActionSpeed> speedMap = new HashMap<>();

    @Override
    public String getName() {
        return "CimCracCreatorParameters";
    }

    public void setRemedialActionSpeed(Set<RangeActionSpeed> rangeActionSpeedList) {
        this.speedMap = new HashMap<>();
        rangeActionSpeedList.forEach(rangeActionSpeed -> speedMap.put(rangeActionSpeed.getRangeActionId(), rangeActionSpeed));
    }

    public Set<RangeActionSpeed> getRangeActionSpeedSet() {
        return new HashSet<>(speedMap.values());
    }

    public RangeActionSpeed getRangeActionSpeed(String remedialActionId) {
        return speedMap.get(remedialActionId);
    }

}
