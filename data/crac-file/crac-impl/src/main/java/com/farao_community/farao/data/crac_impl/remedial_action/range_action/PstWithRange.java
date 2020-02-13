/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.List;

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
    private int currentTapPosition;

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
        lowTapPosition = (int) Double.NaN;
        highTapPosition = (int) Double.NaN;
        initialTapPosition = (int) Double.NaN;
        currentTapPosition = (int) Double.NaN;
    }

    public PstWithRange(String id, String name, String operator, NetworkElement networkElement) {
        super(id, name, operator, networkElement);
        lowTapPosition = (int) Double.NaN;
        highTapPosition = (int) Double.NaN;
        initialTapPosition = (int) Double.NaN;
        currentTapPosition = (int) Double.NaN;
    }

    public PstWithRange(String id, NetworkElement networkElement) {
        super(id, networkElement);
        lowTapPosition = (int) Double.NaN;
        highTapPosition = (int) Double.NaN;
        initialTapPosition = (int) Double.NaN;
        currentTapPosition = (int) Double.NaN;
    }

    @Override
    public void synchronize(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElement.getId());
        PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
        currentTapPosition = phaseTapChanger.getTapPosition();
    }

    @Override
    public void desynchronize() {
        currentTapPosition = (int) Double.NaN;
    }

    public void setReferenceValue(Network network) {
        synchronize(network);
        initialTapPosition = currentTapPosition;
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(networkElement.getId()).getPhaseTapChanger();
        lowTapPosition = phaseTapChanger.getLowTapPosition();
        highTapPosition = phaseTapChanger.getHighTapPosition();
        Range physicalRange = new Range(lowTapPosition,
                highTapPosition,
                RangeType.ABSOLUTE_FIXED,
                RangeDefinition.CENTERED_ON_ZERO);
        addRange(physicalRange);
    }

    @Override
    protected double getMinValueWithRange(Network network, Range range) {
        double minValue = range.getMin();
        return convertTapToAngle(network, (int) getExtremumValueWithRange(range, minValue));
    }

    @Override
    public double getMaxValueWithRange(Network network, Range range) {
        double maxValue = range.getMax();
        return convertTapToAngle(network, (int) getExtremumValueWithRange(range, maxValue));
    }

    @Override
    public double getMaxNegativeVariation(Network network) {
        return Math.abs(convertTapToAngle(network, (int) getMinValue(network)) - convertTapToAngle(network, getCurrentTapPosition(network)));
    }

    @Override
    public double getMaxPositiveVariation(Network network) {
        return Math.abs(convertTapToAngle(network, (int) getMaxValue(network))  - convertTapToAngle(network, getCurrentTapPosition(network)));
    }

    private int getCurrentTapPosition(Network network) {
        return network.getTwoWindingsTransformer(networkElement.getId()).getPhaseTapChanger().getTapPosition();
    }

    private double convertTapToAngle(Network network, int tap) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElement.getId());
        return transformer.getPhaseTapChanger().getStep(tap).getAlpha();
    }

    private double getExtremumValueWithRange(Range range, double extremumValue) {
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
     * @param setpoint: tap value to set on the PST. Even if defined as a double here the setpoint
     *                is a tap value so it's an int, if not it will be truncated.
     */
    @Override
    public void apply(Network network, double setpoint) {
        // TODO : check that the exception is already thrown by Powsybl
        PhaseTapChanger phaseTapChanger = checkValidPstAndGetPhaseTapChanger(network);
        if (phaseTapChanger.getHighTapPosition() - phaseTapChanger.getLowTapPosition() + 1 >= setpoint && setpoint >= 1) {
            phaseTapChanger.setTapPosition((int) setpoint + phaseTapChanger.getLowTapPosition() - 1);
        } else {
            throw new FaraoException("PST cannot be set because setpoint is out of PST boundaries");
        }
    }

    private PhaseTapChanger checkValidPstAndGetPhaseTapChanger(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(getNetworkElement().getId());
        if (transformer == null) {
            throw new FaraoException(String.format("PST %s does not exist in the current network", networkElement.getId()));
        }
        PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
        if (phaseTapChanger == null) {
            throw new FaraoException(String.format("Transformer %s is not a PST but is defined as a PstRange", networkElement.getId()));
        }
        return phaseTapChanger;
    }
}
