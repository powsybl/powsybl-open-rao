/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.ThresholdAdderImpl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BranchCnecAdder extends AbstractIdentifiableAdder<BranchCnecAdder> implements CnecAdder {

    private SimpleCrac parent;
    private NetworkElement networkElement;
    private Set<AbstractFlowThreshold> thresholds = new HashSet<>();
    private Instant instant;
    private Contingency contingency;
    private double frm;
    private boolean optimized;
    private boolean monitored;

    public BranchCnecAdder(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    public void addThreshold(AbstractFlowThreshold threshold) {
        thresholds.add(threshold);
    }

    @Override
    public BranchCnecAdder setInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public BranchCnecAdder setContingency(Contingency contingency) {
        this.contingency = contingency;
        return this;
    }

    @Override
    public NetworkElementAdder newNetworkElement() {
        if (networkElement == null) {
            return new NetworkElementAdderImpl<CnecAdder>(this);
        } else {
            throw new FaraoException("Only one network element can be added to cnec.");
        }
    }

    @Override
    public ThresholdAdder newThreshold() {
        return new ThresholdAdderImpl(this);
    }

    @Override
    public CnecAdder setFrm(double frm) {
        this.frm = frm;
        return this;
    }

    @Override
    public CnecAdder setOptimized(boolean optimized) {
        this.optimized = optimized;
        return this;
    }

    @Override
    public CnecAdder setMonitored(boolean monitored) {
        this.monitored = monitored;
        return this;
    }

    @Override
    public Cnec add() {
        checkId();
        if (parent.getCnec(id) != null) {
            throw new FaraoException(format("Cannot add a cnec with an already existing ID - %s.", id));
        }
        if (this.networkElement == null) {
            throw new FaraoException("Cannot add a cnec without a network element. Please use newNetworkElement.");
        }
        if (this.thresholds.isEmpty()) {
            throw new FaraoException("Cannot add a cnec without a threshold. Please use newThreshold.");
        }
        if (this.instant == null) {
            throw new FaraoException("Cannot add a cnec with no specified state instant. Please use setInstant.");
        }
        parent.addNetworkElement(networkElement);
        State state = parent.addState(contingency, instant);
        return parent.addCnec(id, name, networkElement.getId(), thresholds, state.getId(), frm, optimized, monitored);
    }

    @Override
    public NetworkElement addNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
        return networkElement;
    }
}
