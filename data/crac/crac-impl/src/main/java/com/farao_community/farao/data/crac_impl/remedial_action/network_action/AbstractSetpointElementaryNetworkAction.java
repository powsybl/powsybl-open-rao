/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;

import java.util.List;

/**
 * This abstract class gathers methods and attributes commons to {@link HvdcSetpoint}s and {@link PstSetpoint}s.
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public abstract class AbstractSetpointElementaryNetworkAction extends AbstractElementaryNetworkAction {

    protected double setpoint;

    public AbstractSetpointElementaryNetworkAction(String id, String name, String operator, List<UsageRule> usageRules,
                                                   NetworkElement networkElement, double setpoint) {
        super(id, name, operator, usageRules, networkElement);
        this.setpoint = setpoint;
    }

    public AbstractSetpointElementaryNetworkAction(String id, String name, String operator, NetworkElement networkElement, double setpoint) {
        super(id, name, operator, networkElement);
        this.setpoint = setpoint;
    }

    public AbstractSetpointElementaryNetworkAction(String id, NetworkElement networkElement, double setpoint) {
        super(id, networkElement);
        this.setpoint = setpoint;
    }

    public double getSetpoint() {
        return setpoint;
    }

    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractSetpointElementaryNetworkAction otherAbstractSetpointElementary = (AbstractSetpointElementaryNetworkAction) o;
        return super.equals(otherAbstractSetpointElementary) && setpoint == otherAbstractSetpointElementary.setpoint;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) setpoint;
        return result;
    }
}
