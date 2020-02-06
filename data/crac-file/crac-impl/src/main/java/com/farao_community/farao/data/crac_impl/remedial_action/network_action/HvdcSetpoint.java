/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * HVDC setpoint remedial action: set an HVDC line's setpoint at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("hvdc-setpoint")
public final class HvdcSetpoint extends AbstractSetpointElementaryNetworkAction {

    @JsonCreator
    public HvdcSetpoint(@JsonProperty("id") String id,
                        @JsonProperty("name") String name,
                        @JsonProperty("operator") String operator,
                        @JsonProperty("usageRules") List<UsageRule> usageRules,
                        @JsonProperty("networkElement") NetworkElement networkElement,
                        @JsonProperty("setpoint") double setpoint) {
        super(id, name, operator, usageRules, networkElement, setpoint);
    }

    public HvdcSetpoint(String id, NetworkElement networkElement, double setpoint) {
        super(id, networkElement, setpoint);
    }

    public double getSetpoint() {
        return setpoint;
    }

    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    @Override
    public void apply(Network network) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HvdcSetpoint otherHvdcSetpoint = (HvdcSetpoint) o;
        return super.equals(o) && setpoint == otherHvdcSetpoint.getSetpoint();
    }

    @Override
    public int hashCode() {
        return String.format("%s%f", getId(), setpoint).hashCode();
    }
}
