/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;
import java.util.List;
import java.util.Set;

/**
 * Most generic interface for remedial actions
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public interface RemedialAction extends Identifiable {

    String getOperator();

    UsageMethod getUsageMethod(Network network, State state);

    List<UsageRule> getUsageRules();

    void addUsageRule(UsageRule usageRule);

    /**
     * Gather all the network elements present in the applicable range action. It returns a set because network
     * elements must not be duplicated inside an applicable range action and there is no defined order for network elements.
     *
     * @return A set of network elements.
     */
    @JsonIgnore
    Set<NetworkElement> getNetworkElements();
}
