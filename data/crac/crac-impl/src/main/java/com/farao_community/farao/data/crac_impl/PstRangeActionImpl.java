/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.Range;
import com.farao_community.farao.data.crac_api.range_action.TapRange;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Elementary PST range remedial action.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class PstRangeActionImpl extends AbstractRangeAction implements PstRangeAction {

    private List<TapRange> ranges;
    private NetworkElement networkElement;

    private int lowTapPosition; // min value of PST in the Network (with implicit TapConvention)
    private int highTapPosition; // max value of PST in the Network (with implicit TapConvention)
    private int initialTapPosition;

    private PhaseTapChanger phaseTapChangerInCache;
    private boolean isSynchronized;

    private static final double EPSILON = 1e-3;

    PstRangeActionImpl(String id, String name, String operator, List<UsageRule> usageRules, List<TapRange> ranges,
                              NetworkElement networkElement, String groupId, int lowestFeasibleTap, int highestFeasibleTap, int initialTap) {
        super(id, name, operator, usageRules, groupId);
        this.networkElement = networkElement;
        this.ranges = ranges;
        this.lowTapPosition = lowestFeasibleTap;
        this.highTapPosition = highestFeasibleTap;
        this.initialTapPosition = initialTap;
    }

    @Deprecated
    // TODO : delete
    public PstRangeActionImpl(String id, String name, String operator, List<UsageRule> usageRules, List<TapRange> ranges,
                        NetworkElement networkElement, String groupId) {
        super(id, name, operator, usageRules, groupId);
        this.networkElement = networkElement;
        this.ranges = ranges;
    }

    @Deprecated
    // TODO : delete
    public PstRangeActionImpl(String id, String name, String operator, List<UsageRule> usageRules, List<TapRange> ranges, NetworkElement networkElement) {
        super(id, name, operator, usageRules);
        this.networkElement = networkElement;
        this.ranges = ranges;
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public List<TapRange> getRanges() {
        return ranges;
    }

    @Override
    public int getLowestFeasibleTap() {
        return lowTapPosition;
    }

    @Override
    public int getHighestFeasibleTap() {
        return highTapPosition;
    }

    @Override
    public int getInitialTap() {
        return initialTapPosition;
    }

    @Override
    public Map<Integer, Double> getTapToAngleConversionMap() {
        return null;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public void synchronize(Network network) {
        if (isSynchronized()) {
            throw new AlreadySynchronizedException(String.format("PST %s has already been synchronized", getId()));
        }
        /*
        phaseTapChanger = checkValidPstAndGetPhaseTapChanger(network);
        initialTapPosition = phaseTapChanger.getTapPosition();
        lowTapPosition = phaseTapChanger.getLowTapPosition();
        highTapPosition = phaseTapChanger.getHighTapPosition();
        isSynchronized = true;
        */
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
    public double getMinValue(double previousInstantValue, Network network) {

        double minValue = Math.min(convertTapToAngle(lowTapPosition, network), convertTapToAngle(highTapPosition, network));
        for (TapRange range: ranges) {
            minValue = Math.max(getMinValueWithRange(range, previousInstantValue, network), minValue);
        }
        return minValue;
    }

    /**
     * Max angle value allowed by all ranges and the physical limitations of the PST itself
     */
    @Override
    public double getMaxValue(double previousInstantValue, Network network) {

        double maxValue = Math.max(convertTapToAngle(lowTapPosition, network), convertTapToAngle(highTapPosition, network));
        for (TapRange range: ranges) {
            maxValue = Math.min(getMaxValueWithRange(range, previousInstantValue, network), maxValue);
        }
        return maxValue;
    }

    @Override
    public void apply(Network network, double targetAngle) {
        PhaseTapChanger phaseTapChanger = getPhaseTapChanger(network);
        int setpoint = convertAngleToTap(targetAngle, phaseTapChanger);
        phaseTapChanger.setTapPosition(setpoint);
    }

    @Override
    public double getMinValue(double previousInstantValue) {
        return 0;
    }

    @Override
    public double getMaxValue(double previousInstantValue) {
        return 0;
    }

    @Override
    public double getCurrentValue(Network network) {
        /*
        the phaseTapChanger is re-initiated to ensure that the current position is taken on the
        network given in argument of this method
         */
        this.phaseTapChangerInCache = null;
        return convertTapToAngle(getCurrentTapPosition(network), network);
    }

    @Override
    public int getCurrentTapPosition(Network network, TapConvention requestedRangeDefinition) {
        /*
        the phaseTapChanger is re-initiated to ensure that the current position is taken on the
        network given in argument of this method
         */
        this.phaseTapChangerInCache = null;
        switch (requestedRangeDefinition) {
            case STARTS_AT_ONE:
                return convertToStartsAtOne(getPhaseTapChanger(network).getTapPosition());
            case CENTERED_ON_ZERO:
                return convertToCenteredOnZero(getPhaseTapChanger(network).getTapPosition());
            default:
                throw new FaraoException("Unknown range definition");
        }
    }

    @Override
    public double convertTapToAngle(int tap) {
        return 0;
    }

    @Override
    public int convertAngleToTap(double angle) {
        return 0;
    }

    @Override
    public double convertTapToAngle(int tap, Network network) {
        return getPhaseTapChanger(network).getStep(tap).getAlpha();
    }

    @Override
    public int convertAngleToTap(double angle, Network network) {
        return convertAngleToTap(angle, getPhaseTapChanger(network));
    }

    // Min value allowed by a range (from Crac)
    private double getMinValueWithRange(TapRange range, double prePerimeterValue, Network network) {
        int prePerimeterTapPosition = convertAngleToTap(prePerimeterValue, network);
        return Math.min(lowTapPositionRangeIntersection(prePerimeterTapPosition, range, network), highTapPositionRangeIntersection(prePerimeterTapPosition, range, network));
    }

    // Max value allowed by a range (from Crac)
    private double getMaxValueWithRange(TapRange range, double prePerimeterValue, Network network) {
        int prePerimeterTapPosition = convertAngleToTap(prePerimeterValue, network);
        return Math.max(lowTapPositionRangeIntersection(prePerimeterTapPosition, range, network), highTapPositionRangeIntersection(prePerimeterTapPosition, range, network));
    }

    /**
    Maximum value between lowTapPosition and lower range bound
     */
    private double lowTapPositionRangeIntersection(int prePerimeterTapPosition, TapRange range, Network network) {
        int minTap = Math.max(lowTapPosition, (int) getExtremumValueWithRange(range, prePerimeterTapPosition, range.getMinTap()));
        return convertTapToAngle(minTap, network);
    }

    /**
     Minimum value between highTapPosition and upper range bound
     */
    private double highTapPositionRangeIntersection(int prePerimeterTapPosition, TapRange range,  Network network) {
        int maxTap = Math.min(highTapPosition, (int) getExtremumValueWithRange(range, prePerimeterTapPosition, range.getMaxTap()));
        return convertTapToAngle(maxTap, network);
    }

    private int getCurrentTapPosition(Network network) {
        this.phaseTapChangerInCache = null;
        return getPhaseTapChanger(network).getTapPosition();
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

    private double getExtremumValueWithRange(Range range, double prePerimeterTapPosition, double extremumValue) {
        TapRange pstRange = (TapRange) range;
        switch (pstRange.getRangeType()) {
            case ABSOLUTE:
                switch (pstRange.getTapConvention()) {
                    case STARTS_AT_ONE:
                        return lowTapPosition + extremumValue - 1;
                    case CENTERED_ON_ZERO:
                        return ((double) lowTapPosition + highTapPosition) / 2 + extremumValue;
                    default:
                        throw new FaraoException("Unknown range definition");
                }
            case RELATIVE_TO_INITIAL_NETWORK:
                return initialTapPosition + extremumValue;
            case RELATIVE_TO_PREVIOUS_INSTANT:
                return prePerimeterTapPosition + extremumValue;
            default:
                throw new FaraoException("Unknown range type");
        }
    }

    private PhaseTapChanger checkValidPstAndGetPhaseTapChanger(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElement.getId());
        if (transformer == null) {
            throw new FaraoException(String.format("PST %s does not exist in the current network", networkElement.getId()));
        }
        PhaseTapChanger phaseTapChangerFromNetwork = transformer.getPhaseTapChanger();
        if (phaseTapChangerFromNetwork == null) {
            throw new FaraoException(String.format("Transformer %s is not a PST but is defined as a TapRange", networkElement.getId()));
        }
        return phaseTapChangerFromNetwork;
    }

    private int convertAngleToTap(double angle, PhaseTapChanger phaseTapChanger) {
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
        if (angle < minAngle && Math.abs(angle - minAngle) > EPSILON || angle > maxAngle && Math.abs(angle - maxAngle) > EPSILON) {
            throw new FaraoException(String.format("Angle value %.4f not is the range of minimum and maximum angle values [%.4f,%.4f] of the phase tap changer %s steps", angle, minAngle, maxAngle, networkElement.getId()));
        }
        AtomicReference<Double> angleDifference = new AtomicReference<>(Double.MAX_VALUE);
        AtomicInteger approximatedTapPosition = new AtomicInteger(phaseTapChanger.getTapPosition());
        steps.forEach((tapPosition, step) -> {
            double diff = Math.abs(step.getAlpha() - angle);
            if (diff < angleDifference.get()) {
                angleDifference.set(diff);
                approximatedTapPosition.set(tapPosition);
            }
        });
        return approximatedTapPosition.get();
    }

    @Deprecated
    //todo: delete
    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
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
        //todo add parameters specific to PST here
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Deprecated
    //todo: delete
    public void addRange(TapRange range) {
        ranges.add(range);
    }

    private PhaseTapChanger getPhaseTapChanger(Network network) {
        if (Objects.isNull(phaseTapChangerInCache)) {
            this.phaseTapChangerInCache = checkValidPstAndGetPhaseTapChanger(network);
        }
        return phaseTapChangerInCache;
    }
}
