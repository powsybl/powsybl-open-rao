/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRange;
import com.fasterxml.jackson.annotation.*;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Business object of the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class SimpleCrac extends AbstractIdentifiable implements Crac {

    private Set<Instant> instants;
    private Set<Contingency> contingencies;
    private Set<State> states;
    private Set<Cnec> cnecs;
    private Set<RangeAction> rangeActions;
    private Set<NetworkAction> networkActions;

    @JsonCreator
    public SimpleCrac(@JsonProperty("id") String id,
                       @JsonProperty("name") String name,
                       @JsonProperty("instants") Set<Instant> instants,
                       @JsonProperty("contingencies") Set<Contingency> contingencies,
                       @JsonProperty("states") Set<State> states,
                       @JsonProperty("cnecs") Set<Cnec> cnecs,
                       @JsonProperty("rangeActions") Set<RangeAction> rangeActions,
                       @JsonProperty("networkActions") Set<NetworkAction> networkActions) {
        super(id, name);
        this.instants = instants;
        this.states = states;
        this.cnecs = cnecs;
        this.contingencies = contingencies;
        this.rangeActions = rangeActions;
        this.networkActions = networkActions;
    }

    public SimpleCrac(String id, String name) {
        this(id, name, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    public SimpleCrac(String id) {
        this(id, id);
    }

    final Set<Instant> getInstants() {
        return instants;
    }

    final Set<State> getStates() {
        return states;
    }

    @Override
    public Instant getInstant(String id) {
        return instants.stream()
            .filter(instant -> instant.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public void addInstant(Instant instant) {
        // If no strictly equal elements are present in the Crac
        if (instants.stream().noneMatch(cracInstant -> cracInstant.equals(instant))) {
            // If an element with the same ID is present
            if (instants.stream().anyMatch(cracInstant -> cracInstant.getId().equals(instant.getId()))) {
                throw new FaraoException("An instant with the same ID but different seconds already exists.");
            } else if (instants.stream().anyMatch(cracInstant -> cracInstant.getSeconds() == instant.getSeconds())) {
                throw new FaraoException("An instant with the same seconds but different ID already exists.");
            }
            instants.add(instant);
        }
    }

    @Override
    public Set<Contingency> getContingencies() {
        return contingencies;
    }

    @Override
    public Contingency getContingency(String id) {
        return contingencies.stream()
            .filter(contingency -> contingency.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public void addContingency(Contingency contingency) {
        // If no strictly equal elements are present in the Crac
        if (contingencies.stream().noneMatch(cracContingency -> cracContingency.equals(contingency))) {
            // If an element with the same ID is present
            if (contingencies.stream().anyMatch(cracContingency -> cracContingency.getId().equals(contingency.getId()))) {
                throw new FaraoException("A contingency with the same ID and different network elements already exists.");
            }
            contingencies.add(contingency);
        }
    }

    @Override
    public State getPreventiveState() {
        return states.stream().filter(state -> !state.getContingency().isPresent()).findFirst().orElse(null);
    }

    @Override
    public SortedSet<State> getStates(Contingency contingency) {
        return states.stream()
            .filter(state -> state.getContingency().isPresent() && state.getContingency().get().getId().equals(contingency.getId()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public Set<State> getStates(Instant instant) {
        return states.stream()
            .filter(state -> state.getInstant().getId().equals(instant.getId()))
            .collect(Collectors.toSet());
    }

    @Override
    public State getState(Contingency contingency, Instant instant) {
        return states.stream()
            .filter(state -> state.getContingency().isPresent() && state.getInstant().getId().equals(instant.getId()))
            .filter(state -> state.getContingency().isPresent() && state.getContingency().get().getId().equals(contingency.getId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Add a state in the Crac object. When adding a state which is made of a Contingency and an Instant,
     * these Contingency and Instant objects have to be independently present in the Crac as well. So if they
     * are not, they have to be added as well.
     * Then the State has to point on the good objects, meaning those which are present independently in the Crac.
     * So in the end a new State object will be created with references on Contingency and Instant objects
     * that have been already added to the Crac.
     *
     * @param state: state object that can be created from already existing Contingency and Instant object of the Crac
     *             or not.
     */
    @Override
    public void addState(State state) {
        // If the two instants are strictly equals no need to add it
        if (instants.stream().noneMatch(instant ->
            instant.getId().equals(state.getInstant().getId()) && instant.getSeconds() == state.getInstant().getSeconds())
        ) {
            // Can thow FaraoException if this instant and already present instants are incompatible
            addInstant(state.getInstant());
        }
        Instant instant = getInstant(state.getInstant().getId());

        Optional<Contingency> stateContingency = state.getContingency();
        Optional<Contingency> contingency;
        if (stateContingency.isPresent()) {
            if (contingencies.stream().noneMatch(stateContingency.get()::equals)) {
                addContingency(stateContingency.get());
            }
            contingency = Optional.of(getContingency(stateContingency.get().getId()));
        } else {
            contingency = Optional.empty();
        }
        states.add(new SimpleState(contingency, instant));
    }

    @Override
    public Set<Cnec> getCnecs() {
        return cnecs;
    }

    @Override
    public Set<Cnec> getCnecs(State state) {
        return cnecs.stream()
            .filter(cnec -> cnec.getState().equals(state))
            .collect(Collectors.toSet());
    }

    @Override
    public void addCnec(Cnec cnec) {
        addState(cnec.getState());
        Optional<Contingency> contingency = cnec.getState().getContingency();
        State state;
        if (contingency.isPresent()) {
            state = getState(contingency.get(), cnec.getState().getInstant());
        } else {
            state = getPreventiveState();
        }
        cnecs.add(new SimpleCnec(
            cnec.getId(),
            cnec.getName(),
            cnec.getCriticalNetworkElement(),
            cnec.getThreshold(),
            state
        ));
    }

    @Override
    public Set<RangeAction> getRangeActions() {
        return rangeActions;
    }

    @Override
    public Set<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    @Override
    public void addNetworkAction(NetworkAction networkAction) {
        networkAction.getUsageRules().forEach(usageRule -> addState(usageRule.getState()));
        networkActions.add(networkAction);
    }

    @Override
    public void addRangeAction(RangeAction rangeAction) {
        rangeAction.getUsageRules().forEach(usageRule -> addState(usageRule.getState()));
        rangeActions.add(rangeAction);
    }

    @Override
    public Set<NetworkAction> getNetworkActions(Network network, State state, UsageMethod usageMethod) {
        return networkActions.stream()
            .filter(networkAction -> networkAction.getUsageMethod(network, state).equals(usageMethod))
            .collect(Collectors.toSet());
    }

    @Override
    public NetworkAction getNetworkAction(String id) {
        return networkActions.stream()
                .filter(networkAction -> networkAction.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<RangeAction> getRangeActions(Network network, State state, UsageMethod usageMethod) {
        return rangeActions.stream()
            .filter(rangeAction -> rangeAction.getUsageMethod(network, state).equals(usageMethod))
            .collect(Collectors.toSet());
    }

    @Override
    public RangeAction getRangeAction(String id) {
        return rangeActions.stream()
                .filter(rangeAction ->  rangeAction.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void synchronize(Network network) {
        cnecs.forEach(cnec -> cnec.synchronize(network));
        rangeActions.forEach(rangeAction -> rangeAction.synchronize(network));
    }

    @Override
    public void desynchronize() {
        cnecs.forEach(Synchronizable::desynchronize);
    }

    @Override
    public void setReferenceValues(Network network) {
        rangeActions.stream()
                .filter(rangeAction -> rangeAction instanceof PstRange)
                .forEach(rangeAction -> ((PstRange) rangeAction).setReferenceValue(network));
    }

    @Override
    public void generateValidityReport(Network network) {
        throw new UnsupportedOperationException();
    }
}
