/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_impl.json.serializers.SimpleCnecSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.NetworkActionSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.range_action.RangeActionSerializer;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCrac.class);

    private Set<NetworkElement> networkElements;
    private Set<Instant> instants;
    private Set<Contingency> contingencies;
    private Set<State> states;
    private Set<Cnec> cnecs;
    private Set<RangeAction> rangeActions;
    private Set<NetworkAction> networkActions;
    private boolean isSynchronized;

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
        this.networkElements = networkElements;
        this.instants = instants;
        this.states = states;
        this.cnecs = cnecs;
        this.contingencies = contingencies;
        this.rangeActions = rangeActions;
        this.networkActions = networkActions;
        this.isSynchronized = false;
    }

    public SimpleCrac(String id, String name) {
        this(id, name, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    public SimpleCrac(String id) {
        this(id, id);
    }

    @Override
    public final Set<NetworkElement> getNetworkElements() {
        return networkElements;
    }

    public NetworkElement addNetworkElement(String networkElementId) {
        return addNetworkElement(networkElementId, networkElementId);
    }

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
        networkElements.add(cracNetworkElement);
        return cracNetworkElement;
    }

    public final NetworkElement getNetworkElement(String id) {
        return networkElements.stream().filter(networkElement -> networkElement.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public final Set<Instant> getInstants() {
        return instants;
    }

    @Override
    public Instant getInstant(String id) {
        return instants.stream()
            .filter(instant -> instant.getId().equals(id))
            .findFirst()
            .orElse(null);
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
        if (contingencies.stream().noneMatch(cracContingency -> cracContingency.equals(contingency))) {
            // If an element with the same ID is present
            if (contingencies.stream().anyMatch(cracContingency -> cracContingency.getId().equals(contingency.getId()))) {
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
            contingencies.add(new ComplexContingency(contingency.getId(), contingency.getName(), networkElementsFromInternalSet));
        }
    }

    public final Set<State> getStates() {
        return states;
    }

    public final State getState(String id) {
        return states.stream().filter(state -> state.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    @JsonIgnore
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

    public State addState(Contingency contingency, Instant instant) {
        State state;
        if (contingency != null) {
            if (contingencies.contains(contingency) && instants.contains(instant)) {
                state = new SimpleState(Optional.of(getContingency(contingency.getId())), getInstant(instant.getId()));
            } else {
                throw new FaraoException(format(ADD_ELEMENTS_TO_CRAC_ERROR_MESSAGE, contingency.getId(), instant.getId()));
            }
        } else {
            if (instants.contains(instant)) {
                state = new SimpleState(Optional.empty(), getInstant(instant.getId()));
            } else {
                throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, instant.getId()));
            }
        }
        states.add(state);
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
        states.add(state);
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
    public Cnec getCnec(String id) {
        return cnecs.stream().filter(cnec -> cnec.getId().equals(id)).findFirst().orElse(null);
    }

    @JsonSerialize(contentUsing = SimpleCnecSerializer.class)
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

    public Cnec addCnec(String id, NetworkElement networkElement, Set<AbstractThreshold> abstractThresholds, State state) {
        if (!networkElements.contains(networkElement) || !states.contains(state)) {
            throw new FaraoException(format(ADD_ELEMENTS_TO_CRAC_ERROR_MESSAGE, networkElement.getId(), state.getId()));
        }
        Cnec cnec = new SimpleCnec(id, networkElement, abstractThresholds, state);
        cnecs.add(cnec);
        return cnec;
    }

    public Cnec addCnec(String id, String name, String networkElementId, Set<AbstractThreshold> abstractThresholds, String stateId) {
        if (getNetworkElement(networkElementId) == null || getState(stateId) == null) {
            throw new FaraoException(format(ADD_ELEMENTS_TO_CRAC_ERROR_MESSAGE, networkElementId, stateId));
        }
        Cnec cnec = new SimpleCnec(id, name, getNetworkElement(networkElementId), abstractThresholds, getState(stateId));
        cnecs.add(cnec);
        return cnec;
    }

    public Cnec addCnec(String id, String networkElementId, Set<AbstractThreshold> abstractThresholds, String stateId) {
        return this.addCnec(id, id, networkElementId, abstractThresholds, stateId);
    }

    @Override
    public void addCnec(Cnec cnec) {
        addState(cnec.getState());
        NetworkElement networkElement = addNetworkElement(cnec.getNetworkElement());

        // add cnec
        cnecs.add(((SimpleCnec) cnec).copy(networkElement, getState(cnec.getState().getId())));

        // add extensions
        if (!cnec.getExtensions().isEmpty()) {
            Cnec cnecInCrac = getCnec(cnec.getId());
            ExtensionsHandler.getCnecExtensionSerializers().addExtensions(cnecInCrac, cnec.getExtensions());
        }
    }

    @JsonSerialize(contentUsing = RangeActionSerializer.class)
    @Override
    public Set<RangeAction> getRangeActions() {
        return rangeActions;
    }

    @JsonSerialize(contentUsing = NetworkActionSerializer.class)
    @Override
    public Set<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    public void addNetworkAction(NetworkAction<?> networkAction) {
        networkAction.getUsageRules().forEach(usageRule -> addState(usageRule.getState()));
        networkAction.getNetworkElements().forEach(this::addNetworkElement);
        networkActions.add(networkAction);
    }

    public void addRangeAction(RangeAction<?> rangeAction) {
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
                .filter(rangeAction -> rangeAction.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void synchronize(Network network) {
        if (isSynchronized) {
            throw new AlreadySynchronizedException(format("Crac %s has already been synchronized", getId()));
        }
        cnecs.forEach(cnec -> cnec.synchronize(network));
        rangeActions.forEach(rangeAction -> rangeAction.synchronize(network));
        isSynchronized = true;
    }

    @Override
    public void desynchronize() {
        cnecs.forEach(Synchronizable::desynchronize);
        rangeActions.forEach(Synchronizable::desynchronize);
        isSynchronized = false;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }

    @Override
    public void generateValidityReport(Network network) {
        ArrayList<Cnec> absentFromNetworkCnecs = new ArrayList<>();
        getCnecs().forEach(cnec -> {
            if (network.getBranch(cnec.getNetworkElement().getId()) == null) {
                absentFromNetworkCnecs.add(cnec);
                LOGGER.warn(String.format("Cnec %s with network element [%s] is not present in the network. It is removed from the Crac", cnec.getId(), cnec.getNetworkElement().getId()));
            }
        });
        absentFromNetworkCnecs.forEach(cnec -> cnecs.remove(cnec));
        ArrayList<RangeAction> absentFromNetworkRangeActions = new ArrayList<>();
        for (RangeAction<?> rangeAction: getRangeActions()) {
            rangeAction.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absentFromNetworkRangeActions.add(rangeAction);
                    LOGGER.warn(String.format("Remedial Action %s with network element [%s] is not present in the network. It is removed from the Crac", rangeAction.getId(), networkElement.getId()));
                }
            });
        }
        absentFromNetworkRangeActions.forEach(rangeAction -> rangeActions.remove(rangeAction));

        ArrayList<NetworkAction> absentFromNetworkNetworkActions = new ArrayList<>();
        for (NetworkAction<?> networkAction: getNetworkActions()) {
            networkAction.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absentFromNetworkNetworkActions.add(networkAction);
                    LOGGER.warn(String.format("Remedial Action %s with network element [%s] is not present in the network. It is removed from the Crac", networkAction.getId(), networkElement.getId()));
                }
            });
        }
        absentFromNetworkNetworkActions.forEach(networkAction -> networkActions.remove(networkAction));
        // TODO: remove contingencies that are not present in the network (and states associated...)
    }
}
