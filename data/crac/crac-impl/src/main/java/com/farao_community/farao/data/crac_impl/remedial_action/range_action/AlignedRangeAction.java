/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.fasterxml.jackson.annotation.*;
import com.powsybl.iidm.network.Network;

import com.powsybl.sensitivity.SensitivityComputationResults;

import java.util.*;


/**
 * Range remedial action with several {@link NetworkElement}s sharing common characteristics:
 * id, name, operator, {@link UsageRule}s and {@link Range}s.
 * This class is not fully implemented. In the end it will be an abstract class and we will have children classes
 * such as AlignedPstRange and AlignedHvdcRange. For the moment we keep it a concrete class in order to be able to
 * test the methods we already implemented.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("aligned-range-action")
public class AlignedRangeAction extends AbstractRemedialAction<RangeAction> implements RangeAction {

    public static final int TEMP_VALUE_ARA = 0;

    @JsonProperty("ranges")
    private List<Range> ranges;
    @JsonProperty("networkElements")
    private Set<NetworkElement> networkElements;

    @JsonCreator
    public AlignedRangeAction(@JsonProperty("id") String id,
                              @JsonProperty("name") String name,
                              @JsonProperty("operator") String operator,
                              @JsonProperty("usageRules") List<UsageRule> usageRules,
                              @JsonProperty("ranges") List<Range> ranges,
                              @JsonProperty("networkElements") Set<NetworkElement> networkElements) {
        super(id, name, operator, usageRules);
        this.ranges = new ArrayList<>(ranges);
        this.networkElements = new HashSet<>(networkElements);
    }

    public AlignedRangeAction(String id, String name, String operator, List<Range> ranges, Set<NetworkElement> networkElements) {
        super(id, name, operator);
        this.ranges = ranges;
        this.networkElements = new HashSet<>(networkElements);
    }

    public AlignedRangeAction(String id, String name, String operator, Set<NetworkElement> networkElements) {
        this(id, name, operator, new ArrayList<>(), networkElements);
    }

    public AlignedRangeAction(String id) {
        super(id);
        this.ranges = new ArrayList<>();
        this.networkElements = new HashSet<>();
    }

    public final List<Range> getRanges() {
        return ranges;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return networkElements;
    }

    @Override
    public double getMinValue(Network network) {
        return ranges.stream().map(Range::getMin).max(Double::compareTo).orElseThrow(FaraoException::new);
    }

    @Override
    public double getMaxValue(Network network) {
        return ranges.stream().map(Range::getMax).min(Double::compareTo).orElseThrow(FaraoException::new);
    }

    @Override
    public double getMaxNegativeVariation(Network network) {
        // to implement
        return TEMP_VALUE_ARA;
    }

    @Override
    public double getMaxPositiveVariation(Network network) {
        // to implement
        return TEMP_VALUE_ARA;
    }

    @Override
    public double getSensitivityValue(SensitivityComputationResults sensitivityComputationResults, Cnec cnec) {
        // to implement
        return TEMP_VALUE_ARA;
    }

    @Override
    public void apply(Network network, double setpoint) {
        // to implement
    }

    public void addRange(Range range) {
        this.ranges.add(range);
    }

    public void addNetworkElement(NetworkElement networkElement) {
        this.networkElements.add(networkElement);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AlignedRangeAction otherAlignedRangeAction = (AlignedRangeAction) o;
        return super.equals(o)
                && new HashSet<>(ranges).equals(new HashSet<>(otherAlignedRangeAction.ranges))
                && networkElements.equals(otherAlignedRangeAction.networkElements);
    }

    @Override
    public int hashCode() {
        return String.format("%s%d%d", getId(), ranges.size(), networkElements.size()).hashCode();
    }

    @Override
    public void synchronize(Network network) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void desynchronize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSynchronized() {
        throw new UnsupportedOperationException();
    }
}
