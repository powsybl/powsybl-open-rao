/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.AlreadySynchronizedException;
import com.farao_community.farao.data.crac_impl.NotSynchronizedException;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerStep;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Elementary PST range remedial action.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("pst-with-range")
public final class PstWithRange extends AbstractElementaryRangeAction implements PstRange {
    private int lowTapPosition;
    private int highTapPosition;
    private int initialTapPosition;
    private boolean isSynchronized;

    private static final double EPSILON = 1e-3;

    /**
     * Constructor of a remedial action on a PST. The value of the tap to set will be specify at the application.
     *
     * @param networkElement: PST element to modify
     */
    @JsonCreator
    public PstWithRange(@JsonProperty("id") String id,
                        @JsonProperty("name") String name,
                        @JsonProperty("operator") String operator,
                        @JsonProperty("usageRules") List<UsageRule> usageRules,
                        @JsonProperty("ranges") List<Range> ranges,
                        @JsonProperty("networkElement") NetworkElement networkElement) {
        super(id, name, operator, usageRules, ranges, networkElement);
        initAttributes();
    }

    public PstWithRange(String id, String name, String operator, NetworkElement networkElement) {
        super(id, name, operator, networkElement);
        initAttributes();
    }

    public PstWithRange(String id, NetworkElement networkElement) {
        super(id, networkElement);
        initAttributes();
    }

    private void initAttributes() {
        lowTapPosition = 0;
        highTapPosition = 0;
        initialTapPosition = 0;
        isSynchronized = false;
    }

