/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.fasterxml.jackson.annotation.*;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Critical network element and contingency.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("simple-cnec")
@JsonIdentityInfo(scope = SimpleCnec.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIdentityReference(alwaysAsId = true)
public class SimpleCnec extends AbstractIdentifiable<Cnec> implements Cnec {
    private NetworkElement networkElement;
    private Set<AbstractThreshold> thresholds;
    private State state;
    private boolean isSynchronized;
    private double frm;
    private boolean optimized;
    private boolean monitored;

    @JsonCreator
    public SimpleCnec(@JsonProperty("id") String id, @JsonProperty("name") String name,
                      @JsonProperty("networkElement") NetworkElement networkElement,
                      @JsonProperty("thresholds") Set<AbstractThreshold> thresholds, @JsonProperty("state") State state,
                      @JsonProperty("frm") double frm, @JsonProperty("optimized") boolean optimized,
                      @JsonProperty("monitored") boolean monitored) {
        super(id, name);

        this.networkElement = networkElement;
        this.thresholds = new HashSet<>();
        thresholds.forEach(threshold -> threshold.setMargin(frm, Unit.MEGAWATT));
        thresholds.forEach(this::addThreshold);
        this.state = state;
        isSynchronized = false;
        this.frm = frm;
        this.optimized = optimized;
        this.monitored = monitored;
    }

    public SimpleCnec(@JsonProperty("id") String id, @JsonProperty("name") String name,
                      @JsonProperty("networkElement") NetworkElement networkElement,
                      @JsonProperty("thresholds") Set<AbstractThreshold> thresholds, @JsonProperty("state") State state,
                      @JsonProperty("frm") double frm) {
        this(id, name, networkElement, thresholds, state, frm, true, false);
    }

    public SimpleCnec(String id, String name,
                      NetworkElement networkElement,
                      Set<AbstractThreshold> thresholds, State state) {
        this(id, name, networkElement, thresholds, state, 0.0);
    }

    public SimpleCnec(String id, NetworkElement networkElement, Set<AbstractThreshold> thresholds, State state) {
        this(id, id, networkElement, thresholds, state);
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public double computeMargin(double actualValue, Unit unit) {
        return Math.min(getMaxThreshold(unit).orElse(Double.POSITIVE_INFINITY) - actualValue, actualValue - getMinThreshold(unit).orElse(Double.NEGATIVE_INFINITY));
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    public Set<Threshold> getThresholds() {
        Set<Threshold> getThresholds = new HashSet<>();
        getThresholds.addAll(this.thresholds);
        return getThresholds;
    }

    public void setThresholds(Set<AbstractThreshold> thresholds) {
        this.thresholds = thresholds;
    }

    public void addThreshold(AbstractThreshold threshold) {
        AbstractThreshold thresholdCopy = threshold.copy();
        thresholdCopy.setNetworkElement(networkElement);
        this.thresholds.add(thresholdCopy);
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
        //TODO: make this clean
        //return network.getBranch(getNetworkElement().getId()).getTerminal(((AbstractFlowThreshold) thresholds).getBranchSide()); // this is dirty but we can assume that this method will be used only if the threshold of the cnec is an abstractflowthreshold
        return network.getBranch(getNetworkElement().getId()).getTerminal(Branch.Side.ONE); // Very dirty! To handle the Cnecs with several thresholds, we consider that the terminal needed for the computation is always on Branch.Side.ONE
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
        if (!thresholds.isEmpty()) {
            PhysicalParameter physicalParameter = thresholds.iterator().next().getPhysicalParameter();
            if (thresholds.stream().allMatch(threshold -> threshold.getPhysicalParameter().equals(physicalParameter))) {
                return physicalParameter;
            } else {
                throw new FaraoException(String.format("Cnec %s has several physical parameters.", super.name));
            }
        } else {
            throw new FaraoException(String.format("Cnec %s has no threshold.", super.name));
        }
    }

    @Override
    public Optional<Double> getMinThreshold(Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        if (thresholds.stream().anyMatch(t -> t.getMinThreshold(requestedUnit).isPresent())) {
            return Optional.of(thresholds.stream().map(threshold -> threshold.getMinThreshold(requestedUnit).orElse(Double.NEGATIVE_INFINITY)).max(Double::compare).orElse(Double.NEGATIVE_INFINITY));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Double> getMaxThreshold(Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        if (thresholds.stream().anyMatch(t -> t.getMaxThreshold(requestedUnit).isPresent())) {
            return Optional.of(thresholds.stream().map(threshold -> threshold.getMaxThreshold(requestedUnit).orElse(Double.POSITIVE_INFINITY)).min(Double::compare).orElse(Double.POSITIVE_INFINITY));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public double getI(Network network) {
        return isCnecDisconnected(network) ? 0 : getTerminal(network).getI();
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
    public boolean isOptimized() {
        return optimized;
    }

    @Override
    public boolean isMonitored() {
        return monitored;
    }

    @Override
    public void synchronize(Network network) {
        thresholds.forEach(threshold -> threshold.synchronize(network));
        isSynchronized = true;
    }

    @Override
    public void desynchronize() {
        thresholds.forEach(AbstractThreshold::desynchronize);
        isSynchronized = false;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }

    public Cnec copy(NetworkElement networkElement, State state) {
        return new SimpleCnec(super.getId(), super.name, networkElement, thresholds, state);
    }

    public Cnec copy(NetworkElement networkElement, State state, double frm) {
        return new SimpleCnec(super.getId(), super.name, networkElement, thresholds, state, frm);
    }

    public Cnec copy(NetworkElement networkElement, State state, double frm, boolean optimized, boolean monitored) {
        return new SimpleCnec(super.getId(), super.name, networkElement, thresholds, state, frm, optimized, monitored);
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
            && state.equals(cnec.getState())
            && thresholds.equals(cnec.thresholds);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + networkElement.hashCode();
        result = 31 * result + state.hashCode();
        return result;
    }

    public double getFrm() {
        return frm;
    }

    public void setFrm(double frm) {
        this.frm = frm;
    }

    public void setOptimized(boolean optimized) {
        this.optimized = optimized;
    }

    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }
}
