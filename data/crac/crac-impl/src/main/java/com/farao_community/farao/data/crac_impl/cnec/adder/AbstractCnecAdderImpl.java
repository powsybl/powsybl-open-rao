/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.cnec.adder;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.adder.CnecAdder;
import com.farao_community.farao.data.crac_impl.AbstractIdentifiableAdder;
import com.farao_community.farao.data.crac_impl.NetworkElementAdderImpl;
import com.farao_community.farao.data.crac_impl.SimpleCrac;

import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractCnecAdderImpl<I extends Cnec<I>, J extends CnecAdder<I, J>> extends AbstractIdentifiableAdder<J> implements CnecAdder<I, J> {

    protected SimpleCrac parent;
    protected NetworkElement networkElement;
    protected Instant instant;
    protected Contingency contingency;
    protected boolean optimized;
    protected boolean monitored;
    protected double reliabilityMargin;
    protected String operator;

    protected AbstractCnecAdderImpl(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    protected void checkCnec() {
        checkId();
        if (this.networkElement == null) {
            throw new FaraoException("Cannot add a cnec without a network element. Please use newNetworkElement.");
        }
        if (this.instant == null) {
            throw new FaraoException("Cannot add a cnec with no specified state instant. Please use setInstant.");
        }
        parent.addNetworkElement(networkElement);
    }

    @Override
    public J setInstant(Instant instant) {
        this.instant = instant;
        return (J) this;
    }

    @Override
    public J setContingency(Contingency contingency) {
        this.contingency = contingency;
        return (J) this;
    }

    @Override
    public NetworkElementAdder<J> newNetworkElement() {
        if (networkElement == null) {
            return new NetworkElementAdderImpl<>((J) this);
        } else {
            throw new FaraoException("Only one network element can be added to cnec.");
        }
    }

    @Override
    public J optimized() {
        this.optimized = true;
        return (J) this;
    }

    @Override
    public J monitored() {
        this.monitored = true;
        return (J) this;
    }

    @Override
    public J setReliabilityMargin(double reliabilityMargin) {
        this.reliabilityMargin = reliabilityMargin;
        return (J) this;
    }

    @Override
    public J addNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
        return (J) this;
    }

    @Override
    public J setOperator(String operator) {
        this.operator = operator;
        return (J) this;
    }
}
