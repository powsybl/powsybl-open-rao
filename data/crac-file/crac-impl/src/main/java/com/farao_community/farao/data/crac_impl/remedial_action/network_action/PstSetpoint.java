/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRange;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * PST setpoint remedial action: set a PST's tap at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("pst-setpoint")
public final class PstSetpoint extends AbstractSetpointElementaryNetworkAction {

    @JsonCreator
    public PstSetpoint(@JsonProperty("id") String id,
                       @JsonProperty("name") String name,
                       @JsonProperty("operator") String operator,
                       @JsonProperty("usageRules") List<UsageRule> usageRules,
                       @JsonProperty("networkElement") NetworkElement networkElement,
                       @JsonProperty("setpoint") double setpoint) {
        super(id, name, operator, usageRules, networkElement, setpoint);
    }

    public PstSetpoint(String id, String name, String operator, NetworkElement networkElement, double setpoint) {
        super(id, name, operator, networkElement, setpoint);
    }

    /**
     * Constructor of a remedial action on a PST to fix a tap
     *
     * @param id value used for id, name and operator
     * @param networkElement PST element to modify
     * @param setpoint value of the tap. That should be an int value, if not it will be truncated.
     */
    public PstSetpoint(String id, NetworkElement networkElement, double setpoint) {
        super(id, networkElement, setpoint);
    }

    /**
     * Change tap position of the PST pointed by the network element at the tap given at object instantiation.
     *
     * @param network network to modify
     */
    @Override
    public void apply(Network network) {
        PstRange pst = new PstRange(getId(), networkElement);
        pst.apply(network, setpoint);
    }
}
