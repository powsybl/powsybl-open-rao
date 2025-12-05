/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.CnecAdder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractCnecAdderImpl<J extends CnecAdder<J>> extends AbstractIdentifiableAdder<J> implements CnecAdder<J> {

    protected CracImpl owner;
    protected Map<String, String> networkElementsIdAndName = new HashMap<>();
    protected String instantId;
    protected String contingencyId;
    protected boolean optimized = false;
    protected boolean monitored = false;
    protected double reliabilityMargin = .0;
    protected String operator;
    protected String border = "";

    protected AbstractCnecAdderImpl(CracImpl owner) {
        Objects.requireNonNull(owner);
        this.owner = owner;
    }

    protected void checkCnec() {
        checkId();
        AdderUtils.assertAttributeNotEmpty(networkElementsIdAndName.entrySet(), "Cnec", "network element", "withNetworkElement()");
        AdderUtils.assertAttributeNotNull(instantId, "Cnec", "instant", "withInstant()");

        if (owner.getInstant(instantId).isPreventive()) {
            if (contingencyId != null) {
                throw new OpenRaoException("You cannot define a contingency for a preventive cnec.");
            }
        } else {
            if (contingencyId == null) {
                throw new OpenRaoException("You must define a contingency for a non-preventive cnec.");
            } else if (owner.getContingency(contingencyId) == null) {
                throw new OpenRaoException(format("Contingency %s of Cnec %s does not exist in the crac. Use crac.newContingency() first.", contingencyId, id));
            }
        }
        networkElementsIdAndName.entrySet().forEach(entry -> this.owner.addNetworkElement(entry.getKey(), entry.getValue()));

        if (owner.getCnec(id) != null) {
            throw new OpenRaoException(format("Cannot add a cnec with an already existing ID - %s.", id));
        }
    }

    protected State getState() {
        State state;
        Instant instant = owner.getInstant(instantId);
        if (!instant.isPreventive()) {
            state = owner.addState(owner.getContingency(contingencyId), instant);
        } else {
            state = owner.addPreventiveState();
        }
        return state;
    }

    @Override
    public J withInstant(String instantId) {
        this.instantId = instantId;
        return (J) this;
    }

    @Override
    public J withContingency(String contingencyId) {
        this.contingencyId = contingencyId;
        return (J) this;
    }

    @Override
    public J withReliabilityMargin(double reliabilityMargin) {
        this.reliabilityMargin = reliabilityMargin;
        return (J) this;
    }

    @Override
    public J withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementsIdAndName.put(networkElementId, networkElementName);
        return (J) this;
    }

    @Override
    public J withNetworkElement(String networkElementId) {
        return withNetworkElement(networkElementId, networkElementId);
    }

    @Override
    public J withOperator(String operator) {
        this.operator = operator;
        return (J) this;
    }

    @Override
    public J withBorder(String border) {
        this.border = border;
        return (J) this;
    }

    @Override
    public J withOptimized() {
        return withOptimized(true);
    }

    @Override
    public J withMonitored() {
        return withMonitored(true);
    }

    @Override
    public J withOptimized(boolean optimized) {
        this.optimized = optimized;
        return (J) this;
    }

    @Override
    public J withMonitored(boolean monitored) {
        this.monitored = monitored;
        return (J) this;
    }
}
