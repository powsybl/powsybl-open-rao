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
import com.farao_community.farao.data.crac_impl.threshold.AbstractFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.fasterxml.jackson.annotation.*;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Critical network element and contingency.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("simple-cnec")
@JsonIdentityInfo(scope = BranchCnec.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIdentityReference(alwaysAsId = true)
public class BranchCnec extends AbstractIdentifiable<Cnec> implements Cnec {
    private NetworkElement networkElement;
    private Set<AbstractFlowThreshold> thresholds;
    private State state;
    private boolean isSynchronized;
    private double frm;
    private boolean optimized;
    private boolean monitored;
    private final double[] voltageLevels = new double[2];

    @JsonCreator
    public BranchCnec(@JsonProperty("id") String id, @JsonProperty("name") String name,
                      @JsonProperty("networkElement") NetworkElement networkElement,
                      @JsonProperty("thresholds") Set<AbstractFlowThreshold> thresholds, @JsonProperty("state") State state,
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

    public BranchCnec(@JsonProperty("id") String id, @JsonProperty("name") String name,
                      @JsonProperty("networkElement") NetworkElement networkElement,
                      @JsonProperty("thresholds") Set<AbstractFlowThreshold> thresholds, @JsonProperty("state") State state,
                      @JsonProperty("frm") double frm) {
        this(id, name, networkElement, thresholds, state, frm, true, false);
    }

    public BranchCnec(String id, String name,
                      NetworkElement networkElement,
                      Set<AbstractFlowThreshold> thresholds, State state) {
        this(id, name, networkElement, thresholds, state, 0.0);
    }

    public BranchCnec(String id, NetworkElement networkElement, Set<AbstractFlowThreshold> thresholds, State state) {
        this(id, id, networkElement, thresholds, state);
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public double computeMargin(double leftFlow, Unit unit) {
        // Here we assume that actual value here will be always given for left side (or Side.ONE PowSyBl wise)
        double worstMargin = Double.MAX_VALUE;
        for (AbstractFlowThreshold threshold : thresholds) {
            double flow = leftFlow;
            if (threshold.getSide().equals(Side.RIGHT)
                && unit.equals(Unit.AMPERE)
                && threshold.getUnit().equals(Unit.AMPERE)
                && getVoltageLevel(Side.LEFT) != getVoltageLevel(Side.RIGHT)) {
                flow = leftFlow * getVoltageLevel(Side.LEFT) / getVoltageLevel(Side.RIGHT);
            }
            double margin = threshold.computeMargin(flow, unit);
            if (Math.abs(margin) < Math.abs(worstMargin)) {
                worstMargin = margin;
            }
        }
        return worstMargin;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    public Set<AbstractFlowThreshold> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Set<AbstractFlowThreshold> thresholds) {
        this.thresholds = thresholds;
    }

    public void addThreshold(AbstractFlowThreshold threshold) {
        AbstractFlowThreshold thresholdCopy = threshold.copy();
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

    private boolean isCnecInMainComponent(Network network) {
        return getTerminal(network).getBusView().getBus().isInMainSynchronousComponent();
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
        // For now we assume that we convert thresholds towards LEFT side
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        Set<AbstractFlowThreshold> limitingThresholds = thresholds.stream()
            .filter(threshold -> threshold.getMinThreshold(requestedUnit).isPresent())
            .collect(Collectors.toSet());
        if (!limitingThresholds.isEmpty()) {
            double mostLimitingThreshold = Double.MAX_VALUE;
            for (AbstractFlowThreshold threshold : limitingThresholds) {
                double thresholdValue = convertThresholdValue(threshold, requestedUnit, threshold.getMinThreshold(requestedUnit).get());
                if (thresholdValue < mostLimitingThreshold) {
                    mostLimitingThreshold = thresholdValue;
                }
            }
            return Optional.of(mostLimitingThreshold);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Double> getMaxThreshold(Unit requestedUnit) {
        // For now we assume that we convert thresholds towards LEFT side
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        Set<AbstractFlowThreshold> limitingThresholds = thresholds.stream()
            .filter(threshold -> threshold.getMaxThreshold(requestedUnit).isPresent())
            .collect(Collectors.toSet());
        if (!limitingThresholds.isEmpty()) {
            double mostLimitingThreshold = Double.MAX_VALUE;
            for (AbstractFlowThreshold threshold : limitingThresholds) {
                double thresholdValue = convertThresholdValue(threshold, requestedUnit, threshold.getMaxThreshold(requestedUnit).get());
                if (thresholdValue < mostLimitingThreshold) {
                    mostLimitingThreshold = thresholdValue;
                }
            }
            return Optional.of(mostLimitingThreshold);
        } else {
            return Optional.empty();
        }
    }

    private double convertThresholdValue(AbstractFlowThreshold threshold, Unit requestedUnit, double thresholdValue) {
        if (threshold.getSide().equals(Side.RIGHT)
            && requestedUnit.equals(Unit.AMPERE)
            && getVoltageLevel(Side.LEFT) != getVoltageLevel(Side.RIGHT)) {
            return thresholdValue * getVoltageLevel(Side.RIGHT) / getVoltageLevel(Side.LEFT);
        }
        return thresholdValue;
    }

    @Override
    public double getI(Network network) {
        return isCnecDisconnected(network) ? 0 : getTerminal(network).getI();
    }

    @Override
    public double getP(Network network) {
        double p = getTerminal(network).getP();
        if (Double.isNaN(p)) {
            if (isCnecDisconnected(network) || !isCnecInMainComponent(network)) {
                return 0.0;
            }
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
        setVoltageLevel(Side.LEFT, network.getBranch(networkElement.getId()).getTerminal1().getVoltageLevel().getNominalV());
        setVoltageLevel(Side.RIGHT, network.getBranch(networkElement.getId()).getTerminal2().getVoltageLevel().getNominalV());
        isSynchronized = true;
    }

    private double getVoltageLevel(Side side) {
        return voltageLevels[side.equals(Side.LEFT) ? 0 : 1];
    }

    private void setVoltageLevel(Side side, double value) {
        voltageLevels[side.equals(Side.LEFT) ? 0 : 1] = value;
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
        return new BranchCnec(super.getId(), super.name, networkElement, thresholds, state);
    }

    public Cnec copy(NetworkElement networkElement, State state, double frm) {
        return new BranchCnec(super.getId(), super.name, networkElement, thresholds, state, frm);
    }

    public Cnec copy(NetworkElement networkElement, State state, double frm, boolean optimized, boolean monitored) {
        return new BranchCnec(super.getId(), super.name, networkElement, thresholds, state, frm, optimized, monitored);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BranchCnec cnec = (BranchCnec) o;
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

    @Override
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
