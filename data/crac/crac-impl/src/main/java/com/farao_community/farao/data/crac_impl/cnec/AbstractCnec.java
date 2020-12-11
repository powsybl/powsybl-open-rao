/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.cnec;

import com.farao_community.farao.data.crac_api.AbstractIdentifiable;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_impl.NotSynchronizedException;
import com.powsybl.iidm.network.Network;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractCnec<I extends Cnec<I>> extends AbstractIdentifiable<I> implements Cnec<I> {

    protected final NetworkElement networkElement;
    protected final State state;
    protected boolean optimized;
    protected boolean monitored;
    protected boolean isSynchronized = false;

    protected AbstractCnec(String id, String name, NetworkElement networkElement, State state, boolean optimized, boolean monitored) {
        super(id, name);
        this.networkElement = networkElement;
        this.state = state;
        this.optimized = optimized;
        this.monitored = monitored;
    }

    protected AbstractCnec(String id, NetworkElement networkElement, State state, boolean optimized, boolean monitored) {
        super(id);
        this.networkElement = networkElement;
        this.state = state;
        this.optimized = optimized;
        this.monitored = monitored;
    }

    @Override
    public final State getState() {
        return state;
    }

    @Override
    public final NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public boolean isOptimized() {
        return optimized;
    }

    @Override
    public void setOptimized(boolean optimized) {
        this.optimized = optimized;
    }

    @Override
    public boolean isMonitored() {
        return monitored;
    }

    @Override
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    @Override
    public void synchronize(Network network) {
        isSynchronized = true;
    }

    @Override
    public void desynchronize() {
        isSynchronized = false;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }

    protected void checkSynchronized(String action) {
        if (!isSynchronized()) {
            throw new NotSynchronizedException(format("Cnec must be synchronized to perform this action: %s",  action));
        }
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
            && networkElement.equals(cnec.getNetworkElement())
            && state.equals(cnec.getState())
            && optimized == cnec.isOptimized()
            && monitored == cnec.isMonitored();
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + networkElement.hashCode();
        result = 31 * result + state.hashCode();
        return result;
    }
}
