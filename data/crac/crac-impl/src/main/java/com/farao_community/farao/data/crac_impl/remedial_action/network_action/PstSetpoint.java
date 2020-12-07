/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.PstSetPointSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;

import java.util.List;

import static com.farao_community.farao.data.crac_api.RangeDefinition.CENTERED_ON_ZERO;
import static com.farao_community.farao.data.crac_api.RangeDefinition.STARTS_AT_ONE;

/**
 * PST setpoint remedial action: set a PST's tap at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("pst-setpoint")
@JsonSerialize(using = PstSetPointSerializer.class)
public final class PstSetpoint extends AbstractSetpointElementaryNetworkAction {

    private RangeDefinition rangeDefinition;

    @JsonCreator
    public PstSetpoint(@JsonProperty("id") String id,
                       @JsonProperty("name") String name,
                       @JsonProperty("operator") String operator,
                       @JsonProperty("usageRules") List<UsageRule> usageRules,
                       @JsonProperty("networkElement") NetworkElement networkElement,
                       @JsonProperty("setpoint") double setpoint,
                       @JsonProperty("rangeDefinition") RangeDefinition rangeDefinition) {
        super(id, name, operator, usageRules, networkElement, setpoint);
        this.rangeDefinition = rangeDefinition;
    }

    public PstSetpoint(String id, String name, String operator, NetworkElement networkElement, double setpoint, RangeDefinition rangeDefinition) {
        super(id, name, operator, networkElement, setpoint);
        this.rangeDefinition = rangeDefinition;
    }

    /**
     * @param id              value used for id, name and operator
     * @param networkElement  PST element to modify
     * @param setpoint        value of the tap. That should be an int value, if not it will be truncated. The convention depends
     *                        on the rangeDefinition value
     * @param rangeDefinition value used to define which convention type is used for the setpoint value,
     *                        "starts at 1" means we have to put a set point as if the lowest position of the PST tap is 1
     *                        "centered on zero" means that there is no conversion of the setpoint, this is the real value
     */
    public PstSetpoint(String id, NetworkElement networkElement, double setpoint, RangeDefinition rangeDefinition) {
        super(id, networkElement, setpoint);
        this.rangeDefinition = rangeDefinition;
    }

    public RangeDefinition getRangeDefinition() {
        return this.rangeDefinition;
    }

    public void setRangeDefinition(RangeDefinition rangeDefinition) {
        this.rangeDefinition = rangeDefinition;
    }

    /**
     * Change tap position of the PST pointed by the network element at the tap given at object instantiation.
     *
     * @param network network to modify
     */
    @Override
    public void apply(Network network) {
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(networkElement.getId()).getPhaseTapChanger();

        int normalizedSetPoint = 0;

        if (rangeDefinition == CENTERED_ON_ZERO) {
            normalizedSetPoint = ((phaseTapChanger.getLowTapPosition() + phaseTapChanger.getHighTapPosition()) / 2) + (int) setpoint;
        } else if (rangeDefinition == STARTS_AT_ONE) {
            normalizedSetPoint = phaseTapChanger.getLowTapPosition() + (int) setpoint - 1;
        }

        if (normalizedSetPoint >= phaseTapChanger.getLowTapPosition() && normalizedSetPoint <= phaseTapChanger.getHighTapPosition()) {
            phaseTapChanger.setTapPosition(normalizedSetPoint);
        } else {
            throw new FaraoException(String.format(
                    "Tap value %d not in the range of high and low tap positions [%d,%d] of the phase tap changer %s steps",
                    normalizedSetPoint,
                    phaseTapChanger.getLowTapPosition(),
                    phaseTapChanger.getHighTapPosition(),
                    networkElement.getId()));
        }
    }
}
