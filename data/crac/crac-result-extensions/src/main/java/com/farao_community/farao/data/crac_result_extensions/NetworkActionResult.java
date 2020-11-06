/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeName("network-action-result")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class NetworkActionResult implements Result {
    protected Map<String, Boolean> activationMap;

    @JsonCreator
    public NetworkActionResult(@JsonProperty("activationMap") Map<String, Boolean> activationMap) {
        this.activationMap = activationMap;
    }

    public NetworkActionResult(Set<String> stateIds) {
        activationMap = new HashMap<>();
        stateIds.forEach(state -> activationMap.put(state, false));
    }

    public boolean isActivated(String stateId) {
        return activationMap.get(stateId);
    }

    public void activate(String stateId) {
        activationMap.put(stateId, true);
    }

    public void deactivate(String stateId) {
        activationMap.put(stateId, false);
    }

}
