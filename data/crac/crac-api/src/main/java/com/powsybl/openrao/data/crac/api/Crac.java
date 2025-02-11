/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.api;

import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.BranchCnec;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.crac.api.io.Exporter;
import com.powsybl.openrao.data.crac.api.io.Importer;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;

import java.io.*;
import java.util.*;

import static java.lang.String.format;

/**
 * Interface to manage CRAC.
 * CRAC stands for Contingency list, Remedial Actions and additional Constraints
 * <p>
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

    // Instants management

    /**
     * Add instant
     *
     * @return crac
     */
    Crac newInstant(String instantId, InstantKind instantKind);

    /**
     * Get instant based on Id
     * If the ID is null, this will return a null instant
     */
    Instant getInstant(String instantId);

    /**
     * Gather all the instants present in the Crac. It returns a list of ordered instants
     */
    List<Instant> getSortedInstants();

    /**
     * Get instant based on a kind. Throws exception:
     * - if crac does not contain such instant Kind or
     * - if multiple instants of this kind are defined in the crac
     */
    Instant getInstant(InstantKind instantKind);

    /**
     * Returns all the instants present in the Crac with the correct instantKind.
     */
    SortedSet<Instant> getInstants(InstantKind instantKind);

    /**
     * Returns the previous instant of an instant.
     * Optional is empty if no previous instant is defined.
     */
    Instant getInstantBefore(Instant providedInstant);

    /**
     * Returns the unique preventive instant
     */
    Instant getPreventiveInstant();

    /**
     * Returns the unique outage instant
     */
    Instant getOutageInstant();

    /**
     * Return the last instant, i.e. the only instant that has no instant after it
     */
    Instant getLastInstant();

    /**
     * Returns whether a crac has an auto instant
     */
    boolean hasAutoInstant();

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
     * Get the curative states for all curative instants.
     */
    Set<State> getCurativeStates();

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
     * @param instant:     The instant at which we want to select the state.
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
     * @param instant:       The instant at which we want to select the state.
     * @return State after a contingency and at a specific instant. Can return null if no matching
     * state or contingency are found.
     */
    default State getState(String contingencyId, Instant instant) {
        Objects.requireNonNull(contingencyId, "Contingency ID should be defined.");
        Objects.requireNonNull(instant, "Instant should be defined.");
        if (getContingency(contingencyId) == null) {
            throw new OpenRaoException(format("Contingency %s does not exist, as well as the related state.", contingencyId));
        }
        return getState(getContingency(contingencyId), instant);
    }

    // Cnecs management

    /**
     * Get a {@link FlowCnecAdder} adder, to add a {@link FlowCnec} to the Crac
     */
    FlowCnecAdder newFlowCnec();

    /**
     * Get an {@link AngleCnecAdder} adder, to add an {@link AngleCnec} to the Crac
     */
    AngleCnecAdder newAngleCnec();

    /**
     * Get a {@link VoltageCnecAdder} adder, to add a {@link VoltageCnec} to the Crac
     */
    VoltageCnecAdder newVoltageCnec();

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

    Set<Cnec> getCnecs(PhysicalParameter physicalParameter);

    Set<Cnec> getCnecs(PhysicalParameter physicalParameter, State state);

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
    // keep the method (might be useful when we will have other BranchCnec than FlowCnec)
    @Deprecated(since = "3.0.0")
    Set<BranchCnec> getBranchCnecs();

    /**
     * Gather all the BranchCnecs of a specified State. It returns a set because Cnecs
     * must not be duplicated and there is no defined order for Cnecs.
     *
     * @deprecated consider using getCnecs() or getFlowCnecs() instead
     */
    // keep the method (might be useful when we will have other BranchCnec than FlowCnec)
    @Deprecated(since = "3.0.0")
    Set<BranchCnec> getBranchCnecs(State state);

    /**
     * Find a BranchCnec by its id, returns null if the BranchCnec does not exists
     *
     * @deprecated consider using getCnec() or getFlowCnec() instead
     */
    // keep the method (might be usefuls when we will have other BranchCnec than FlowCnec)
    @Deprecated(since = "3.0.0")
    BranchCnec getBranchCnec(String branchCnecId);

    /**
     * Gather all the FlowCnecs present in the Crac. It returns a set because Cnecs must not
     * be duplicated and there is no defined order for Cnecs.
     */
    Set<FlowCnec> getFlowCnecs();

    /**
     * Gather all the FlowCnecs of a specified State. It returns a set because Cnecs must not be
     * duplicated and there is no defined order for Cnecs.
     */
    Set<FlowCnec> getFlowCnecs(State state);

    /**
     * Find a FlowCnec by its id, returns null if the FlowCnec does not exist.
     */
    FlowCnec getFlowCnec(String flowCnecId);

    /**
     * Gather all the AngleCnecs present in the Crac. It returns a set because Cnecs must not
     * be duplicated and there is no defined order for Cnecs.
     */
    Set<AngleCnec> getAngleCnecs();

    /**
     * Gather all the AngleCnecs of a specified State. It returns a set because Cnecs must not be
     * duplicated and there is no defined order for Cnecs.
     */
    Set<AngleCnec> getAngleCnecs(State state);

    /**
     * Find an AngleCnec by its id, returns null if the AngleCnec does not exist.
     */
    AngleCnec getAngleCnec(String angleCnecId);

    /**
     * Gather all the VoltageCnecs present in the Crac. It returns a set because Cnecs must not
     * be duplicated and there is no defined order for Cnecs.
     */
    Set<VoltageCnec> getVoltageCnecs();

    /**
     * Gather all the VoltageCnecs of a specified State. It returns a set because Cnecs must not be
     * duplicated and there is no defined order for Cnecs.
     */
    Set<VoltageCnec> getVoltageCnecs(State state);

    /**
     * Find a VoltageCnec by its id, returns null if the VoltageCnec does not exist.
     */
    VoltageCnec getVoltageCnec(String voltageCnecId);

    /**
     * Remove a Cnec - identified by its id - from the Crac
     */
    void removeCnec(String cnecId);

    /**
     * Remove a FlowCnec - identified by its id - from the Crac
     */
    void removeFlowCnec(String flowCnecId);

    /**
     * Remove a set of FlowCnecs - identified by their id - from the Crac
     */
    void removeFlowCnecs(Set<String> flowCnecsIds);

    /**
     * Remove an AngleCnec - identified by its id - from the Crac
     */
    void removeAngleCnec(String angleCnecId);

    /**
     * Remove a set of AngleCnecs - identified by their id - from the Crac
     */
    void removeAngleCnecs(Set<String> angleCnecsIds);

    /**
     * Remove a VoltageCnec - identified by its id - from the Crac
     */
    void removeVoltageCnec(String voltageCnecId);

    /**
     * Remove a set of VoltageCnecs - identified by their id - from the Crac
     */
    void removeVoltageCnecs(Set<String> voltageCnecsIds);

    // Remedial actions management

    /**
     * Gather all the remedial actions present in the Crac. It returns a set because remedial
     * actions must not be duplicated and there is no defined order for remedial actions.
     */
    Set<RemedialAction<?>> getRemedialActions();

    /**
     * Find a remedial action by its id, returns null if the remedial action does not exists
     */
    RemedialAction<?> getRemedialAction(String remedialActionId);

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
     * Get a {@link HvdcRangeActionAdder}, to add a {@link HvdcRangeAction} to the crac
     */
    HvdcRangeActionAdder newHvdcRangeAction();

    /**
     * Get a {@link InjectionRangeActionAdder}, to add an {@link InjectionRangeAction} to the crac
     */
    InjectionRangeActionAdder newInjectionRangeAction();


    /**
     * Get a {@link CounterTradeRangeActionAdder}, to add an {@link CounterTradeRangeAction} to the crac
     */
    CounterTradeRangeActionAdder newCounterTradeRangeAction();

    /**
     * Gather all the range actions present in the Crac. It returns a set because range
     * actions must not be duplicated and there is no defined order for range actions.
     */
    Set<RangeAction<?>> getRangeActions();

    /**
     * Gather all the range actions of a specified state with one of the specified usage methods
     */
    Set<RangeAction<?>> getRangeActions(State state, UsageMethod... usageMethod);

    /**
     * Gather all the network actions of a specified state that are potentially available
     */
    Set<RangeAction<?>> getPotentiallyAvailableRangeActions(State state);

    default boolean isRangeActionPreventive(RangeAction<?> rangeAction) {
        return isRangeActionAvailableInState(rangeAction, getPreventiveState());
    }

    default boolean isRangeActionAutoOrCurative(RangeAction<?> rangeAction) {
        return getStates().stream()
            .filter(state -> state.getInstant().isAuto() || state.getInstant().isCurative())
            .anyMatch(state -> isRangeActionAvailableInState(rangeAction, state));
    }

    default boolean isRangeActionAvailableInState(RangeAction<?> rangeAction, State state) {
        return getPotentiallyAvailableRangeActions(state).contains(rangeAction);
    }

    /**
     * Find a range action by its id, returns null if the range action does not exists
     */
    RangeAction<?> getRangeAction(String id);

    /**
     * Gather all the PstRangeAction present in the Crac. It returns a set because remedial
     * actions must not be duplicated and there is no defined order for remedial actions.
     */
    Set<PstRangeAction> getPstRangeActions();

    /**
     * Gather all the HvdcRangeAction present in the Crac. It returns a set because remedial
     * actions must not be duplicated and there is no defined order for remedial actions.
     */
    Set<HvdcRangeAction> getHvdcRangeActions();

    /**
     * Gather all the InjectionRangeAction present in the Crac. It returns a set because remedial
     * actions must not be duplicated and there is no defined order for remedial actions.
     */
    Set<InjectionRangeAction> getInjectionRangeActions();

    /**
     * Gather all the CounterTradeRangeAction present in the Crac. It returns a set because remedial
     * actions must not be duplicated and there is no defined order for remedial actions.
     */
    Set<CounterTradeRangeAction> getCounterTradeRangeActions();

    /**
     * Find a PstRangeAction by its id, returns null if the remedial action does not exists
     */
    PstRangeAction getPstRangeAction(String pstRangeActionId);

    /**
     * Find a HvdcRangeAction by its id, returns null if the remedial action does not exists
     */
    HvdcRangeAction getHvdcRangeAction(String hvdcRangeActionId);

    /**
     * Find an InjectionRangeAction by its id, returns null if the remedial action does not exists
     */
    InjectionRangeAction getInjectionRangeAction(String injectionRangeActionId);

    /**
     * Find a CounterTradeRangeAction by its id, returns null if the remedial action does not exists
     */
    CounterTradeRangeAction getCounterTradeRangeAction(String counterTradeRangeActionId);

    /**
     * Remove a PstRangeAction - identified by its id - from the Crac
     */
    void removePstRangeAction(String id);

    /**
     * Remove a HvdcRangeAction - identified by its id - from the Crac
     */
    void removeHvdcRangeAction(String id);

    /**
     * Remove an InjectionRangeAction - identified by its id - from the Crac
     */
    void removeInjectionRangeAction(String id);


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
     * Gather all the network actions of a specified state with one of the specified usage methods
     */
    Set<NetworkAction> getNetworkActions(State state, UsageMethod... usageMethod);

    /**
     * Gather all the network actions of a specified state that are potentially available
     */
    Set<NetworkAction> getPotentiallyAvailableNetworkActions(State state);

    /**
     * Find a NetworkAction by its id, returns null if the network action does not exists
     */
    NetworkAction getNetworkAction(String id);

    /**
     * Remove a NetworkAction - identified by its id - from the Crac
     */
    void removeNetworkAction(String id);

    /**
     * Get all remedial action usage limitation according to the crac creation parameters
     */
    Map<Instant, RaUsageLimits> getRaUsageLimitsPerInstant();

    /**
     * Get remedial action usage limitation for a given instant
     */
    RaUsageLimits getRaUsageLimits(Instant instant);

    /**
     * Get a {@link RaUsageLimitsAdder}, to add a {@link RaUsageLimits} to the crac
     */
    RaUsageLimitsAdder newRaUsageLimits(String instantName);

    /**
     * Get the CRAC format
     *
     * @param filename    CRAC file name
     * @param inputStream CRAC data
     * @return the CRAC format (if found)
     */
    static String getCracFormat(String filename, InputStream inputStream) throws IOException {
        byte[] bytes = getBytesFromInputStream(inputStream);
        return findImporter(filename, bytes).getFormat();
    }

    /**
     * Import CRAC from a file, inside a CracCreationContext
     *
     * @param filename               CRAC file name
     * @param inputStream            CRAC data
     * @param network                the network on which the CRAC data is based
     * @param cracCreationParameters extra CRAC creation parameters
     * @return CracCreationContext object
     */
    static CracCreationContext readWithContext(String filename, InputStream inputStream, Network network, CracCreationParameters cracCreationParameters) throws IOException {
        byte[] bytes = getBytesFromInputStream(inputStream);
        return findImporter(filename, bytes).importData(new ByteArrayInputStream(bytes), cracCreationParameters, network);
    }

    private static Importer findImporter(String filename, byte[] bytes) {
        return new ServiceLoaderCache<>(Importer.class).getServices().stream()
            .filter(importer -> importer.exists(filename, new ByteArrayInputStream(bytes)))
            .findAny()
            .orElseThrow(() -> new OpenRaoException("No suitable CRAC importer found."));
    }

    /**
     * Import CRAC from a file, inside a CracCreationContext
     *
     * @param filename    CRAC file name
     * @param inputStream CRAC data
     * @param network     the network on which the CRAC data is based
     * @return CracCreationContext object
     */
    static CracCreationContext readWithContext(String filename, InputStream inputStream, Network network) throws IOException {
        return readWithContext(filename, inputStream, network, CracCreationParameters.load());
    }

    /**
     * Import CRAC from a file
     *
     * @param filename               CRAC file name
     * @param inputStream            CRAC data
     * @param network                the network on which the CRAC data is based
     * @param cracCreationParameters extra CRAC creation parameters
     * @return CRAC object
     */
    static Crac read(String filename, InputStream inputStream, Network network, CracCreationParameters cracCreationParameters) throws IOException {
        return readWithContext(filename, inputStream, network, cracCreationParameters).getCrac();
    }

    /**
     * Import CRAC from a file
     *
     * @param filename    CRAC file name
     * @param inputStream CRAC data
     * @param network     the network on which the CRAC data is based
     * @return CRAC object
     */
    static Crac read(String filename, InputStream inputStream, Network network) throws IOException {
        return read(filename, inputStream, network, CracCreationParameters.load());
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(inputStream, baos);
        return baos.toByteArray();
    }

    /**
     * Write CRAC data into a file
     *
     * @param exporters    candidate CRAC exporters
     * @param format       desired output CRAC data type
     * @param outputStream file where to write the CRAC data
     */
    private void write(List<Exporter> exporters, String format, OutputStream outputStream) {
        exporters.stream()
            .filter(ex -> format.equals(ex.getFormat()))
            .findAny()
            .orElseThrow(() -> new OpenRaoException("Export format " + format + " not supported"))
            .exportData(this, outputStream);
    }

    /**
     * Write CRAC data into a file
     *
     * @param format       desired output CRAC data type
     * @param outputStream file where to write the CRAC data
     */
    default void write(String format, OutputStream outputStream) {
        write(new ServiceLoaderCache<>(Exporter.class).getServices(), format, outputStream);
    }
}
