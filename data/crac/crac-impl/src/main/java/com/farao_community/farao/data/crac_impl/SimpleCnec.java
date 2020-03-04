/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.fasterxml.jackson.annotation.*;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;

import java.util.Optional;

/**
 * Critical network element and contingency.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("simple-cnec")
@JsonIdentityInfo(scope = SimpleCnec.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class SimpleCnec extends AbstractIdentifiable<Cnec> implements Cnec {
    private NetworkElement networkElement;
    private AbstractThreshold thresholds;
    private State state;
    private boolean isSynchronized;

    @JsonCreator
    public SimpleCnec(@JsonProperty("id") String id, @JsonProperty("name") String name,
                      @JsonProperty("networkElement") NetworkElement networkElement,
                      @JsonProperty("thresholds") AbstractThreshold thresholds, @JsonProperty("state") State state) {
        super(id, name);

        this.networkElement = networkElement;
        this.thresholds = thresholds.copy();
        this.thresholds.setNetworkElement(networkElement);
        this.state = state;
        isSynchronized = false;
    }

    public SimpleCnec(String id, NetworkElement networkElement, AbstractThreshold thresholds, State state) {
        this(id, id, networkElement, thresholds, state);
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public double computeMargin(Network network) {
        // todo : switch units if no I is available but P is available
        // todo : add a requested unit
        double flow;
        Unit unit = thresholds.getUnit();
        if (unit.equals(Unit.AMPERE)) {
            flow = getI(network);
        } else {
            flow = getP(network);
        }
        return Math.min(thresholds.getMaxThreshold(unit).orElse(Double.POSITIVE_INFINITY) - flow, flow - thresholds.getMinThreshold(unit).orElse(Double.NEGATIVE_INFINITY));
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    public AbstractThreshold getThresholds() {
        return thresholds;
    }

    public void setThresholds(AbstractThreshold thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @JsonIgnore
    public boolean isBasecase() {
        return !state.getContingency().isPresent();
    }

    /**
     * Get the monitored Terminal of a Cnec.
     */
    private Terminal getTerminal(Network network) {
        return network.getBranch(getNetworkElement().getId()).getTerminal(((AbstractFlowThreshold) thresholds).getBranchSide()); // this is dirty but we can assume that this method will be used only if the threshold of the cnec is an abstractflowthreshold
    }

    /**
     * Check if a Cnec is connected, on both side, to the network.
     */
    private boolean isCnecDisconnected(Network network) {
        Branch branch = network.getBranch(getNetworkElement().getId());
        return !branch.getTerminal1().isConnected() || !branch.getTerminal2().isConnected();
    }

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return thresholds.getPhysicalParameter();
    }

    @Override
    public Optional<Double> getMinThreshold(Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        return thresholds.getMinThreshold(requestedUnit);
    }

    @Override
    public Optional<Double> getMaxThreshold(Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        return thresholds.getMaxThreshold(requestedUnit);
    }

    @Override
    public double getI(Network network) {
        double i = isCnecDisconnected(network) ? 0 : getTerminal(network).getI();
        if (Double.isNaN(i)) {
            throw new FaraoException(String.format("No intensity (I) data available for CNEC %s", getName()));
        }
        return i;
    }

    @Override
    public double getP(Network network) {
        double p = isCnecDisconnected(network) ? 0 : getTerminal(network).getP();
        if (Double.isNaN(p)) {
            throw new FaraoException(String.format("No transmitted power (P) data available for CNEC %s", getName()));
        }
        return p;
    }

    @Override
    public void synchronize(Network network) {
        thresholds.synchronize(network);
        isSynchronized = true;
    }

    @Override
    public void desynchronize() {
        thresholds.desynchronize();
        isSynchronized = false;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }

    public Cnec copy(NetworkElement networkElement, State state) {
        return new SimpleCnec(super.getId(), super.name, networkElement, thresholds, state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleCnec cnec = (SimpleCnec) o;
        return super.equals(cnec) && networkElement.equals(cnec.getNetworkElement())
            && state.equals(cnec.getState()) && thresholds.equals(cnec.thresholds);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + networkElement.hashCode();
        result = 31 * result + state.hashCode();
        result = 31 * result + thresholds.hashCode();
        return result;
    }
}
