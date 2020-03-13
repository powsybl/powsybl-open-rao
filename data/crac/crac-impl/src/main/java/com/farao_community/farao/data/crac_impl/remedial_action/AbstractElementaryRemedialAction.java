/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.*;

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
public abstract class AbstractElementaryRemedialAction extends AbstractRemedialAction {
    protected NetworkElement networkElement;

    @JsonCreator
    public AbstractElementaryRemedialAction(@JsonProperty("id") String id,
                                            @JsonProperty("name") String name,
                                            @JsonProperty("operator") String operator,
                                            @JsonProperty("usageRules") List<UsageRule> usageRules,
                                            @JsonProperty("networkElement") NetworkElement networkElement) {
        super(id, name, operator, usageRules);
        this.networkElement = networkElement;
    }

    public AbstractElementaryRemedialAction(String id, String name, String operator, NetworkElement networkElement) {
        super(id, name, operator);
        this.networkElement = networkElement;
    }

    public AbstractElementaryRemedialAction(String id, NetworkElement networkElement) {
        super(id);
        this.networkElement = networkElement;
    }

    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    protected abstract double getMinValueWithRange(Network network, Range range);

    protected abstract double getMaxValueWithRange(Network network, Range range);

    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractElementaryRemedialAction otherAbstractElementaryRemedialAction = (AbstractElementaryRemedialAction) o;

        return super.equals(o)
                && networkElement.equals(otherAbstractElementaryRemedialAction.getNetworkElement());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + networkElement.hashCode();
        return result;
    }
}
