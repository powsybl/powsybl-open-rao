/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;
import java.util.List;
import java.util.Set;

/**
 * Interface to manage CRAC
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public interface Crac extends Identifiable, Synchronizable {

    // Instants management
    Instant getInstant(String id);

    void addInstant(Instant instant);

    // Contingencies management
    Set<Contingency> getContingencies();

    Contingency getContingency(String id);

    void addContingency(Contingency contingency);

    //States management

    /**
     * Select the preventive state. This state must be unique. It's the only state that is
     * defined with no contingency.
     *
     * @return The preventive state of the problem definition.
     */
    State getPreventiveState();

    /**
     * Chronological list of states after a defined contingency. The chronology is defined by
     * instants objects.
     *
     * @param contingency: The contingency after which we want to gather states.
     * @return Ordered list of states after the specified contingency.
     */
    List<State> getStates(Contingency contingency);

    /**
     * Unordered set of States defined at the same instant. It will be either the preventive state either
     * the set of all the states defined at at the same instant after all the contingencies.
     *
     * @param instant: The instant at which we want to gather states.
     * @return Unordered set of states at the same specified instant.
     */
    Set<State> getStates(Instant instant);

    /**
     * Select a unique state after a contingency and at a specific instant.
     *
     * @param contingency: The contingency after which we want to select the state.
     * @param instant: The instant at which we want to select the state.
     * @return  State after a contingency and at a specific instant.
     */
    State getState(Contingency contingency, Instant instant);

    void addState(State state);

    // Cnecs management
    Set<Cnec> getCnecs();

    Set<Cnec> getCnecs(State state);

    void addCnec(Cnec cnec);

    // Range actions management
    Set<RangeAction> getRangeActions();

    Set<RangeAction> getRangeActions(Network network, State state, UsageMethod usageMethod);

    void addRangeAction(RangeAction rangeAction);

    // Network actions management
    Set<NetworkAction> getNetworkActions();

    Set<NetworkAction> getNetworkActions(Network network, State state, UsageMethod usageMethod);

    void addNetworkAction(NetworkAction networkAction);

    // General methods
    void generateValidityReport(Network network);
}
