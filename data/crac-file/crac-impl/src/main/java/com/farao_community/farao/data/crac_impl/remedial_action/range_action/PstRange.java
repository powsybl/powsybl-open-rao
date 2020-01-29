/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.List;

/**
 * Elementary PST range remedial action: choose the optimal value for a PST tap.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public final class PstRange extends AbstractNetworkElementRangeAction {

    /**
     * Constructor of a remedial action on a PST. The value of the tap to set will be specify at the application.
     *
     * @param networkElement: PST element to modify
     */
    @JsonCreator
    public PstRange(@JsonProperty("id") String id,
                    @JsonProperty("name") String name,
                    @JsonProperty("operator") String operator,
                    @JsonProperty("usageRules") List<UsageRule> usageRules,
                    @JsonProperty("ranges") List<Range> ranges,
                    @JsonProperty("networkElement") NetworkElement networkElement) {
        super(id, name, operator, usageRules, ranges, networkElement);
    }

    public PstRange(String id,
                    NetworkElement networkElement) {
        super(id, networkElement);
    }

    @Override
    public void synchronize(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElement.getId());
        PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
        int highTapPosition = phaseTapChanger.getHighTapPosition();
        int lowTapPosition = phaseTapChanger.getLowTapPosition();
        RangeDefinition rangeDefinition;
        if (lowTapPosition <= 0) {
            rangeDefinition = RangeDefinition.CENTERED_ON_ZERO;
        } else {
            rangeDefinition = RangeDefinition.STARTS_AT_ONE;
        }
        Range absoluteFixedRange = new Range(lowTapPosition, highTapPosition, RangeType.ABSOLUTE_FIXED, rangeDefinition);
        addRange(absoluteFixedRange);
    }

    @Override
    protected double getMinValueWithRange(Network network, Range range) {
        double minValue = range.getMin();
        return getExtremumValueWithRange(network, range, minValue);
    }

    @Override
    public double getMaxValueWithRange(Network network, Range range) {
        double maxValue = range.getMax();
        return getExtremumValueWithRange(network, range, maxValue);
    }

    private double getExtremumValueWithRange(Network network, Range range, double extremumValue) {
        switch (range.getRangeType()) {
            case ABSOLUTE_FIXED:
                return extremumValue;
            case RELATIVE_FIXED:
                // TODO: clarify the sign convention of relative fixed range
                return getCurrentTapPosition(network) - extremumValue;
            case RELATIVE_DYNAMIC:
                throw new FaraoException("RelativeDynamicRanges are not handled for the moment");
            default:
                throw new FaraoException("Invalid range given in argument");
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

    public int getCurrentTapPosition(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElement.getId());
        PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
        return phaseTapChanger.getTapPosition();
    }
}
