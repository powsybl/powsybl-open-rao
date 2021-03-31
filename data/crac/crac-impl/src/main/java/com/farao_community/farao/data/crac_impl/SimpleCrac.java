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
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.adder.BranchCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_impl.cnec.FlowCnecImpl;
import com.farao_community.farao.data.crac_impl.cnec.adder.FlowCnecAdderImpl;
import com.farao_community.farao.data.crac_impl.json.serializers.FlowCnecImplSerializer;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;

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

    private final List<Instant> sortedInstants;
    private final Map<String, NetworkElement> networkElements = new HashMap<>();
    private final Map<String, Contingency> contingencies = new HashMap<>();
    private final Map<String, State> states = new HashMap<>();
    private final Map<String, BranchCnec> branchCnecs = new HashMap<>();
    private final Map<String, RangeAction> rangeActions = new HashMap<>();
    private final Map<String, NetworkAction> networkActions = new HashMap<>();
    private boolean isSynchronized = false;
    private DateTime networkDate = null;

    public SimpleCrac(String id, String name, Set<Instant> instants) {
        super(id, name);
        // Copy the set in case it is immutable
        Set<Instant> copiedInstants = new HashSet<>(instants);
        // Ignore preventive instant if it is in the set
        copiedInstants.remove(Instant.PREVENTIVE);
        // Sort the set into a list
        this.sortedInstants = copiedInstants.stream().sorted(Comparator.comparingInt(Instant::getOrder)).collect(Collectors.toList());
        // Insert preventive instant at the beginning
        this.sortedInstants.add(0, Instant.PREVENTIVE);
        addPreventiveState();
    }

    @Override
    public DateTime getNetworkDate() {
        return networkDate;
    }

    public void setNetworkDate(DateTime networkDate) {
        this.networkDate = networkDate;
    }

    @Override
    public NetworkElementAdder<Crac> newNetworkElement() {
        return new NetworkElementAdderImpl<>(this);
    }

    @Override
    public final Set<NetworkElement> getNetworkElements() {
        return new HashSet<>(networkElements.values());
    }

    public NetworkElement addNetworkElement(String networkElementId) {
        return addNetworkElement(networkElementId, networkElementId);
    }

    @Override
    public Crac addNetworkElement(NetworkElement networkElement) {
        addNetworkElement(networkElement.getId(), networkElement.getName());
        return this;
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

    @Override
    public final NetworkElement getNetworkElement(String id) {
        return networkElements.getOrDefault(id, null);
    }

    @Override
    public final List<Instant> getInstants() {
        return sortedInstants;
    }

    @Override
    public Set<Contingency> getContingencies() {
        return new HashSet<>(contingencies.values());
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
        return new ContingencyAdderImpl(this);
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

            // Add the contingency itself
            if (contingency instanceof XnodeContingency) {
                // We cannot iterate through an XnodeContingency's network elements before it is synchronized
                contingencies.put(contingency.getId(), contingency);
            } else {
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

            // Add states related to this contingency (preventive instant must be excluded)
            sortedInstants.stream().filter(i -> !i.equals(Instant.PREVENTIVE)).forEach(i -> addState(contingency, i));
        }
    }

    public final Set<State> getStates() {
        return new HashSet<>(states.values());
    }

    public final State getState(String id) {
        return states.get(id);
    }

    @Override
    @JsonIgnore
    public State getPreventiveState() {
        return states.values().stream().filter(state -> state.getInstant().equals(Instant.PREVENTIVE)).findAny().orElse(null);
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
            .filter(state -> state.getInstant().equals(instant))
            .collect(Collectors.toSet());
    }

    @Override
    public State getState(Contingency contingency, Instant instant) {
        if (contingency == null) {
            if (instant.equals(Instant.PREVENTIVE)) {
                return getPreventiveState();
            } else {
                return null;
            }
        }
        return states.values().stream()
            .filter(state -> state.getContingency().isPresent() && state.getInstant().equals(instant))
            .filter(state -> state.getContingency().isPresent() && state.getContingency().get().getId().equals(contingency.getId()))
            .findAny()
            .orElse(null);
    }

    @Override
    public void removeState(String id) {
        states.remove(id);
    }

    private State addState(State state) {
        states.put(state.getId(), state);
        return state;
    }

    public void addPreventiveState() {
        addState(new SimpleState(Optional.empty(), Instant.PREVENTIVE));
    }

    private State addState(Contingency contingency, Instant instant) {
        Objects.requireNonNull(contingency, "Contingency must not be null when adding a state.");
        if (instant.equals(Instant.PREVENTIVE)) {
            throw new FaraoException("Impossible to add a preventive state within the CRAC.");
        }
        if (contingencies.containsKey(contingency.getId())) {
            return addState(new SimpleState(Optional.of(getContingency(contingency.getId())), instant));
        } else {
            throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, contingency.getId()));
        }
    }

    @Override
    public BranchCnecAdder newBranchCnec() {
        return new FlowCnecAdderImpl(this);
    }

    @Override
    public BranchCnec getBranchCnec(String id) {
        return branchCnecs.get(id);
    }

    @JsonSerialize(contentUsing = FlowCnecImplSerializer.class)
    @Override
    public Set<BranchCnec> getBranchCnecs() {
        return new HashSet<>(branchCnecs.values());
    }

    @Override
    public Set<BranchCnec> getBranchCnecs(State state) {
        return branchCnecs.values().stream()
            .filter(cnec -> cnec.getState().equals(state))
            .collect(Collectors.toSet());
    }

    @Override
    public void removeCnec(String cnecId) {
        branchCnecs.remove(cnecId);
    }

    public BranchCnec addCnec(String id, String name, String networkElementId, String operator, Set<BranchThreshold> branchThresholds, String stateId, double frm, boolean optimized, boolean monitored) {
        if (getNetworkElement(networkElementId) == null || getState(stateId) == null) {
            throw new FaraoException(format(ADD_ELEMENTS_TO_CRAC_ERROR_MESSAGE, networkElementId, stateId));
        }
        BranchCnec cnec = new FlowCnecImpl(id, name, getNetworkElement(networkElementId), operator, getState(stateId), optimized, monitored, branchThresholds, frm);
        branchCnecs.put(id, cnec);
        return cnec;
    }

    public BranchCnec addCnec(String id, String name, String networkElementId, String operator, Set<BranchThreshold> branchThresholds, String stateId, double frm) {
        return this.addCnec(id, name, networkElementId, operator, branchThresholds, stateId, frm, true, false);
    }

    public BranchCnec addCnec(String id, String networkElementId, String operator, Set<BranchThreshold> branchThresholds, String stateId) {
        return this.addCnec(id, id, networkElementId, operator, branchThresholds, stateId, 0);
    }

    @Override
    public void addCnec(Cnec<?> cnec) {
        // add cnec
        if (cnec instanceof BranchCnec) {
            if (getState(cnec.getState().getId()) == null) {
                throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, cnec.getState().getId()));
            }
            addNetworkElement(cnec.getNetworkElement());
            NetworkElement networkElement = getNetworkElement(cnec.getNetworkElement().getId());
            BranchCnec branchCnec = (BranchCnec) cnec;

            branchCnecs.put(cnec.getId(), branchCnec.copy(networkElement, getState(cnec.getState().getId())));

            // add extensions
            if (!cnec.getExtensions().isEmpty()) {
                BranchCnec cnecInCrac = getBranchCnec(cnec.getId());
                ExtensionsHandler.getExtensionsSerializers().addExtensions(cnecInCrac, branchCnec.getExtensions());
            }
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
        branchCnecs.values().forEach(cnec -> cnec.synchronize(network));
        rangeActions.values().forEach(rangeAction -> rangeAction.synchronize(network));
        contingencies.values().forEach(contingency -> contingency.synchronize(network));
        addXnodeContingenciesNetworkElements();
        networkDate = network.getCaseDate();
        isSynchronized = true;
    }

    /**
     * This method adds network elements from XnodeContingencies to the crac.
     * For ComplexContingencies, they are added when the contingencies are added to the crac. This is not possible for
     * XnodeContingencies since they have to be synchronized with the network first in order to find the network elements
     * corresponding to the xnodes.
     */
    private void addXnodeContingenciesNetworkElements() {
        contingencies.values().stream().filter(contingency -> contingency instanceof XnodeContingency)
                .forEach(contingency -> contingency.getNetworkElements().forEach(networkElement -> {
                    if (getNetworkElement(networkElement.getId()) == null) {
                        addNetworkElement(networkElement);
                    }
                }));
    }

    @Override
    public void desynchronize() {
        branchCnecs.values().forEach(Synchronizable::desynchronize);
        rangeActions.values().forEach(Synchronizable::desynchronize);
        contingencies.values().forEach(Synchronizable::desynchronize);
        networkDate = null;
        isSynchronized = false;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }
}
