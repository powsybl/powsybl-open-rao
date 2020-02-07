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
 * Injection setpoint remedial action: set a load or generator at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("injection-setpoint")
public final class InjectionSetpoint extends AbstractSetpointElementaryNetworkAction {

    @JsonCreator
    public InjectionSetpoint(@JsonProperty("id") String id,
                             @JsonProperty("name") String name,
                             @JsonProperty("operator") String operator,
                             @JsonProperty("usageRules") List<UsageRule> usageRules,
                             @JsonProperty("networkElement") NetworkElement networkElement,
                             @JsonProperty("setpoint")  double setpoint) {
        super(id, name, operator, usageRules, networkElement, setpoint);
    }

    public InjectionSetpoint(String id, NetworkElement networkElement, double setpoint) {
        super(id, networkElement, setpoint);
    }

    @Override
    public void apply(Network network) {
        throw new UnsupportedOperationException();
    }
}