    @Override
    public void synchronize(Network network) {
        if (isSynchronized()) {
            throw new AlreadySynchronizedException(String.format("PST %s has already been synchronized", getId()));
        }
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(networkElement.getId()).getPhaseTapChanger();
        initialTapPosition = phaseTapChanger.getTapPosition();
        lowTapPosition = phaseTapChanger.getLowTapPosition();
        highTapPosition = phaseTapChanger.getHighTapPosition();
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

    @Override
    public double getMinValue(Network network) {
        if (!isSynchronized) {
            throw new NotSynchronizedException(String.format("PST %s have not been synchronized so its min value cannot be accessed", getId()));
        }
        double minValue = convertTapToAngle(network, lowTapPosition);
        for (Range range: ranges) {
            minValue = Math.max(getMinValueWithRange(network, range), minValue);
        }
        return minValue;
    }

    @Override
    public double getMaxValue(Network network) {
        if (!isSynchronized) {
            throw new NotSynchronizedException(String.format("PST %s have not been synchronized so its max value cannot be accessed", getId()));
        }
        double maxValue = convertTapToAngle(network, highTapPosition);
        for (Range range: ranges) {
            maxValue = Math.min(getMaxValueWithRange(network, range), maxValue);
        }
        return maxValue;
    }

    @Override
    protected double getMinValueWithRange(Network network, Range range) {
        double minValue = range.getMin();
        return convertTapToAngle(network, Math.max(lowTapPosition, (int) getExtremumValueWithRange(range, getCurrentTapPosition(network), minValue)));
    }

    @Override
    protected double getMaxValueWithRange(Network network, Range range) {
        double maxValue = range.getMax();
        return convertTapToAngle(network, Math.min(highTapPosition, (int) getExtremumValueWithRange(range, getCurrentTapPosition(network), maxValue)));
    }

    @Override
    public double getMaxNegativeVariation(Network network) {
        // This method calls getMinValue so it will throw a NotSynchronizedException if required
        return Math.max(convertTapToAngle(network, getCurrentTapPosition(network)) - getMinValue(network), 0);
    }

    @Override
    public double getMaxPositiveVariation(Network network) {
        // This method calls getMaxValue so it will throw a NotSynchronizedException if required
        return Math.max(getMaxValue(network) - convertTapToAngle(network, getCurrentTapPosition(network)), 0);
    }

    private int getCurrentTapPosition(Network network) {
        return network.getTwoWindingsTransformer(networkElement.getId()).getPhaseTapChanger().getTapPosition();
    }

    private double convertTapToAngle(Network network, int tap) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElement.getId());
        return transformer.getPhaseTapChanger().getStep(tap).getAlpha();
    }

    private double getExtremumValueWithRange(Range range, double currentTapPosition, double extremumValue) {
        switch (range.getRangeType()) {
            case ABSOLUTE_FIXED:
                switch (range.getRangeDefinition()) {
                    case STARTS_AT_ONE:
                        return lowTapPosition + extremumValue - 1;
                    case CENTERED_ON_ZERO:
                        return ((double) lowTapPosition + highTapPosition) / 2 + extremumValue;
                    default:
                        throw new FaraoException("Unknown range definition");
                }
            case RELATIVE_FIXED:
                return initialTapPosition + extremumValue;
            case RELATIVE_DYNAMIC:
                return currentTapPosition + extremumValue;
            default:
                throw new FaraoException("Unknown range type");
        }
    }

    /**
     * Change tap position of the PST pointed by the network element.
     *
     * @param network: network to modify
     * @param finalAngle: angle value to set on the PST.
     */
    @Override
    public void apply(Network network, double finalAngle) {
        PhaseTapChanger phaseTapChanger = checkValidPstAndGetPhaseTapChanger(network);
        int setpoint = computeTapPosition(finalAngle, phaseTapChanger);
        phaseTapChanger.setTapPosition(setpoint);
    }

    private PhaseTapChanger checkValidPstAndGetPhaseTapChanger(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElement.getId());
        if (transformer == null) {
            throw new FaraoException(String.format("PST %s does not exist in the current network", networkElement.getId()));
        }
        PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
        if (phaseTapChanger == null) {
            throw new FaraoException(String.format("Transformer %s is not a PST but is defined as a PstRange", networkElement.getId()));
        }
        return phaseTapChanger;
    }

    @Override
    public int computeTapPosition(double finalAngle, PhaseTapChanger phaseTapChanger) {

        Map<Integer, PhaseTapChangerStep> steps = new TreeMap<>();
        for (int tapPosition = phaseTapChanger.getLowTapPosition(); tapPosition <= phaseTapChanger.getHighTapPosition(); tapPosition++) {
            steps.put(tapPosition, phaseTapChanger.getStep(tapPosition));
        }
        double minAngle = steps.values().stream().mapToDouble(PhaseTapChangerStep::getAlpha).min().orElse(Double.NaN);
        double maxAngle = steps.values().stream().mapToDouble(PhaseTapChangerStep::getAlpha).max().orElse(Double.NaN);
        if (Double.isNaN(minAngle) || Double.isNaN(maxAngle)) {
            throw new FaraoException(String.format("Phase tap changer %s steps may be invalid", networkElement.getId()));
        }

        // Modification of the range limitation control allowing the final angle to exceed of an EPSILON value the limitation.
        if (finalAngle < minAngle && Math.abs(finalAngle - minAngle) > EPSILON || finalAngle > maxAngle && Math.abs(finalAngle - maxAngle) > EPSILON) {
            throw new FaraoException(String.format("Angle value %.4f not is the range of minimum and maximum angle values [%.4f,%.4f] of the phase tap changer %s steps", finalAngle, minAngle, maxAngle, networkElement.getId()));
        }
        AtomicReference<Double> angleDifference = new AtomicReference<>(Double.MAX_VALUE);
        AtomicInteger approximatedTapPosition = new AtomicInteger(phaseTapChanger.getTapPosition());
        steps.forEach((tapPosition, step) -> {
            double diff = Math.abs(step.getAlpha() - finalAngle);
            if (diff < angleDifference.get()) {
                angleDifference.set(diff);
                approximatedTapPosition.set(tapPosition);
            }
        });
        return approximatedTapPosition.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
