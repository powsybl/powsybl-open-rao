/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
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
import com.powsybl.iidm.network.*;

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
public final class PstWithRange extends AbstractElementaryRangeAction<PstRange> implements PstRange {
    private int lowTapPosition; // min value of PST in the Network (with implicit RangeDefinition)
    private int highTapPosition; // max value of PST in the Network (with implicit RangeDefinition)
    private int initialTapPosition;

    private boolean isSynchronized;
    private PhaseTapChanger phaseTapChanger;

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
        phaseTapChanger = null;
    }

    @Override
    public void synchronize(Network network) {
        if (isSynchronized()) {
            throw new AlreadySynchronizedException(String.format("PST %s has already been synchronized", getId()));
        }
        phaseTapChanger = checkValidPstAndGetPhaseTapChanger(network);
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

    /**
     * Min angle value allowed by all ranges and the physical limitations of the PST itself
     */
    @Override
    public double getMinValue(Network network) {
        if (!isSynchronized) {
            throw new NotSynchronizedException(String.format("PST %s have not been synchronized so its min value cannot be accessed", getId()));
        }
        double minValue = convertTapToAngle(lowTapPosition);
        for (Range range: ranges) {
            minValue = Math.max(getMinValueWithRange(network, range), minValue);
        }
        return minValue;
    }

    /**
     * Max angle value allowed by all ranges and the physical limitations of the PST itself
     */
    @Override
    public double getMaxValue(Network network) {
        if (!isSynchronized) {
            throw new NotSynchronizedException(String.format("PST %s have not been synchronized so its max value cannot be accessed", getId()));
        }
        double maxValue = convertTapToAngle(highTapPosition);
        for (Range range: ranges) {
            maxValue = Math.min(getMaxValueWithRange(network, range), maxValue);
        }
        return maxValue;
    }

    // Min value allowed by a range (from Crac)
    @Override
    protected double getMinValueWithRange(Network network, Range range) {
        double minValue = range.getMin();
        return convertTapToAngle(Math.max(lowTapPosition, (int) getExtremumValueWithRange(range, getCurrentTapPosition(network), minValue)));
    }

    @Override
    protected double getMaxValueWithRange(Network network, Range range) {
        double maxValue = range.getMax();
        return convertTapToAngle(Math.min(highTapPosition, (int) getExtremumValueWithRange(range, getCurrentTapPosition(network), maxValue)));
    }

    @Override
    public double getCurrentValue(Network network) {
        return convertTapToAngle(getCurrentTapPosition(network));
    }

    private int getCurrentTapPosition(Network network) {
        return checkValidPstAndGetPhaseTapChanger(network).getTapPosition();
    }

    @Override
    public int getCurrentTapPosition(Network network, RangeDefinition requestedRangeDefinition) {
        switch (requestedRangeDefinition) {
            case STARTS_AT_ONE:
                return convertToStartsAtOne(getCurrentTapPosition(network));
            case CENTERED_ON_ZERO:
                return convertToCenteredOnZero(getCurrentTapPosition(network));
            default:
                throw new FaraoException("Unknown range definition");
        }
    }

    /**
     * Conversion from any (implicit) to STARTS_AT_ONE
     */
    private int convertToStartsAtOne(int tap) {
        if (highTapPosition == -lowTapPosition) { // the tap is CENTERED_ON_ZERO in the network
            return tap + highTapPosition + 1;
        } else if (lowTapPosition == 1) { // the tap STARTS_AT_ONE in the network
            return tap;
        } else {
            throw new FaraoException(String.format("Unhandled range definition, between %s and %s.", lowTapPosition, highTapPosition));
        }
    }

    /**
     * Conversion from any (implicit) to CENTERED_ON_ZERO
     */
    private int convertToCenteredOnZero(int tap) {
        if (lowTapPosition == -highTapPosition) { // the tap is CENTERED_ON_ZERO in the network
            return tap;
        } else if (lowTapPosition == 1) { // the tap STARTS_AT_ONE in the network
            return tap - (int) Math.ceil(((double) highTapPosition + 1) / 2);
        } else {
            throw new FaraoException(String.format("Unhandled range definition, between %s and %s.", lowTapPosition, highTapPosition));
        }
    }

    private double convertTapToAngle(int tap) {
        if (!isSynchronized()) {
            throw new NotSynchronizedException(String.format("PST %s have not been synchronized so tap cannot be converted to angle", getId()));
        }
        return phaseTapChanger.getStep(tap).getAlpha();
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
        PhaseTapChanger phaseTapChangerFromNetwork = checkValidPstAndGetPhaseTapChanger(network);
        int setpoint = computeTapPosition(finalAngle, phaseTapChangerFromNetwork);
        phaseTapChangerFromNetwork.setTapPosition(setpoint);
    }

    private PhaseTapChanger checkValidPstAndGetPhaseTapChanger(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElement.getId());
        if (transformer == null) {
            throw new FaraoException(String.format("PST %s does not exist in the current network", networkElement.getId()));
        }
        PhaseTapChanger phaseTapChangerFromNetwork = transformer.getPhaseTapChanger();
        if (phaseTapChangerFromNetwork == null) {
            throw new FaraoException(String.format("Transformer %s is not a PST but is defined as a PstRange", networkElement.getId()));
        }
        return phaseTapChangerFromNetwork;
    }

    @Override
    public int computeTapPosition(double finalAngle) {
        if (!isSynchronized()) {
            throw new NotSynchronizedException(String.format("PST %s have not been synchronized so tap cannot be computed from angle", getId()));
        }
        return computeTapPosition(finalAngle, phaseTapChanger);
    }

    private int computeTapPosition(double finalAngle, PhaseTapChanger phaseTapChanger) {
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
