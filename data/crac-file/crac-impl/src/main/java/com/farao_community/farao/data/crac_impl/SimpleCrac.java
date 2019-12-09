/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
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

    private List<Instant> instants;
    private List<State> states;
    private List<Cnec> cnecs;
    private List<Contingency> contingencies;
    private List<RangeAction> rangeActions;
    private List<NetworkAction> networkActions;

    @JsonCreator
    public SimpleCrac(@JsonProperty("id") String id, @JsonProperty("name") String name,
                      @JsonProperty("instants") List<Instant> instants,
                      @JsonProperty("states") List<State> states,
                      @JsonProperty("cnecs") List<Cnec> cnecs,
                      @JsonProperty("contingencies") List<Contingency> contingencies,
                      @JsonProperty("rangeActions") List<RangeAction> rangeActions,
                      @JsonProperty("networkActions") List<NetworkAction> networkActions) {
        super(id, name);
        this.instants = instants;
        this.states = states;
        this.cnecs = cnecs;
        this.contingencies = contingencies;
        this.rangeActions = rangeActions;
        this.networkActions = networkActions;
    }

    public SimpleCrac(String id, String name) {
        this(id, name, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public SimpleCrac(String id) {
        this(id, id);
    }

    @Override
    public List<Cnec> getCnecs() {
        return cnecs;
    }

    @Override
    public void setCnecs(List<Cnec> cnecs) {
        this.cnecs = cnecs;
    }

    @Override
    public List<RangeAction> getRangeActions() {
        return rangeActions;
    }

    @Override
    public void setRangeActions(List<RangeAction> rangeActions) {
        this.rangeActions = rangeActions;
    }

    @Override
    public List<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    @Override
    public void setNetworkActions(List<NetworkAction> networkActions) {
        this.networkActions = networkActions;
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
        if (states.stream().noneMatch(cracState -> cracState.equals(state))) {
            // reference that will point at an object existing in the Crac
            Optional<Contingency> cracContingency;
            if (state.getContingency().isPresent()) {
                Contingency contingency = state.getContingency().get();
                if (getContingency(contingency.getId()) == null) {
                    addContingency(contingency);
                }
                cracContingency = Optional.of(getContingency(contingency.getId()));
            } else {
                cracContingency = Optional.empty();
            }

            if (getInstant(state.getInstant().getId()) == null) {
                addInstant(state.getInstant());
            }

            states.add(new SimpleState(cracContingency, getInstant(state.getInstant().getId())));
        }
    }

    @Override
    @JsonProperty("cnecs")
    public void addCnec(Cnec cnec) {
        cnecs.add(cnec);
    }

    @Override
    public void addInstant(Instant instant) {
        if (getInstant(instant.getId()) == null) {
            if (instants.stream().anyMatch(cracInstant -> cracInstant.equals(instant))) {
                throw new FaraoException("Two instants with same duration and different IDs cannot be added.");
            }
            instants.add(instant);
        }
    }

    @JsonProperty("contingency")
    @Override
    public void addContingency(Contingency contingency) {
        if (getContingency(contingency.getId()) == null) {
            contingencies.add(contingency);
        }
    }

    @JsonProperty("networkActions")
    @Override
    public void addNetworkRemedialAction(NetworkAction networkAction) {
        networkActions.add(networkAction);
    }

    @Override
    @JsonProperty("rangeActions")
    public void addRangeRemedialAction(RangeAction rangeAction) {
        rangeActions.add(rangeAction);
    }

    @Override
    @JsonIgnore
    public State getPreventiveState() {
        return states.stream().filter(state -> !state.getContingency().isPresent()).findFirst().orElse(null);
    }

    @Override
    public List<State> getStates(Contingency contingency) {
        return states.stream()
            .filter(state -> state.getContingency().isPresent() && state.getContingency().get().getId().equals(contingency.getId()))
            .sorted().collect(Collectors.toList());
    }

    @Override
    public List<State> getStates(Instant instant) {
        return states.stream()
            .filter(state -> state.getInstant().getId().equals(instant.getId()))
            .sorted().collect(Collectors.toList());
    }

    @Override
    public State getState(Contingency contingency, Instant instant) {
        return states.stream()
            .filter(state -> state.getContingency().isPresent() && state.getInstant().getId().equals(instant.getId()))
            .filter(state -> state.getContingency().isPresent() && state.getContingency().get().getId().equals(contingency.getId()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public Set<NetworkAction> getNetworkActions(Network network, State state, UsageMethod usageMethod) {
        return networkActions.stream()
            .filter(networkAction -> networkAction.getUsageMethod(network, state).equals(usageMethod))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<RangeAction> getRangeActions(Network network, State state, UsageMethod usageMethod) {
        return rangeActions.stream()
            .filter(networkAction -> networkAction.getUsageMethod(network, state).equals(usageMethod))
            .collect(Collectors.toSet());
    }

    @Override
    public Contingency getContingency(String id) {
        return contingencies.stream()
            .filter(contingency -> contingency.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public Instant getInstant(String id) {
        return instants.stream()
            .filter(instant -> instant.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public Set<Cnec> getCnecs(State state) {
        return cnecs.stream()
            .filter(cnec -> cnec.getState().equals(state))
            .collect(Collectors.toSet());
    }

    @Override
    @JsonIgnore
    public List<NetworkElement> getCriticalNetworkElements() {
        List<NetworkElement> criticalNetworkElements = new ArrayList<>();
        cnecs.forEach(cnec -> criticalNetworkElements.add(cnec.getCriticalNetworkElement()));
        return criticalNetworkElements;
    }

    @Override
    public List<Contingency> getContingencies() {
        return contingencies;
    }

    @Override
    public void synchronize(Network network) {
        cnecs.forEach(cnec -> cnec.synchronize(network));
    }

    @Override
    public void desynchronize() {
        cnecs.forEach(Synchronizable::desynchronize);
    }

    @Override
    public void generateValidityReport(Network network) {
        throw new UnsupportedOperationException();
    }
}
