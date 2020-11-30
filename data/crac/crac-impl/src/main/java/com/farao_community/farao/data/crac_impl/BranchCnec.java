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
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchCnec.class);

    private NetworkElement networkElement;
    private Set<AbstractFlowThreshold> thresholds;
    private State state;
    private boolean isSynchronized;
    private double frm;
    private boolean optimized;
    private boolean monitored;
    private BoundsCache bounds = new BoundsCache();
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

    /**
     * A margin can be computed on a {@code BranchCnec}. The {@code unit} is the one of the {@code leftFlow} and will be
     * the one of the returned margin. It is assumed in input that the {@code leftFlow} is defined on the
     * LEFT {@link Side}. Then given the thresholds and the parameters of the margin computation a conversion could be
     * done to get the flow on the right side. The only use case for this conversion is if both threshold
     * and {@code unit} are in AMPERE, the threshold is defined on the RIGHT {@link Side} and the {@code BranchCnec}
     * is on a transformer.
     *
     * @param leftFlow: Flow on the LEFT {@link Side} of the {@code BranchCnec} on which to make the difference to
     *                compute the threshold.
     * @param unit: Unit of the input flow. It will also be the one of the returned value.
     * @return A margin given {@code leftFlow} and {@code unit} for this flow threshold.
     */
    public double computeMargin(double leftFlow, Unit unit) {
        // Here we assume that actual value here will be always given for left side (or Side.ONE PowSyBl wise)
        double oppositeMargin = leftFlow - getMinThreshold(unit).orElse(Double.POSITIVE_INFINITY);
        double directMargin = getMaxThreshold(unit).orElse(Double.POSITIVE_INFINITY) - leftFlow;
        return Math.abs(oppositeMargin) < Math.abs(directMargin) ? oppositeMargin : directMargin;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    public Set<AbstractFlowThreshold> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Set<AbstractFlowThreshold> thresholds) {
        bounds.resetBounds();
        this.thresholds = thresholds;
    }

    public void addThreshold(AbstractFlowThreshold threshold) {
        bounds.resetBounds();
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
        if (!bounds.isLowerBoundComputed(requestedUnit)) {
            LOGGER.debug("Lower bound computed for {} in {}", getId(), requestedUnit);
            // For now we assume that we convert thresholds towards LEFT side
            Set<AbstractFlowThreshold> limitingThresholds = thresholds.stream()
                .filter(threshold -> threshold.getMinThreshold(requestedUnit).isPresent())
                .collect(Collectors.toSet());
            if (!limitingThresholds.isEmpty()) {
                double mostLimitingThreshold = Double.MAX_VALUE;
                for (AbstractFlowThreshold threshold : limitingThresholds) {
                    double thresholdValue = convertThresholdValue(threshold, requestedUnit, threshold.getMinThreshold(requestedUnit).orElseThrow());
                    if (Math.abs(thresholdValue) < Math.abs(mostLimitingThreshold)) {
                        mostLimitingThreshold = thresholdValue;
                    }
                }
                bounds.setLowerBound(mostLimitingThreshold, requestedUnit);
            } else {
                bounds.setLowerBound(null, requestedUnit);
            }
        }
        return Optional.ofNullable(bounds.getLowerBound(requestedUnit));
    }

    @Override
    public Optional<Double> getMaxThreshold(Unit requestedUnit) {
        // For now we assume that we convert thresholds towards LEFT side
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        if (!bounds.isUpperBoundComputed(requestedUnit)) {
            LOGGER.debug("Upper bound computed for {} in {}", getId(), requestedUnit);
            Set<AbstractFlowThreshold> limitingThresholds = thresholds.stream()
                .filter(threshold -> threshold.getMaxThreshold(requestedUnit).isPresent())
                .collect(Collectors.toSet());
            if (!limitingThresholds.isEmpty()) {
                double mostLimitingThreshold = Double.MAX_VALUE;
                for (AbstractFlowThreshold threshold : limitingThresholds) {
                    double thresholdValue = convertThresholdValue(threshold, requestedUnit, threshold.getMaxThreshold(requestedUnit).orElseThrow());
                    if (Math.abs(thresholdValue) < Math.abs(mostLimitingThreshold)) {
                        mostLimitingThreshold = thresholdValue;
                    }
                }
                bounds.setUpperBound(mostLimitingThreshold, requestedUnit);
            } else {
                bounds.setUpperBound(null, requestedUnit);
            }
        }
        return Optional.ofNullable(bounds.getUpperBound(requestedUnit));
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
