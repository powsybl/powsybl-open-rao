/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.farao_community.farao.data.crac_impl.threshold.ThresholdAdderImpl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SimpleCnecAdder extends AbstractIdentifiableAdder<SimpleCnecAdder> implements CnecAdder {

    private SimpleCrac parent;
    private NetworkElement networkElement;
    private Set<AbstractThreshold> thresholds = new HashSet<>();
    private Instant instant;
    private Contingency contingency;

    public SimpleCnecAdder(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    public void addThreshold(AbstractThreshold threshold) {
        thresholds.add(threshold);
    }

    @Override
    public SimpleCnecAdder setInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public SimpleCnecAdder setContingency(Contingency contingency) {
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
    public Cnec add() {
        checkId();
        if (this.networkElement == null) {
            throw new FaraoException("Cannot add a cnec without a network element. Please use newNetworkElement.");
        }
        if (this.thresholds.isEmpty()) {
            throw new FaraoException("Cannot add a cnec without a threshold. Please use newThreshold.");
        }
        if (this.instant == null) {
            throw new FaraoException("Cannot add a cnec with no specified state instant. Please use setInstant.");
        }
        SimpleState state = new SimpleState((this.contingency != null) ? Optional.of(this.contingency) : Optional.empty(), this.instant);
        SimpleCnec cnec = new SimpleCnec(this.id, this.name, networkElement, thresholds, state);
        parent.addCnec(cnec);
        return cnec;
    }

    @Override
    public NetworkElement addNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
        return networkElement;
    }
}
