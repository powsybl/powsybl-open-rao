/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.joda.time.DateTime;

import java.util.*;

import static java.lang.String.format;

/**
 * Interface to manage CRAC.
 * CRAC stands for Contingency list, Remedial Actions and additional Constraints
 *
 * It involves:
 * <ul>
 *     <li>{@link Instant} objects</li>
 *     <li>{@link Contingency} objects</li>
 *     <li>{@link State} objects: one of them represents the network without contingency applied and can be accessed with the getPreventiveState method</li>
 *     <li>{@link Cnec} objects</li>
 *     <li>{@link RangeAction} objects</li>
 *     <li>{@link NetworkAction} objects</li>
 * </ul>
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface Crac extends Identifiable<Crac> {

    // Contingencies management

    /**
     * Get a {@link ContingencyAdder}, to add a contingency to the Crac
     */
    ContingencyAdder newContingency();

    /**
     * Gather all the contingencies present in the Crac. It returns a set because contingencies
     * must not be duplicated and there is no defined order for contingencies.
     */
    Set<Contingency> getContingencies();

    /**
     * Get a contingency given its id. Returns null if the contingency does not exist.
     */
    Contingency getContingency(String id);

    /**
     * Remove a contingency - identified by its id - from the Crac
     */
    void removeContingency(String id);

    // States management

    /**
     * Gather all the states present in the Crac. It returns a set because states must not
     * be duplicated and there is no defined order for states.
     */
    Set<State> getStates();

    /**
     * Select the preventive state. This state is unique. It's the only state that is
     * defined on the preventive instant, with no contingency.
     */
    State getPreventiveState();

    /**
     * Chronological list of states after a defined contingency. The chronology is defined by
     * instants objects. This is a set because states must not be duplicated and it is sorted
     * by chronology of instants. Can return null if no matching contingency is found.
     *
     * @param contingency: The contingency after which we want to gather states.
     * @return Ordered set of states after the specified contingency.
     */
    SortedSet<State> getStates(Contingency contingency);

    /**
     * Unordered set of States defined at the same instant. It will be either the preventive state or
     * the set of all the states defined at the same instant after all the contingencies. It is a set
     * because states must not be duplicated and there is no defined order for states selected by
     * instants. Can return null if no matching instant is found.
     *
     * @param instant: The instant at which we want to gather states.
     * @return Unordered set of states at the same specified instant.
     */
    Set<State> getStates(Instant instant);

    /**
     * Select a unique state after a contingency and at a specific instant.
     * Can return null if no matching state or contingency are found.
     *
     * @param contingency: The contingency after which we want to select the state.
     * @param instant: The instant at which we want to select the state.
     * @return State after a contingency and at a specific instant.
     */
    State getState(Contingency contingency, Instant instant);

    /**
     * Unordered set of States defined at the same instant. It will be either the preventive state or
     * the set of all the states defined at the same instant after all the contingencies. It is a set
     * because states must not be duplicated and there is no defined order for states selected by
     * instants. Can return null if no matching instant is found.
     *
     * @param instant: The instant at which we want to gather states.
     * @return Unordered set of states at the same specified instant.
     */
    default Set<State> getStatesFromInstant(Instant instant) {
        return getStates(instant);
    }

    /**
     * Chronological list of states after a defined contingency. The chronology is defined by
     * instants objects. This is a set because states must not be duplicated and it is sorted
     * by chronology of instants. Can return null if no matching contingency is found.
     *
     * @param id: The contingency id after which we want to gather states.
     * @return Ordered set of states after the specified contingency.
     */
    default SortedSet<State> getStatesFromContingency(String id) {
        if (getContingency(id) != null) {
            return getStates(getContingency(id));
        } else {
            return new TreeSet<>();
        }
    }

    /**
     * Select a unique state after a contingency and at a specific instant, specified by their ids.
     *
     * @param contingencyId: The contingency id after which we want to select the state.
     * @param instant: The instant at which we want to select the state.
     * @return State after a contingency and at a specific instant. Can return null if no matching
     * state or contingency are found.
     */
    default State getState(String contingencyId, Instant instant) {
        Objects.requireNonNull(contingencyId, "Contingency ID should be defined.");
        Objects.requireNonNull(instant, "Instant should be defined.");
        if (getContingency(contingencyId) == null) {
            throw new FaraoException(format("Contingency %s does not exist, as well as the related state.", contingencyId));
        }
        return getState(getContingency(contingencyId), instant);
    }

    // Cnecs management

    /**
     * Get a {@link FlowCnecAdder} adder, to add a {@link FlowCnec} to the Crac
     */
    FlowCnecAdder newFlowCnec();

    /**
     * Gather all the Cnecs present in the Crac. It returns a set because Cnecs must not
     * be duplicated and there is no defined order for Cnecs.
     */
    Set<Cnec> getCnecs();

    /**
     * Gather all the Cnecs of a specified State. It returns a set because Cnecs
     * must not be duplicated and there is no defined order for Cnecs.
     */
    Set<Cnec> getCnecs(State state);

    /**
     * Find a Cnec by its id, returns null if the Cnec does not exists
     */
    Cnec getCnec(String cnecId);

    /**
     * Gather all the BranchCnecs present in the Crac. It returns a set because Cnecs
     * must not be duplicated and there is no defined order for Cnecs.
     *
     * @deprecated consider using getCnecs() or getFlowCnecs() instead
     */
    // keep the method (might be usefull when we will have other BranchCnec than FlowCnec)
    @Deprecated
    Set<BranchCnec> getBranchCnecs();

    /**
     * Gather all the BranchCnecs of a specified State. It returns a set because Cnecs
     * must not be duplicated and there is no defined order for Cnecs.
     *
     * @deprecated consider using getCnecs() or getFlowCnecs() instead
     */
    // keep the method (might be usefull when we will have other BranchCnec than FlowCnec)
    @Deprecated
    Set<BranchCnec> getBranchCnecs(State state);

    /**
     * Find a BranchCnec by its id, returns null if the BranchCnec does not exists
     *
     * @deprecated consider using getCnec() or getFlowCnec() instead
     */
    // keep the method (might be usefull when we will have other BranchCnec than FlowCnec)
    @Deprecated
    BranchCnec getBranchCnec(String branchCnecId);

    /**
     * Gather all the FlowCnec present in the Crac. It returns a set because Cnecs must not
     * be duplicated and there is no defined order for Cnecs.
     */
    Set<FlowCnec> getFlowCnecs();

    /**
     * Gather all the Cnecs of a specified State. It returns a set because Cnecs must not be
     * duplicated and there is no defined order for Cnecs.
     */
    Set<FlowCnec> getFlowCnecs(State state);

    /**
     * Find a FlowCnec by its id, returns null if the FlowCnec does not exists
     */
    FlowCnec getFlowCnec(String flowCnecId);

    /**
     * Remove a Cnec - identified by its id - from the Crac
     */
    void removeCnec(String cnecId);

    /**
     * Remove a FlowCnec - identified by its id - from the Crac
     */
    void removeFlowCnec(String flowCnecId);

    // Remedial actions management

    /**
     * Gather all the remedial actions present in the Crac. It returns a set because remedial
     * actions must not be duplicated and there is no defined order for remedial actions.
     */
    Set<RemedialAction> getRemedialActions();

    /**
     * Find a remedial action by its id, returns null if the remedial action does not exists
     */
    RemedialAction getRemedialAction(String remedialActionId);

    /**
     * Remove a remedial action - identified by its id - from the Crac
     */
    void removeRemedialAction(String id);

    // Range actions management

    /**
     * Get a {@link PstRangeActionAdder}, to add a {@link PstRangeAction} to the crac
     */
    PstRangeActionAdder newPstRangeAction();

    /**
     * Gather all the range actions present in the Crac. It returns a set because range
     * actions must not be duplicated and there is no defined order for range actions.
     */
    Set<RangeAction> getRangeActions();

    /**
     * Gather all the range actions of a specified state with the specified usage method (available, forced or
     * unavailable).
     */
    Set<RangeAction> getRangeActions(State state, UsageMethod usageMethod);

    /**
     * Find a range action by its id, returns null if the range action does not exists
     */
    RangeAction getRangeAction(String id);

    /**
     * Gather all the PstRangeAction present in the Crac. It returns a set because remedial
     * actions must not be duplicated and there is no defined order for remedial actions.
     */
    Set<PstRangeAction> getPstRangeActions();

    /**
     * Find a PstRangeAction by its id, returns null if the remedial action does not exists
     */
    PstRangeAction getPstRangeAction(String pstRangeActionId);

    /**
     * Remove a PstRangeAction - identified by its id - from the Crac
     */
    void removePstRangeAction(String id);

    // Network actions management

    /**
     * Get a {@link NetworkActionAdder}, to add a {@link NetworkAction} to the crac
     */
    NetworkActionAdder newNetworkAction();

    /**
     * Gather all the network actions present in the Crac. It returns a set because network
     * actions must not be duplicated and there is no defined order for network actions.
     */
    Set<NetworkAction> getNetworkActions();

    /**
     * Gather all the network actions of a specified state with the specified usage method (available, forced or
     * unavailable).
     */
    Set<NetworkAction> getNetworkActions(State state, UsageMethod usageMethod);

    /**
     * Find a NetworkAction by its id, returns null if the network action does not exists
     */
    NetworkAction getNetworkAction(String id);

    /**
     * Remove a NetworkAction - identified by its id - from the Crac
     */
    void removeNetworkAction(String id);
}
