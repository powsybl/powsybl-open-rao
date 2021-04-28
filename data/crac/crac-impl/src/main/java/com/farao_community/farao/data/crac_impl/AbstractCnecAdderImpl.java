/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.CnecAdder;

import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractCnecAdderImpl<J extends CnecAdder<J>> extends AbstractIdentifiableAdder<J> implements CnecAdder<J> {

    protected CracImpl owner;
    protected String networkElementId;
    protected String networkElementName;
    protected Instant instant;
    protected String contingencyId;
    protected boolean optimized = false;
    protected boolean monitored = false;
    protected double reliabilityMargin = .0;
    protected String operator;

    protected AbstractCnecAdderImpl(CracImpl owner) {
        Objects.requireNonNull(owner);
        this.owner = owner;
    }

    protected void checkCnec() {
        checkId();
        AdderUtils.assertAttributeNotNull(networkElementId, "Cnec", "network element", "withNetworkElement()");
        AdderUtils.assertAttributeNotNull(instant, "Cnec", "instant", "withInstant()");

        if (instant.equals(Instant.PREVENTIVE)) {
            if (contingencyId != null) {
                throw new FaraoException("You cannot define a contingency for a preventive cnec.");
            }
        } else {
            if (contingencyId == null) {
                throw new FaraoException("You must define a contingency for a non-preventive cnec.");
            } else if (owner.getContingency(contingencyId) == null) {
                throw new FaraoException(String.format("Contingency %s of Cnec %s does not exist in the crac. Use crac.newContingency() first.", contingencyId, id));
            }
        }
        this.owner.addNetworkElement(networkElementId, networkElementName);
    }

    @Override
    public J withInstant(Instant instant) {
        this.instant = instant;
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
        if (this.networkElementId != null) {
            throw new FaraoException("Cannot add multiple network elements for a cnec.");
        } else {
            this.networkElementId = networkElementId;
            this.networkElementName = networkElementName;
            return (J) this;
        }
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
