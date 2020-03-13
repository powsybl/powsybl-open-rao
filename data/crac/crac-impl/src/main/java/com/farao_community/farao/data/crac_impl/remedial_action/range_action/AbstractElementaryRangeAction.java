/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityValue;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic object to define any simple range action on a network element
 * (HVDC line, PST, injection, redispatching, etc.).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PstWithRange.class, name = "pst-with-range"),
        @JsonSubTypes.Type(value = HvdcRange.class, name = "hvdc-range"),
        @JsonSubTypes.Type(value = InjectionRange.class, name = "injection-range"),
        @JsonSubTypes.Type(value = Redispatching.class, name = "redispatching")
    })
public abstract class AbstractElementaryRangeAction<I extends RangeAction<I>> extends AbstractRangeAction<I> implements RangeAction<I> {
    protected NetworkElement networkElement;

    @JsonCreator
    public AbstractElementaryRangeAction(@JsonProperty("id") String id,
                                         @JsonProperty("name") String name,
                                         @JsonProperty("operator") String operator,
                                         @JsonProperty("usageRules") List<UsageRule> usageRules,
                                         @JsonProperty("ranges") List<Range> ranges,
                                         @JsonProperty("networkElement") NetworkElement networkElement) {
        super(id, name, operator, usageRules, ranges);
        this.networkElement = networkElement;
    }

    public AbstractElementaryRangeAction(String id, String name, String operator, NetworkElement networkElement) {
        super(id, name, operator);
        this.networkElement = networkElement;
    }

    public AbstractElementaryRangeAction(String id, NetworkElement networkElement) {
        super(id);
        this.networkElement = networkElement;
    }

    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
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

    protected abstract double getMinValueWithRange(Network network, Range range);

    @Override
    public double getMinValue(Network network) {
        double minValue = Double.NEGATIVE_INFINITY;
        for (Range range: ranges) {
            minValue = Math.max(getMinValueWithRange(network, range), minValue);
        }
        return minValue;
    }

    protected abstract double getMaxValueWithRange(Network network, Range range);

    @Override
    public double getMaxValue(Network network) {
        double maxValue = Double.POSITIVE_INFINITY;
        for (Range range: ranges) {
            maxValue = Math.min(getMaxValueWithRange(network, range), maxValue);
        }
        return maxValue;
    }

    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public double getSensitivityValue(SensitivityComputationResults sensitivityComputationResults, Cnec cnec) {
        List<SensitivityValue> sensitivityValueStream = sensitivityComputationResults.getSensitivityValues().stream()
            .filter(sensitivityValue -> sensitivityValue.getFactor().getVariable().getId().equals(networkElement.getId()))
            .filter(sensitivityValue -> sensitivityValue.getFactor().getFunction().getId().equals(cnec.getId()))
            .collect(Collectors.toList());

        if (sensitivityValueStream.size() > 1) {
            throw new FaraoException(String.format("More than one sensitivity value found for couple Cnec %s - RA %s", cnec.getId(), this.getId()));
        }
        if (sensitivityValueStream.isEmpty()) {
            throw new FaraoException(String.format("No sensitivity value found for couple Cnec %s - RA %s", cnec.getId(), this.getId()));
        }

        return sensitivityValueStream.get(0).getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractElementaryRangeAction otherAbstractElementaryRangeAction = (AbstractElementaryRangeAction) o;

        return super.equals(o)
                && networkElement.equals(otherAbstractElementaryRangeAction.getNetworkElement());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + networkElement.hashCode();
        return result;
    }
}
