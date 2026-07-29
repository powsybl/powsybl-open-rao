/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.PhaseTapChangerTapPositionActionBuilder;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.TapRange;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Elementary PST range remedial action.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class PstRangeActionImpl extends AbstractRangeAction<PstRangeAction> implements PstRangeAction {
    // Note : Ranges of type RELATIVE_TO_PREVIOUS_TIME_STEP are not taken into account
    private static final double EPSILON = 1e-3;

    private final NetworkElement networkElement;
    private final List<TapRange> ranges;
    private final int initialTapPosition;
    private final Map<Integer, Double> tapToAngleConversionMap;
    private final double smallestAngleStep;
    private final int lowTapPosition;
    private final int highTapPosition;

    PstRangeActionImpl(String id,
                       String name,
                       String operator,
                       Set<UsageRule> usageRules,
                       List<TapRange> ranges,
                       NetworkElement networkElement,
                       String groupId,
                       int initialTap,
                       Map<Integer, Double> tapToAngleConversionMap,
                       Integer speed,
                       Double activationCost,
                       Map<VariationDirection, Double> variationCosts) {
        super(id, name, operator, usageRules, groupId, speed, activationCost, variationCosts);
        this.networkElement = networkElement;
        this.ranges = ranges;
        this.initialTapPosition = initialTap;
        this.tapToAngleConversionMap = tapToAngleConversionMap;
        this.lowTapPosition = Collections.min(tapToAngleConversionMap.keySet());
        this.highTapPosition = Collections.max(tapToAngleConversionMap.keySet());
        this.smallestAngleStep = computeSmallestAngleStep();
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

    private double computeSmallestAngleStep() {
        double smallestDiff = Double.POSITIVE_INFINITY;
        for (int i = lowTapPosition; i < highTapPosition; i++) {
            double absoluteDiff = Math.abs(tapToAngleConversionMap.get(i + 1) - tapToAngleConversionMap.get(i));
            smallestDiff = Math.min(smallestDiff, absoluteDiff);
        }
        return smallestDiff;
    }

    @Override
    public double getSmallestAngleStep() {
        return smallestAngleStep;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
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
        int tap = convertAngleToTap(targetAngle);
        new PhaseTapChangerTapPositionActionBuilder()
            .withId("id")
            .withNetworkElementId(networkElement.getId())
            .withTapPosition(tap)
            .withRelativeValue(false)
            .build()
            .toModification()
            .apply(network, true, ReportNode.NO_OP);
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
            throw new OpenRaoException(String.format("Pst of Range Action %s does not have a tap %d", getId(), tap));
        }
    }

    @Override
    public int convertAngleToTap(double angle) {
        checkAngle(angle);
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

    @Override
    public void checkAngle(double angle) {
        double minAngle = Collections.min(tapToAngleConversionMap.values());
        double maxAngle = Collections.max(tapToAngleConversionMap.values());

        // Modification of the range limitation control allowing the final angle to exceed of an EPSILON value the limitation.
        if (angle < minAngle && Math.abs(angle - minAngle) > EPSILON || angle > maxAngle && Math.abs(angle - maxAngle) > EPSILON) {
            throw new OpenRaoException(String.format(Locale.ENGLISH,
                "Angle value %.4f is not in the range of minimum and maximum angle values [%.4f;%.4f] of the phase tap changer %s steps",
                angle, minAngle, maxAngle, networkElement.getId()
            ));
        }
    }

    private Pair<Integer, Integer> getMinAndMaxTaps(double previousInstantSetPoint) {
        int minTap = lowTapPosition;
        int maxTap = highTapPosition;
        int previousInstantTap = convertAngleToTap(previousInstantSetPoint);

        for (TapRange range : ranges) {
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
                throw new OpenRaoException(String.format("Unknown Range Type %s", initialRangeType));
        }
    }

    private PhaseTapChanger getPhaseTapChanger(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElement.getId());
        if (transformer == null) {
            throw new OpenRaoException(String.format("PST %s does not exist in the current network", networkElement.getId()));
        }
        PhaseTapChanger phaseTapChangerFromNetwork = transformer.getPhaseTapChanger();
        if (phaseTapChangerFromNetwork == null) {
            throw new OpenRaoException(String.format("Transformer %s is not a PST but is defined as a TapRange", networkElement.getId()));
        }
        return phaseTapChangerFromNetwork;
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
}
