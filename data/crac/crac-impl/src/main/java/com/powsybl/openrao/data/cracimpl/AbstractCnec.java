/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractCnec<I extends Cnec<I>> extends AbstractIdentifiable<I> implements Cnec<I> {

    protected final Set<NetworkElement> networkElements;
    protected final State state;
    protected boolean optimized;
    protected boolean monitored;
    protected final String operator;
    protected final String border;

    protected AbstractCnec(String id,
                           String name,
                           Set<NetworkElement> networkElements,
                           String operator,
                           String border,
                           State state,
                           boolean optimized,
                           boolean monitored) {
        super(id, name);
        this.networkElements = networkElements;
        this.operator = operator;
        this.border = border;
        this.state = state;
        this.optimized = optimized;
        this.monitored = monitored;
    }

    @Override
    public final State getState() {
        return state;
    }

    @Override
    public final Set<NetworkElement> getNetworkElements() {
        return networkElements;
    }

    @Override
    public boolean isOptimized() {
        return optimized;
    }

    @Override
    @Deprecated (since = "3.0.0")
    public void setOptimized(boolean optimized) {
        this.optimized = optimized;
    }

    @Override
    public boolean isMonitored() {
        return monitored;
    }

    @Override
    @Deprecated (since = "3.0.0")
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    @Override
    public String getOperator() {
        return this.operator;
    }

    @Override
    public String getBorder() {
        return border;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractCnec<?> cnec = (AbstractCnec<?>) o;
        return super.equals(cnec)
            && networkElements.equals(cnec.getNetworkElements())
            && state.equals(cnec.getState())
            && optimized == cnec.isOptimized()
            && monitored == cnec.isMonitored();
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + networkElements.hashCode();
        result = 31 * result + state.hashCode();
        return result;
    }
}
