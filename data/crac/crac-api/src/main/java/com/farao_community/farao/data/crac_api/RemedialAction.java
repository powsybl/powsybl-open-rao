/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Most generic interface for remedial actions
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface RemedialAction<I extends RemedialAction<I>> extends Identifiable<I> {

    String getOperator();

    UsageMethod getUsageMethod(State state);

    List<UsageRule> getUsageRules();

    @Deprecated
    void addUsageRule(UsageRule usageRule);

    /**
     * Gather all the network elements present in the applicable range action. It returns a set because network
     * elements must not be duplicated inside an applicable range action and there is no defined order for network elements.
     *
     * @return A set of network elements.
     */
    @JsonIgnore
    Set<NetworkElement> getNetworkElements();

    /**
     * Returns the location of the remedial action, as a set of optional countries
     * @param network: the network object used to look for the network elements of the remedial action
     * @return a set of optional countries containing the remedial action
     */
    @JsonIgnore
    default Set<Optional<Country>> getLocation(Network network) {
        return getNetworkElements().stream().map(networkElement -> networkElement.getLocation(network))
                .flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());
    }
}
