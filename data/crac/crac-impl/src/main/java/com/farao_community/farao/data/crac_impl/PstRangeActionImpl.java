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
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.range_action.TapRange;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Elementary PST range remedial action.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class PstRangeActionImpl extends AbstractRangeAction implements PstRangeAction {

    private static final double EPSILON = 1e-3;

    private NetworkElement networkElement;
    private List<TapRange> ranges;
    private int initialTapPosition;
    private Map<Integer, Double> tapToAngleConversionMap;
    private int lowTapPosition;
    private int highTapPosition;

    PstRangeActionImpl(String id, String name, String operator, List<UsageRule> usageRules, List<TapRange> ranges,
                              NetworkElement networkElement, String groupId, int initialTap, Map<Integer, Double> tapToAngleConversionMap) {
        super(id, name, operator, usageRules, groupId);
        this.networkElement = networkElement;
        this.ranges = ranges;
        this.initialTapPosition = initialTap;
        this.tapToAngleConversionMap = tapToAngleConversionMap;
        this.lowTapPosition = Collections.min(tapToAngleConversionMap.keySet());
        this.highTapPosition = Collections.max(tapToAngleConversionMap.keySet());
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
    public int getInitialTap() {
        return initialTapPosition;
    }

    @Override
    public Map<Integer, Double> getTapToAngleConversionMap() {
        return tapToAngleConversionMap;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public void synchronize(Network network) {
    }

    @Override
    public void desynchronize() {
    }

    @Override
    public boolean isSynchronized() {
        return false;
    }

    /**
     * Min angle value allowed by all ranges and the physical limitations of the PST itself
     */
    @Override
    public double getMinAdmissibleSetpoint(double previousInstantSetPoint) {
        Pair<Integer, Integer> minAndMaxTaps = getMinAndMaxTaps(previousInstantSetPoint);
        return Math.min(convertTapToAngle(minAndMaxTaps.getLeft()), convertTapToAngle(minAndMaxTaps.getRight()));
    }

    /**
     * Max angle value allowed by all ranges and the physical limitations of the PST itself
     */
    @Override
    public double getMaxAdmissibleSetpoint(double previousInstantSetPoint) {
        Pair<Integer, Integer> minAndMaxTaps = getMinAndMaxTaps(previousInstantSetPoint);
        return Math.max(convertTapToAngle(minAndMaxTaps.getLeft()), convertTapToAngle(minAndMaxTaps.getRight()));
    }

    @Override
    public void apply(Network network, double targetAngle) {
        PhaseTapChanger phaseTapChanger = getPhaseTapChanger(network);
        int tap = convertAngleToTap(targetAngle);
        phaseTapChanger.setTapPosition(tap);
    }

    @Override
    public double getCurrentSetpoint(Network network) {
        return convertTapToAngle(getPhaseTapChanger(network).getTapPosition());
    }

    @Override
    public int getCurrentTapPosition(Network network) {
        return getPhaseTapChanger(network).getTapPosition();
    }

    @Override
    public double convertTapToAngle(int tap) {
        if (tapToAngleConversionMap.containsKey(tap)) {
            return tapToAngleConversionMap.get(tap);
        } else {
            throw new FaraoException(String.format("Pst of Range Action %s does not have a tap %d", getId(), tap));
        }
    }

    @Override
    public int convertAngleToTap(double angle) {

        double minAngle = Collections.min(tapToAngleConversionMap.values());
        double maxAngle = Collections.max(tapToAngleConversionMap.values());

        // Modification of the range limitation control allowing the final angle to exceed of an EPSILON value the limitation.
        if (angle < minAngle && Math.abs(angle - minAngle) > EPSILON || angle > maxAngle && Math.abs(angle - maxAngle) > EPSILON) {
            throw new FaraoException(String.format("Angle value %.4f not is the range of minimum and maximum angle values [%.4f,%.4f] of the phase tap changer %s steps", angle, minAngle, maxAngle, networkElement.getId()));
        }

        AtomicReference<Double> smallestAngleDifference = new AtomicReference<>(Double.MAX_VALUE);
        AtomicInteger approximatedTapPosition = new AtomicInteger(0);

        tapToAngleConversionMap.forEach((tap, alpha) -> {
            double diff = Math.abs(alpha - angle);
            if (diff < smallestAngleDifference.get()) {
                smallestAngleDifference.set(diff);
                approximatedTapPosition.set(tap);
            }
        });
        return approximatedTapPosition.get();
    }

    private Pair<Integer, Integer> getMinAndMaxTaps(double previousInstantSetPoint) {
        int minTap = lowTapPosition;
        int maxTap = highTapPosition;
        int previousInstantTap = convertAngleToTap(previousInstantSetPoint);

        for (TapRange range: ranges) {
            minTap = Math.max(minTap, getRangeMinTapAsAbsoluteCenteredOnZero(range, previousInstantTap));
            maxTap = Math.min(maxTap, getRangeMaxTapAsAbsoluteCenteredOnZero(range, previousInstantTap));
        }

        return Pair.of(minTap, maxTap);
    }

    private int getRangeMinTapAsAbsoluteCenteredOnZero(TapRange range, int previousInstantTap) {
        return convertTapToAbsoluteCenteredOnZero(range.getMinTap(), range.getRangeType(), previousInstantTap);

    }

    private int getRangeMaxTapAsAbsoluteCenteredOnZero(TapRange range, int previousInstantTap) {
        return convertTapToAbsoluteCenteredOnZero(range.getMaxTap(), range.getRangeType(), previousInstantTap);
    }

    private int convertTapToAbsoluteCenteredOnZero(int tap, RangeType initialRangeType, int prePerimeterTapPosition) {

        switch (initialRangeType) {
            case ABSOLUTE:
                return tap;
            case RELATIVE_TO_INITIAL_NETWORK:
                return initialTapPosition + tap;
            case RELATIVE_TO_PREVIOUS_INSTANT:
                return prePerimeterTapPosition + tap;
            default:
                throw new FaraoException(String.format("Unknown Range Type %s", initialRangeType));
        }
    }

    private PhaseTapChanger getPhaseTapChanger(Network network) {
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
        if (!super.equals(o)) {
            return false;
        }

        return this.networkElement.equals(((PstRangeAction) o).getNetworkElement())
            && this.ranges.equals(((PstRangeAction) o).getRanges())
            && this.tapToAngleConversionMap.equals(((PstRangeAction) o).getTapToAngleConversionMap())
            && this.initialTapPosition == ((PstRangeAction) o).getInitialTap();
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        for (TapRange range : ranges) {
            hashCode += 31 * range.hashCode();
        }
        hashCode += 31 * initialTapPosition;
        hashCode += 31 * networkElement.hashCode();
        return hashCode;
    }

    @Deprecated
    //todo: delete
    public void addRange(TapRange range) {
        ranges.add(range);
    }
}
