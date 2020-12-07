/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.json.serializers.SimpleCnecSerializer;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Business object of the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("simple-crac")
public class SimpleCrac extends AbstractIdentifiable<Crac> implements Crac {
    private static final String ADD_ELEMENTS_TO_CRAC_ERROR_MESSAGE = "Please add %s and %s to crac first.";
    private static final String ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE = "Please add %s to crac first.";
    private static final String SAME_ELEMENT_ID_DIFFERENT_NAME_ERROR_MESSAGE = "A network element with the same ID (%s) but a different name already exists.";
    private static final String SAME_CONTINGENCY_ID_DIFFERENT_ELEMENTS_ERROR_MESSAGE = "A contingency with the same ID (%s) but a different network elements already exists.";

    private Map<String, NetworkElement> networkElements;
    private Map<String, Instant> instants;
    private Map<String, Contingency> contingencies;
    private Map<String, State> states;
    private Map<String, Cnec> cnecs;
    private Map<String, RangeAction> rangeActions;
    private Map<String, NetworkAction> networkActions;
    private boolean isSynchronized;
    private DateTime networkDate;

    @JsonCreator
    public SimpleCrac(@JsonProperty("id") String id,
                       @JsonProperty("name") String name,
                       @JsonProperty("networkElements") Set<NetworkElement> networkElements,
                       @JsonProperty("instants") Set<Instant> instants,
                       @JsonProperty("contingencies") Set<Contingency> contingencies,
                       @JsonProperty("states") Set<State> states,
                       @JsonProperty("cnecs") Set<Cnec> cnecs,
                       @JsonProperty("rangeActions") Set<RangeAction> rangeActions,
                       @JsonProperty("networkActions") Set<NetworkAction> networkActions) {
        super(id, name);
        this.networkElements = turnIntoMap(networkElements);
        this.instants = turnIntoMap(instants);
        this.states = turnIntoMapForState(states);
        this.cnecs = turnIntoMap(cnecs);
        this.contingencies = turnIntoMap(contingencies);
        this.rangeActions = turnIntoMap(rangeActions);
        this.networkActions = turnIntoMap(networkActions);
        this.isSynchronized = false;
        this.networkDate = null;
    }

    private <T extends Identifiable> Map<String, T> turnIntoMap(Set<T> initialSet) {
        return initialSet.stream().collect(Collectors.toMap(Identifiable::getId, Function.identity()));
    }

    private Map<String, State> turnIntoMapForState(Set<State> initialSet) {
        return initialSet.stream().collect(Collectors.toMap(State::getId, Function.identity()));
    }

