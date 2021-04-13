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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;

import java.time.OffsetDateTime;
import java.util.*;

import static java.lang.String.format;

/**
 * Interface to manage CRAC.
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
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface Crac extends Identifiable<Crac>, Synchronizable {

    @Deprecated
    //todo: delete
    // use crac creation context instead
    DateTime getNetworkDate();

    @Deprecated
    Set<NetworkElement> getNetworkElements();

    @Deprecated
    NetworkElement getNetworkElement(String networkElementId);

    // Contingencies management
    /**
     * Get a {@code Contingency} adder, to add a contingency to the Crac
     * @return a {@code ContingencyAdder} instance
     */
    ContingencyAdder newContingency();

    /**
     * Gather all the contingencies present in the Crac. It returns a set because contingencies
     * must not be duplicated and there is no defined order for contingencies.
     *
     * @return A set of contingencies.
     */
    Set<Contingency> getContingencies();

    //todo : javadoc
    Contingency getContingency(String id);

    void removeContingency(String id);

    @Deprecated
    //todo: delete it
    void addContingency(Contingency contingency);

    // States management
    /**
     * Select the preventive state. This state must be unique. It's the only state that is
     * defined with no contingency.
     *
     * @return The preventive state of the problem definition.
     */
    State getPreventiveState();

    /**
     * Gather all the states present in the Crac. It returns a set because states must not
     * be duplicated and there is no defined order for states.
     *
     * @return Unordered set of states
     */
    Set<State> getStates();

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

    void removeState(String stateId);

    // Cnecs management
    /**
     * Get a {@code Cnec} adder, to add a cnec to the Crac
     * @return a {@code CnecAdder} instance
     */
    FlowCnecAdder newFlowCnec();

    //todo : javadoc
    Set<Cnec> getCnecs();

    //todo : javadoc
    Set<Cnec> getCnecs(State state);

    //todo : javadoc
    Cnec getCnec(String cnecId);

    //todo: javadoc
    Set<FlowCnec> getFlowCnecs();

    //todo: javadoc
    Set<FlowCnec> getFlowCnecs(State state);

    //todo: javadoc
    FlowCnec getFlowCnec(String flowCnecId);

    /**
     * Remove a Cnec by its id
     *
     * @param cnecId: the Cnec identifier.
     */
    void removeCnec(String cnecId);

    void removeFlowCnec(String flowCnecId);

    /**
     * Gather all the Cnecs of a specified State. It returns a set because Cnecs
     * must not be duplicated and there is no defined order for Cnecs.
     *
     * @param state : The state on which we want to select Cnecs.
     * @return A set of Cnecs.
     */
    @Deprecated
    //todo: keep deprecated (might be usefull when we will have other BranchCnec than FlowCnec)
    // Use getCnec() or getFlowCnec() instead
    Set<BranchCnec> getBranchCnecs(State state);

    default Set<BranchCnec> getBranchCnecs(String contingencyId, Instant instant) {
        if (getState(contingencyId, instant) != null) {
            return getBranchCnecs(getState(contingencyId, instant));
        } else {
            return new HashSet<>();
        }
    }

    /**
     * Gather all the Cnecs present in the Crac. It returns a set because Cnecs
     * must not be duplicated and there is no defined order for Cnecs.
     *
     *
     * @return A set of Cnecs.
     */
    @Deprecated
    //todo: keep deprecated (might be usefull when we will have other BranchCnec than FlowCnec)
    // Use getCnecs() or getFlowCnecs() instead
    Set<BranchCnec> getBranchCnecs();

    /**
     * Find a Cnec by its id
     *
     * @param branchCnecId : the Cnec identifier.
     * @return The Cnec with the id given in argument. Or null if it does not exist.
     */
    @Deprecated
    //todo: keep deprecated (might be usefull when we will have other BranchCnec than FlowCnec)
    // Use getCnec() or getFlowCnec() instead
    BranchCnec getBranchCnec(String branchCnecId);


    @Deprecated
    //todo: delete
    // Use newFlowCnec() instead
    void addCnec(Cnec<?> cnec);

    // Remedial actions management

    //todo : javadoc
    Set<RemedialAction> getRemedialActions();

    //todo : javadoc
    RemedialAction getRemedialAction(String remedialActionId);

    void removeRemedialAction(String id);

    // Range actions management
    /**
     * Get a PstRangeAction adder, to add a {@code PstRangeAction}
     * @return a {@code PstRangeActionAdder} instance
     */
    PstRangeActionAdder newPstRangeAction();

    //todo: javadoc
    Set<PstRangeAction> getPstRangeActions();

    //todo: javadocc
    PstRangeAction getPstRangeAction(String pstRangeActionId);

    /**
     * Gather all the range actions present in the Crac. It returns a set because range
     * actions must not be duplicated and there is no defined order for range actions.
     *
     * @return A set of range actions.
     */
    Set<RangeAction> getRangeActions();

    /**
     * Gather all the range actions of a specified state with the specified usage method (available, forced or
     * unavailable). A network is required to determine the usage method. It returns a set because range
     * actions must not be duplicated and there is no defined order for range actions.
     *
     * @param state: Specific state on which range actions can be selected.
     * @param usageMethod: Specific usage method to select range actions.
     * @return A set of range actions.
     */
    Set<RangeAction> getRangeActions(State state, UsageMethod usageMethod);

    /**
     * @param id: id of the RangeAction to get
     * @return null if the RangeAction does not exist in the Crac, the RangeAction otherwise
     */
    RangeAction getRangeAction(String id);

    /**
     * @param id: id of the RangeAction to remove
     */
    void removePstRangeAction(String id);

    // Network actions management

    //todo : javadoc
    NetworkActionAdder newNetworkAction();

    /**
     * Gather all the network actions present in the Crac. It returns a set because network
     * actions must not be duplicated and there is no defined order for network actions.
     *
     * @return A set of network actions.
     */
    Set<NetworkAction> getNetworkActions();

    /**
     * Gather all the network actions of a specified state with the specified usage method (available, forced or
     * unavailable). To determine this usage method it requires a network. It returns a set because network
     * actions must not be duplicated and there is no defined order for network actions.
     *
     * @param state: Specific state on which network actions can be selected.
     * @param usageMethod: Specific usage method to select network actions.
     * @return A set of network actions.
     */
    Set<NetworkAction> getNetworkActions(State state, UsageMethod usageMethod);

    /**
     * @param id: id of the NetworkAction to get
     * @return null if the NetworkAction does not exist in the Crac, the NetworkAction otherwise
     */
    NetworkAction getNetworkAction(String id);

    /**
     * @param id: id of the NetworkAction to remove
     */
    void removeNetworkAction(String id);

}
