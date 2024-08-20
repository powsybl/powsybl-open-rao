/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.usagerule.OnContingencyStateAdderToRemedialAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Most generic interface for remedial actions.
 *
 * A Remedial Action is a lever which can be applied on the network. It is typically used to improve
 * a network situation (e.g. increase the margin on the critical network elements).
 *
 * A Remedial Action contains {@link UsageRule} which defines conditions under which it can be used.
 * For instance, most remedial actions cannot be used in all {@link State}, and the usage rules of the
 * remedial action specify on which state it is available.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RemedialAction<I extends RemedialAction<I>> extends Identifiable<I> {

    /**
     * Get the operator of the remedial action, as a String
     */
    String getOperator();

    /**
     * Get the list of {@link UsageRule} of the Remedial Action
     */
    Set<UsageRule> getUsageRules();

    /**
     * Get the {@link UsageMethod} of the Remedial Action in a given state, deduced from the
     * usage rules of the remedial action
     */
    UsageMethod getUsageMethod(State state);

    /**
     * Get the speed of the Remedial Action, i.e the time it takes to trigger.
     */
    Optional<Integer> getSpeed();

    Set<FlowCnec> getFlowCnecsConstrainingUsageRules(Set<FlowCnec> perimeterCnecs, Network network, State optimizedState);

    Set<FlowCnec> getFlowCnecsConstrainingForOneUsageRule(UsageRule usageRule, Set<FlowCnec> perimeterCnecs, Network network);

    /**
     * Gather all the network elements present in the remedial action. It returns a set because network
     * elements must not be duplicated inside a remedial action and there is no defined order for network elements.
     */
    Set<NetworkElement> getNetworkElements();

    /**
     * Returns the location of the remedial action, as a set of optional countries
     * @param network: the network object used to look for the location of the network elements of the remedial action
     * @return a set of optional countries containing the remedial action
     */
    default Set<Optional<Country>> getLocation(Network network) {
        return getNetworkElements().stream().map(networkElement -> networkElement.getLocation(network))
                .flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());
    }

    OnContingencyStateAdderToRemedialAction<I> newOnStateUsageRule();

    double getActivationCost();
}