    public SimpleCrac(String id, String name) {
        this(id, name, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    public SimpleCrac(String id) {
        this(id, id);
    }

    @Override
    public DateTime getNetworkDate() {
        return networkDate;
    }

    public void setNetworkDate(DateTime networkDate) {
        this.networkDate = networkDate;
    }

    @Override
    public NetworkElementAdder newNetworkElement() {
        return new NetworkElementAdderImpl<SimpleCrac>(this);
    }

    @Override
    public final Set<NetworkElement> getNetworkElements() {
        return new HashSet<>(networkElements.values());
    }

    public NetworkElement addNetworkElement(String networkElementId) {
        return addNetworkElement(networkElementId, networkElementId);
    }

    @Override
    public NetworkElement addNetworkElement(NetworkElement networkElement) {
        return addNetworkElement(networkElement.getId(), networkElement.getName());
    }

    /**
     * This method add a network element to the crac internal set and returns a network element of this set.
     * If an element with the same data is already added, the element of the internal set will be returned,
     * otherwise it is created and then returned. An error is thrown when an element with an already
     * existing ID is added with a different name.
     *
     * @param networkElementId: network element ID as in network files
     * @param networkElementName: network element name for more human readable name
     * @return a network element object that is already defined in the crac
     */
    public NetworkElement addNetworkElement(String networkElementId, String networkElementName) {
        NetworkElement cracNetworkElement = getNetworkElement(networkElementId);
        if (cracNetworkElement == null) {
            cracNetworkElement = new NetworkElement(networkElementId, networkElementName);
        } else if (!cracNetworkElement.getName().equals(networkElementName)) {
            throw new FaraoException(format(SAME_ELEMENT_ID_DIFFERENT_NAME_ERROR_MESSAGE, networkElementId));
        }
        networkElements.put(networkElementId, cracNetworkElement);
        return cracNetworkElement;
    }

    public final NetworkElement getNetworkElement(String id) {
        return networkElements.getOrDefault(id, null);
    }

    @Override
    public InstantAdder newInstant() {
        return new InstantAdderImpl(this);
    }

    @Override
    public final Set<Instant> getInstants() {
        return new HashSet(instants.values());
    }

    @Override
    public Instant getInstant(String id) {
        return instants.get(id);
    }

    public Instant addInstant(String id, int seconds) {
        Instant instant = new Instant(id, seconds);
        checkAndAddInstant(instant);
        return instant;
    }

    @Override
    public void addInstant(Instant instant) {
        checkAndAddInstant(instant);
    }

    private void checkAndAddInstant(Instant instant) {
        // If no strictly equal elements are present in the Crac
        if (instants.values().stream().noneMatch(cracInstant -> cracInstant.equals(instant))) {
            // If an element with the same ID is present
            if (instants.values().stream().anyMatch(cracInstant -> cracInstant.getId().equals(instant.getId()))) {
                throw new FaraoException("An instant with the same ID but different seconds already exists.");
            } else if (instants.values().stream().anyMatch(cracInstant -> cracInstant.getSeconds() == instant.getSeconds())) {
                throw new FaraoException("An instant with the same seconds but different ID already exists.");
            }
            instants.put(instant.getId(), instant);
        }
    }

    @Override
    public Set<Contingency> getContingencies() {
        return new HashSet(contingencies.values());
    }

    @Override
    public Contingency getContingency(String id) {
        return contingencies.get(id);
    }

    @Override
    public void removeContingency(String id) {
        contingencies.remove(id);
    }

    @Override
    public ContingencyAdder newContingency() {
        return new ComplexContingencyAdder(this);
    }

    public Contingency addContingency(String id, String... networkElementIds) {
        Set<NetworkElement> networkElementsToAdd = new HashSet<>();
        for (String networkElementId: networkElementIds) {
            networkElementsToAdd.add(addNetworkElement(networkElementId));
        }
        Contingency contingency = new ComplexContingency(id, networkElementsToAdd);
        addContingency(contingency);
        return Objects.requireNonNull(getContingency(contingency.getId()));
    }

    @Override
    public void addContingency(Contingency contingency) {
        // If no strictly equal elements are present in the Crac
        if (contingencies.values().stream().noneMatch(cracContingency -> cracContingency.equals(contingency))) {
            // If an element with the same ID is present
            if (contingencies.values().stream().anyMatch(cracContingency -> cracContingency.getId().equals(contingency.getId()))) {
                throw new FaraoException(format(SAME_CONTINGENCY_ID_DIFFERENT_ELEMENTS_ERROR_MESSAGE, contingency.getId()));
            }
            /*
             * A contingency contains several network elements to trip. When adding a contingency, all the contained
             * network elements have to be in the networkElements list of the crac.
             * Here we go through all the network elements of the contingency, if an equal element is already present in
             * the crac list we can directly pick its reference, if not we first have to create a new element of the
             * list copying the network element contained in the contingency.
             * Then we can create a new contingency referring to network elements already presents in the crac.
             */
            Set<NetworkElement> networkElementsFromInternalSet = new HashSet<>();
            for (NetworkElement networkElement : contingency.getNetworkElements()) {
                networkElementsFromInternalSet.add(addNetworkElement(networkElement.getId(), networkElement.getName()));
            }
            contingencies.put(contingency.getId(), new ComplexContingency(contingency.getId(), contingency.getName(), networkElementsFromInternalSet));
        }
    }

    public final Set<State> getStates() {
        return new HashSet(states.values());
    }

    public final State getState(String id) {
        return states.get(id);
    }

    @Override
    @JsonIgnore
    public State getPreventiveState() {
        return states.values().stream().filter(state -> !state.getContingency().isPresent()).findAny().orElse(null);
    }

    @Override
    public SortedSet<State> getStates(Contingency contingency) {
        return states.values().stream()
            .filter(state -> state.getContingency().isPresent() && state.getContingency().get().getId().equals(contingency.getId()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public Set<State> getStates(Instant instant) {
        return states.values().stream()
            .filter(state -> state.getInstant().getId().equals(instant.getId()))
            .collect(Collectors.toSet());
    }

    @Override
    public State getState(Contingency contingency, Instant instant) {
        return states.values().stream()
            .filter(state -> state.getContingency().isPresent() && state.getInstant().getId().equals(instant.getId()))
            .filter(state -> state.getContingency().isPresent() && state.getContingency().get().getId().equals(contingency.getId()))
            .findAny()
            .orElse(null);
    }

    @Override
    public void removeState(String id) {
        states.remove(id);
    }

    public State addState(Contingency contingency, Instant instant) {
        State state;
        if (contingency != null) {
            if (contingencies.containsKey(contingency.getId()) && instants.containsKey(instant.getId())) {
                state = new SimpleState(Optional.of(getContingency(contingency.getId())), getInstant(instant.getId()));
            } else {
                throw new FaraoException(format(ADD_ELEMENTS_TO_CRAC_ERROR_MESSAGE, contingency.getId(), instant.getId()));
            }
        } else {
            if (instants.containsKey(instant.getId())) {
                state = new SimpleState(Optional.empty(), getInstant(instant.getId()));
            } else {
                throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, instant.getId()));
            }
        }
        states.put(state.getId(), state);
        return state;
    }

    public State addState(String contingencyId, String instantId) {
        State state;
        if (contingencyId != null) {
            if (getContingency(contingencyId) != null && getInstant(instantId) != null) {
                state = new SimpleState(Optional.of(getContingency(contingencyId)), getInstant(instantId));
            } else {
                throw new FaraoException(format(ADD_ELEMENTS_TO_CRAC_ERROR_MESSAGE, contingencyId, instantId));
            }
        } else {
            if (getInstant(instantId) != null) {
                state = new SimpleState(Optional.empty(), getInstant(instantId));
            } else {
                throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, instantId));
            }
        }
        states.put(state.getId(), state);
        return state;
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
        if (instants.values().stream().noneMatch(instant ->
            instant.getId().equals(state.getInstant().getId()) && instant.getSeconds() == state.getInstant().getSeconds())
        ) {
            // Can thow FaraoException if this instant and already present instants are incompatible
            addInstant(state.getInstant());
        }
        Instant instant = getInstant(state.getInstant().getId());

        Optional<Contingency> stateContingency = state.getContingency();
        Optional<Contingency> contingency;
        if (stateContingency.isPresent()) {
            if (contingencies.values().stream().noneMatch(stateContingency.get()::equals)) {
                addContingency(stateContingency.get());
            }
            contingency = Optional.of(getContingency(stateContingency.get().getId()));
        } else {
            contingency = Optional.empty();
        }
        State newState = new SimpleState(contingency, instant);
        states.put(newState.getId(), newState);
    }

    @Override
    public CnecAdder newCnec() {
        return new SimpleCnecAdder(this);
    }

    @Override
    public Cnec getCnec(String id) {
        return cnecs.get(id);
    }

    @JsonSerialize(contentUsing = SimpleCnecSerializer.class)
    @Override
    public Set<Cnec> getCnecs() {
        return new HashSet<>(cnecs.values());
    }

    @Override
    public Set<Cnec> getCnecs(State state) {
        return cnecs.values().stream()
            .filter(cnec -> cnec.getState().equals(state))
            .collect(Collectors.toSet());
    }

    @Override
    public void removeCnec(String cnecId) {
        cnecs.remove(cnecId);
    }

    public Cnec addCnec(String id, NetworkElement networkElement, Set<AbstractThreshold> abstractThresholds, State state) {
        if (!networkElements.containsKey(networkElement.getId()) || !states.containsKey(state.getId())) {
            throw new FaraoException(format(ADD_ELEMENTS_TO_CRAC_ERROR_MESSAGE, networkElement.getId(), state.getId()));
        }
        Cnec cnec = new SimpleCnec(id, networkElement, abstractThresholds, state);
        cnecs.put(id, cnec);
        return cnec;
    }

    public Cnec addCnec(String id, String name, String networkElementId, Set<AbstractThreshold> abstractThresholds, String stateId, double frm, boolean optimized, boolean monitored) {
        if (getNetworkElement(networkElementId) == null || getState(stateId) == null) {
            throw new FaraoException(format(ADD_ELEMENTS_TO_CRAC_ERROR_MESSAGE, networkElementId, stateId));
        }
        Cnec cnec = new SimpleCnec(id, name, getNetworkElement(networkElementId), abstractThresholds, getState(stateId), frm, optimized, monitored);
        cnecs.put(id, cnec);
        return cnec;
    }

    public Cnec addCnec(String id, String name, String networkElementId, Set<AbstractThreshold> abstractThresholds, String stateId, double frm) {
        return this.addCnec(id, name, networkElementId, abstractThresholds, stateId, frm, true, false);
    }

    public Cnec addCnec(String id, String networkElementId, Set<AbstractThreshold> abstractThresholds, String stateId) {
        return this.addCnec(id, id, networkElementId, abstractThresholds, stateId, 0);
    }

    @Override
    public void addCnec(Cnec cnec) {
        addState(cnec.getState());
        NetworkElement networkElement = addNetworkElement(cnec.getNetworkElement());

        // add cnec
        cnecs.put(cnec.getId(), ((SimpleCnec) cnec).copy(networkElement, getState(cnec.getState().getId()), ((SimpleCnec) cnec).getFrm(), cnec.isOptimized(), cnec.isMonitored()));

        // add extensions
        if (!cnec.getExtensions().isEmpty()) {
            Cnec cnecInCrac = getCnec(cnec.getId());
            ExtensionsHandler.getExtensionsSerializers().addExtensions(cnecInCrac, cnec.getExtensions());
        }
    }

    @Override
    public PstRangeActionAdder newPstRangeAction() {
        return new PstRangeActionAdderImpl(this);
    }

    @Override
    public Set<RangeAction> getRangeActions() {
        return new HashSet<>(rangeActions.values());
    }

    @Override
    public Set<NetworkAction> getNetworkActions() {
        return new HashSet<>(networkActions.values());
    }

    public void addNetworkAction(NetworkAction networkAction) {
        networkAction.getNetworkElements().forEach(this::addNetworkElement);
        networkActions.put(networkAction.getId(), networkAction);
    }

    public void addRangeAction(RangeAction rangeAction) {
        rangeActions.put(rangeAction.getId(), rangeAction);
    }

    @Override
    public Set<NetworkAction> getNetworkActions(Network network, State state, UsageMethod usageMethod) {
        return networkActions.values().stream()
            .filter(networkAction -> networkAction.getUsageMethod(network, state).equals(usageMethod))
            .collect(Collectors.toSet());
    }

    @Override
    public NetworkAction getNetworkAction(String id) {
        return networkActions.get(id);
    }

    @Override
    public void removeNetworkAction(String id) {
        networkActions.remove(id);
    }

    @Override
    public Set<RangeAction> getRangeActions(Network network, State state, UsageMethod usageMethod) {
        return rangeActions.values().stream()
            .filter(rangeAction -> rangeAction.getUsageMethod(network, state).equals(usageMethod))
            .collect(Collectors.toSet());
    }

    @Override
    public RangeAction getRangeAction(String id) {
        return rangeActions.get(id);
    }

    @Override
    public void removeRangeAction(String id) {
        rangeActions.remove(id);
    }

    @Override
    public void synchronize(Network network) {
        if (isSynchronized) {
            throw new AlreadySynchronizedException(format("Crac %s has already been synchronized", getId()));
        }
        cnecs.values().forEach(cnec -> cnec.synchronize(network));
        rangeActions.values().forEach(rangeAction -> rangeAction.synchronize(network));
        networkDate = network.getCaseDate();
        isSynchronized = true;
    }

    @Override
    public void desynchronize() {
        cnecs.values().forEach(Synchronizable::desynchronize);
        rangeActions.values().forEach(Synchronizable::desynchronize);
        networkDate = null;
        isSynchronized = false;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }
}
