/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This abstract class gathers methods and attributes commons to {@link HvdcSetpoint}s and {@link PstSetpoint}s.
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public abstract class AbstractSetpointNetworkAction extends AbstractNetworkElementAction {

    protected double setpoint;

    public AbstractSetpointNetworkAction(@JsonProperty("id") String id,
                                        @JsonProperty("name") String name,
                                        @JsonProperty("operator") String operator,
                                        @JsonProperty("usageRules") List<UsageRule> usageRules,
                                        @JsonProperty("networkElement") NetworkElement networkElement,
                                         double setpoint) {
        super(id, name, operator, usageRules, networkElement);
        this.setpoint = setpoint;
    }

    public AbstractSetpointNetworkAction(String id,
                                         NetworkElement networkElement,
                                         double setpoint) {
        super(id, networkElement);
        this.setpoint = setpoint;
    }
}
