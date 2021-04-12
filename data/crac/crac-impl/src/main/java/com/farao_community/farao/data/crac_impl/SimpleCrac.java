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
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
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
    private static final String ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE = "Please add %s to crac first.";
    private static final String SAME_ELEMENT_ID_DIFFERENT_NAME_ERROR_MESSAGE = "A network element with the same ID (%s) but a different name already exists.";

    private final Map<String, NetworkElement> networkElements = new HashMap<>();
    private final Map<String, Contingency> contingencies = new HashMap<>();
    private final Map<String, State> states = new HashMap<>();
    private final Map<String, FlowCnec> flowCnecs = new HashMap<>();
    private final Map<String, RangeAction> rangeActions = new HashMap<>();
    private final Map<String, NetworkAction> networkActions = new HashMap<>();
    private boolean isSynchronized = false;
    private DateTime networkDate = null;

    public SimpleCrac(String id, String name) {
        super(id, name);
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
    public final Set<NetworkElement> getNetworkElements() {
        return new HashSet<>(networkElements.values());
    }

    // TODO : convert to private package
    @Deprecated
    public NetworkElement addNetworkElement(String networkElementId) {
        return addNetworkElement(networkElementId, networkElementId);
    }

    // TODO : convert to private package
    @Deprecated
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
    // TODO : convert to private package
    @Deprecated
    public NetworkElement addNetworkElement(String networkElementId, String networkElementName) {
        NetworkElement cracNetworkElement = getNetworkElement(networkElementId);
        if (cracNetworkElement == null) {
            cracNetworkElement = new NetworkElementImpl(networkElementId, networkElementName);
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

    @Override
    // TODO : convert to private package
    @Deprecated
    public void addContingency(Contingency contingency) {
        contingencies.put(contingency.getId(), contingency);
    }

    public final Set<State> getStates() {
        return new HashSet<>(states.values());
    }

    @Deprecated
    private final State getState(String id) {
        return states.get(id);
    }

    @Override
    @JsonIgnore
    public State getPreventiveState() {
        return states.get("preventive");
    }

    @Override
    public SortedSet<State> getStates(Contingency contingency) {
        Objects.requireNonNull(contingency, "Contingency must not be null when getting states.");
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
        Objects.requireNonNull(contingency, "Contingency must not be null when getting a state.");
        return states.values().stream()
            .filter(state -> state.getInstant() == instant)
            .filter(state -> state.getContingency().isPresent() && state.getContingency().get().getId().equals(contingency.getId()))
            .findAny()
            .orElse(null);
    }

    @Override
    @Deprecated
    public void removeState(String id) {
        states.remove(id);
    }

    State addPreventiveState() {
        if (getPreventiveState() != null) {
            return getPreventiveState();
        } else {
            State state = new PreventiveState();
            states.put(state.getId(), state);
            return state;
        }
    }

    State addState(Contingency contingency, Instant instant) {
        Objects.requireNonNull(contingency, "Contingency must not be null when adding a state.");
        if (instant.equals(Instant.PREVENTIVE)) {
            throw new FaraoException("Impossible to add a preventive state with a contingency.");
        }
        if (getState(contingency, instant) != null) {
            return getState(contingency, instant);
        } else {
            if (!contingencies.containsKey(contingency.getId())) {
                throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, contingency.getId()));
            }
            State state = new PostContingencyState(getContingency(contingency.getId()), instant);
            states.put(state.getId(), state);
            return state;
        }
    }

    private State addState(PostContingencyState postContingencyState) {
        if (getState(postContingencyState.getId()) == null) {
            Optional<Contingency> optContingency = postContingencyState.getContingency();
            if (optContingency.isPresent()) {
                Contingency contingency = optContingency.get();
                if (getContingency(contingency.getId()) == null) {
                    throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, contingency.getId()));
                } else {
                    return addState(contingency, postContingencyState.getInstant());
                }
            } else {
                throw new FaraoException("Post contingency state should always have a contingency.");
            }
        } else {
            return getState(postContingencyState.getId());
        }
    }

    private State addState(State state) {
        if (state instanceof PreventiveState) {
            return addPreventiveState();
        } else if (state instanceof  PostContingencyState) {
            return addState((PostContingencyState) state);
        } else {
            throw new FaraoException(format("Type %s of state is not handled by simple crac.", state.getClass()));
        }
    }

    @Override
    public FlowCnecAdder newFlowCnec() {
        return new FlowCnecAdderImpl(this);
    }

    @Override
    public Set<Cnec> getCnecs() {
        return new HashSet<>(flowCnecs.values());
    }

    @Override
    public Cnec getCnec(String cnecId) {
        return flowCnecs.get(cnecId);
    }

    @Override
    public BranchCnec getBranchCnec(String id) {
        return flowCnecs.get(id);
    }

    @Override
    public Set<BranchCnec> getBranchCnecs() {
        return new HashSet<>(flowCnecs.values());
    }

    @Override
    public FlowCnec getFlowCnec(String flowCnecId) {
        return flowCnecs.get(flowCnecId);
    }

    @JsonSerialize(contentUsing = FlowCnecImplSerializer.class)
    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return new HashSet<>(flowCnecs.values());
    }

    @Override
    public Set<BranchCnec> getBranchCnecs(State state) {
        return flowCnecs.values().stream()
            .filter(cnec -> cnec.getState().equals(state))
            .collect(Collectors.toSet());
    }

    @Override
    public void removeCnec(String cnecId) {
        flowCnecs.remove(cnecId);
    }

    @Deprecated
    public FlowCnec addCnec(String id, String name, String networkElementId, String operator, Set<BranchThreshold> branchThresholds, Contingency contingency, Instant instant, double frm, boolean optimized, boolean monitored) {
        if (getNetworkElement(networkElementId) == null) {
            throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, networkElementId));
        }
        if (!contingencies.containsValue(contingency)) {
            throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, contingency.getId()));
        }
        State state = addState(contingency, instant);
        FlowCnec cnec = new FlowCnecImpl(id, name, getNetworkElement(networkElementId), operator, state, optimized, monitored, branchThresholds, frm);
        flowCnecs.put(id, cnec);
        return cnec;
    }

    @Deprecated
    public FlowCnec addCnec(String id, String name, String networkElementId, String operator, Set<BranchThreshold> branchThresholds, Contingency contingency, Instant instant, double frm) {
        return addCnec(id, name, networkElementId, operator, branchThresholds, contingency, instant, frm, true, false);
    }

    @Deprecated
    public FlowCnec addCnec(String id, String networkElementId, String operator, Set<BranchThreshold> branchThresholds, Contingency contingency, Instant instant) {
        return addCnec(id, id, networkElementId, operator, branchThresholds, contingency, instant, 0);
    }

    @Deprecated
    public FlowCnec addPreventiveCnec(String id, String name, String networkElementId, String operator, Set<BranchThreshold> branchThresholds, double frm, boolean optimized, boolean monitored) {
        if (getNetworkElement(networkElementId) == null) {
            throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, networkElementId));
        }
        State state = addPreventiveState();
        FlowCnec cnec = new FlowCnecImpl(id, name, getNetworkElement(networkElementId), operator, state, optimized, monitored, branchThresholds, frm);
        flowCnecs.put(id, cnec);
        return cnec;
    }

    @Deprecated
    public BranchCnec addPreventiveCnec(String id, String name, String networkElementId, String operator, Set<BranchThreshold> branchThresholds, double frm) {
        return addPreventiveCnec(id, name, networkElementId, operator, branchThresholds, frm, true, false);
    }

    @Deprecated
    public BranchCnec addPreventiveCnec(String id, String networkElementId, String operator, Set<BranchThreshold> branchThresholds) {
        return addPreventiveCnec(id, id, networkElementId, operator, branchThresholds, 0);
    }

    void addFlowCnec(FlowCnec flowCnec) {
        flowCnecs.put(flowCnec.getId(), flowCnec);
    }

    @Deprecated
    @Override
    public void addCnec(Cnec<?> cnec) {
        // add cnec
        if (cnec instanceof FlowCnec) {
            State state = addState(cnec.getState());
            NetworkElement networkElement = addNetworkElement(cnec.getNetworkElement().getId(), cnec.getNetworkElement().getName());
            FlowCnec flowCnec = (FlowCnec) cnec;
            flowCnecs.put(cnec.getId(), flowCnec);

            // add extensions
            if (!cnec.getExtensions().isEmpty()) {
                BranchCnec cnecInCrac = getBranchCnec(cnec.getId());
                ExtensionsHandler.getExtensionsSerializers().addExtensions(cnecInCrac, flowCnec.getExtensions());
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

    private void addStatesForRemedialAction(RemedialAction<?> remedialAction) {
        remedialAction.getNetworkElements().forEach(this::addNetworkElement);
        remedialAction.getUsageRules().forEach(usageRule -> {
            if (usageRule instanceof OnState) {
                addState(((OnState) usageRule).getState());
            } else if (usageRule instanceof FreeToUse) {
                // TODO: Big flaw here, if we add a contingency after the remedial action, it won't be available after it
                if (((FreeToUse) usageRule).getInstant() == Instant.PREVENTIVE) {
                    addPreventiveState();
                } else {
                    contingencies.values().forEach(co -> addState(co, ((FreeToUse) usageRule).getInstant()));
                }
            }
        });
    }

    @Deprecated
    // TODO : convert to private package
    public void addNetworkAction(NetworkAction networkAction) {
        addStatesForRemedialAction(networkAction); // TODO : remove this ?
        // TODO : verify ID is unique
        networkActions.put(networkAction.getId(), networkAction);
    }

    @Deprecated
    // TODO : convert to private package
    public void addRangeAction(RangeAction rangeAction) {
        addStatesForRemedialAction(rangeAction); // TODO : remove this ?
        // TODO : verify ID is unique
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
    public NetworkActionAdder newNetworkAction() {
        return new NetworkActionAdderImpl(this);
    }

    @Override
    @Deprecated
    // TODO : should we keep this ?
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
    @Deprecated
    // TODO : should we keep this ?
    public void removeRangeAction(String id) {
        rangeActions.remove(id);
    }

    @Override
    public void synchronize(Network network) {
        if (isSynchronized) {
            throw new AlreadySynchronizedException(format("Crac %s has already been synchronized", getId()));
        }
        flowCnecs.values().forEach(cnec -> cnec.synchronize(network));
        rangeActions.values().forEach(rangeAction -> rangeAction.synchronize(network));
        contingencies.values().forEach(contingency -> contingency.synchronize(network));
        networkDate = network.getCaseDate();
        isSynchronized = true;
    }

    @Override
    public void desynchronize() {
        flowCnecs.values().forEach(Synchronizable::desynchronize);
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
