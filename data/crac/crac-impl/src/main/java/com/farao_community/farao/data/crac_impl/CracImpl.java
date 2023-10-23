/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.*;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.range_action.*;
import com.farao_community.farao.data.crac_api.usage_rule.OnContingencyState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Business object of the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracImpl extends AbstractIdentifiable<Crac> implements Crac {
    private static final String ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE = "Please add %s to crac first.";
    private static final String SAME_ELEMENT_ID_DIFFERENT_NAME_ERROR_MESSAGE = "A network element with the same ID (%s) but a different name already exists.";

    private final Map<String, NetworkElement> networkElements = new HashMap<>();
    private final Map<String, Contingency> contingencies = new HashMap<>();
    private final Map<String, Instant> instants = new HashMap<>();
    private final Map<String, State> states = new HashMap<>();
    private final Map<String, FlowCnec> flowCnecs = new HashMap<>();
    private final Map<String, AngleCnec> angleCnecs = new HashMap<>();
    private final Map<String, VoltageCnec> voltageCnecs = new HashMap<>();
    private final Map<String, PstRangeAction> pstRangeActions = new HashMap<>();
    private final Map<String, HvdcRangeAction> hvdcRangeActions = new HashMap<>();
    private final Map<String, InjectionRangeAction> injectionRangeActions = new HashMap<>();
    private final Map<String, NetworkAction> networkActions = new HashMap<>();

    public CracImpl(String id, String name) {
        super(id, name);
    }

    public CracImpl(String id) {
        this(id, id);
    }

    // ========================================
    // region NetworkElements management
    // ========================================

    Set<NetworkElement> getNetworkElements() {
        return new HashSet<>(networkElements.values());
    }

    NetworkElement getNetworkElement(String id) {
        return networkElements.getOrDefault(id, null);
    }

    /**
     * Removes NetworkElement objects from the Crac, if they are not used within other objects of the Crac.
     * Only NetworkElement objects that are not referenced are removed.
     *
     * @param networkElementIds: IDs of the network elements to remove
     */
    void safeRemoveNetworkElements(Set<String> networkElementIds) {
        networkElementIds.stream()
            .filter(networkElementId -> !isNetworkElementUsedWithinCrac(networkElementId))
            .forEach(networkElements::remove);
    }

    /**
     * Check if a NetworkElement is referenced in the CRAC (ie in a Contingency, a Cnec or a RemedialAction)
     *
     * @param networkElementId: ID of the NetworkElement
     * @return true if the NetworkElement is referenced in a Contingency, a Cnec or a RemedialAction
     */
    private boolean isNetworkElementUsedWithinCrac(String networkElementId) {
        return getContingencies().stream()
            .flatMap(co -> co.getNetworkElements().stream())
            .anyMatch(ne -> ne.getId().equals(networkElementId))
            || getCnecs().stream()
            .map(Cnec::getNetworkElements)
            .flatMap(Set::stream)
            .anyMatch(ne -> ((NetworkElement) ne).getId().equals(networkElementId))
            || getRemedialActions().stream()
            .map(RemedialAction::getNetworkElements)
            .flatMap(Set::stream)
            .anyMatch(ne -> ne.getId().equals(networkElementId));
    }

    /**
     * This method add a network element to the crac internal set and returns a network element of this set.
     * If an element with the same data is already added, the element of the internal set will be returned,
     * otherwise it is created and then returned. An error is thrown when an element with an already
     * existing ID is added with a different name.
     *
     * @param networkElementId:   network element ID as in network files
     * @param networkElementName: network element name for more human readable name
     * @return a network element object that is already defined in the crac
     */
    NetworkElement addNetworkElement(String networkElementId, String networkElementName) {
        String name = (networkElementName != null) ? networkElementName : networkElementId;
        NetworkElement cracNetworkElement = getNetworkElement(networkElementId);
        if (cracNetworkElement == null) {
            cracNetworkElement = new NetworkElementImpl(networkElementId, name);
        } else if (!cracNetworkElement.getName().equals(name)) {
            throw new FaraoException(format(SAME_ELEMENT_ID_DIFFERENT_NAME_ERROR_MESSAGE, networkElementId));
        }
        networkElements.put(networkElementId, cracNetworkElement);
        return cracNetworkElement;
    }

    //endregion
    // ========================================
    // region Contingencies management
    // ========================================

    @Override
    public ContingencyAdder newContingency() {
        return new ContingencyAdderImpl(this);
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
        if (isContingencyUsedWithinCrac(id)) {
            throw new FaraoException(format("Contingency %s is used within a CNEC or an OnContingencyState UsageRule. Please remove all references to the contingency first.", id));
        } else {
            Contingency contingency = contingencies.get(id);
            if (contingency != null) {
                contingencies.remove(id);
                safeRemoveNetworkElements(contingency.getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet()));
                safeRemoveStates(getStates(contingency).stream().map(State::getId).collect(Collectors.toSet()));
            }
        }
    }

    @Override
    public Instant addInstant(String instantId, InstantKind instantKind, String prevInstantId) {
        if (instants.containsKey(instantId)) {
            Instant instant = instants.get(instantId);
            if (instantKind == instant.getInstantKind() && Objects.equals(instants.get(prevInstantId), instant.getPreviousInstant())) {
                return instant;
            } else {
                throw new FaraoException(format("Instant %s is already defined with other arguments", instantId));
            }
        }
        Instant prevInstant = getPrevInstant(prevInstantId);
        Instant instant = new InstantImpl(instantId, instantKind, prevInstant);
        instants.put(instantId, instant);
        return instant;

    }

    private Instant getPrevInstant(String prevInstantId) {
        if (prevInstantId == null) {
            return null;
        }
        Instant prevInstant = instants.get(prevInstantId);
        if (prevInstant == null) {
            throw new FaraoException(format("Previous instant %s has not been defined", prevInstantId));
        }
        return prevInstant;
    }

    @Override
    public Set<Instant> getInstants() {
        return new HashSet<>(instants.values());
    }

    @Override
    public Instant getPreventiveInstant() {
        return instants.get("preventive");
    }

    @Override
    public Instant getUniqueInstant(InstantKind instantKind) {
        Set<Instant> instantsOfKind = getInstants(instantKind);
        if (instantsOfKind.size() != 1) {
            throw new FaraoException(String.format("Crac does not contain exactly one '%s' instant. It contains %d instants of kind %s", instantKind.toString(), instantsOfKind.size(), instantKind));
        }
        return instantsOfKind.stream().findFirst().orElseThrow(
            () -> new FaraoException(String.format("Should not occur as there is only one '%s' instant", instantKind))
        );
    }

    @Override
    public Set<Instant> getInstants(InstantKind instantKind) {
        return instants.values().stream()
            .filter(instant -> instant.getInstantKind().equals(instantKind))
            .collect(Collectors.toSet());
    }

    @Override
    public Instant getInstant(String instantId) {
        if (!instants.containsKey(instantId)) {
            throw new FaraoException(String.format("Instant '%s' has not been defined", instantId));
        }
        return instants.get(instantId);
    }

    void addContingency(Contingency contingency) {
        contingencies.put(contingency.getId(), contingency);
    }

    /**
     * Check if a Contingency is referenced in the CRAC (ie in a Cnec or in a RemedialAction's UsageRule)
     *
     * @param contingencyId: ID of the Contingency
     * @return true if the Contingency is referenced in a Cnec or in a RemedialAction's UsageRule
     */
    private boolean isContingencyUsedWithinCrac(String contingencyId) {
        return getCnecs().stream().anyMatch(cnec -> cnec.getState().getContingency().isPresent()
            && cnec.getState().getContingency().get().getId().equals(contingencyId))
            || getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(Set::stream)
            .anyMatch(usageMethod -> (usageMethod instanceof OnContingencyStateImpl onContingencyState)
                && onContingencyState.getContingency().getId().equals(contingencyId));
    }

    //endregion
    // ========================================
    // region States management
    // ========================================

    @Override
    public final Set<State> getStates() {
        return new HashSet<>(states.values());
    }

    @Override
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
    public Set<State> getStates(String instantId) {
        return states.values().stream()
            .filter(state -> state.getInstant().getId().equals(instantId))
            .collect(Collectors.toSet());
    }

    @Override
    public State getState(Contingency contingency, String instantId) {
        Objects.requireNonNull(contingency, "Contingency must not be null when getting a state.");
        return states.get(contingency.getId() + " - " + instantId);
    }

    State addPreventiveState(String instantId) {
        if (getPreventiveState() != null) {
            return getPreventiveState();
        } else {
            State state = new PreventiveState(getInstant(instantId));
            states.put(state.getId(), state);
            return state;
        }
    }

    State addState(Contingency contingency, String instantId) {
        Objects.requireNonNull(contingency, "Contingency must not be null when adding a state.");
        Instant instant = getInstant(instantId);
        if (instant.getInstantKind().equals(InstantKind.PREVENTIVE)) {
            throw new FaraoException("Impossible to add a preventive state with a contingency.");
        }
        if (getState(contingency, instantId) != null) {
            return getState(contingency, instantId);
        } else {
            if (!contingencies.containsKey(contingency.getId())) {
                throw new FaraoException(format(ADD_ELEMENT_TO_CRAC_ERROR_MESSAGE, contingency.getId()));
            }
            State state = new PostContingencyState(getContingency(contingency.getId()), instant);
            states.put(state.getId(), state);
            return state;
        }
    }

    /**
     * Removes State objects from the Crac, if they are not used within other objects of the Crac
     * Only State objects that are not referenced are removed.
     *
     * @param stateIds: IDs of the States to remove
     */
    void safeRemoveStates(Set<String> stateIds) {
        stateIds.stream()
            .filter(stateId -> !isStateUsedWithinCrac(stateId))
            .forEach(states::remove);
    }

    /**
     * Check if a State is referenced in the CRAC (ie in a Cnec or a RemedialAction's UsageRule)
     *
     * @param stateId: ID of the State
     * @return true if the State is referenced in a Cnec or a RemedialAction's UsageRule
     */
    private boolean isStateUsedWithinCrac(String stateId) {
        return getCnecs().stream()
            .anyMatch(cnec -> cnec.getState().getId().equals(stateId))
            || getRemedialActions().stream()
            .map(RemedialAction::getUsageRules)
            .flatMap(Set::stream)
            .anyMatch(ur -> ur instanceof OnContingencyState onContingencyState && onContingencyState.getState().getId().equals(stateId));
    }

    //endregion
    // ========================================
    // region Cnec management
    // ========================================

    @Override
    public FlowCnecAdder newFlowCnec() {
        return new FlowCnecAdderImpl(this);
    }

    @Override
    public AngleCnecAdder newAngleCnec() {
        return new AngleCnecAdderImpl(this);
    }

    @Override
    public VoltageCnecAdder newVoltageCnec() {
        return new VoltageCnecAdderImpl(this);
    }

    @Override
    public Set<Cnec> getCnecs() {
        Set<Cnec> cnecs = new HashSet<>();
        cnecs.addAll(getFlowCnecs());
        cnecs.addAll(getAngleCnecs());
        cnecs.addAll(getVoltageCnecs());
        return cnecs;
    }

    @Override
    public Set<Cnec> getCnecs(State state) {
        Set<Cnec> cnecs = new HashSet<>();
        cnecs.addAll(getFlowCnecs(state));
        cnecs.addAll(getAngleCnecs(state));
        cnecs.addAll(getVoltageCnecs(state));
        return cnecs;
    }

    @Override
    public Cnec getCnec(String cnecId) {
        if (flowCnecs.containsKey(cnecId)) {
            return getFlowCnec(cnecId);
        } else if (angleCnecs.containsKey(cnecId)) {
            return getAngleCnec(cnecId);
        } else if (voltageCnecs.containsKey(cnecId)) {
            return getVoltageCnec(cnecId);
        }
        return null;
    }

    /**
     * Find a BranchCnec by its id, returns null if the BranchCnec does not exists
     *
     * @deprecated consider using getCnec() or getFlowCnec() instead
     */
    @Override
    @Deprecated(since = "3.0.0")
    public BranchCnec getBranchCnec(String id) {
        return getFlowCnec(id);
    }

    /**
     * Gather all the BranchCnecs present in the Crac. It returns a set because Cnecs
     * must not be duplicated and there is no defined order for Cnecs.
     *
     * @deprecated consider using getCnecs() or getFlowCnecs() instead
     */
    @Override
    @Deprecated(since = "3.0.0")
    public Set<BranchCnec> getBranchCnecs() {
        return new HashSet<>(flowCnecs.values());
    }

    /**
     * Gather all the BranchCnecs of a specified State. It returns a set because Cnecs
     * must not be duplicated and there is no defined order for Cnecs.
     *
     * @deprecated consider using getCnecs() or getFlowCnecs() instead
     */
    @Override
    @Deprecated(since = "3.0.0")
    public Set<BranchCnec> getBranchCnecs(State state) {
        return new HashSet<>(getFlowCnecs(state));
    }

    @Override
    public FlowCnec getFlowCnec(String flowCnecId) {
        return flowCnecs.get(flowCnecId);
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return new HashSet<>(flowCnecs.values());
    }

    @Override
    public Set<FlowCnec> getFlowCnecs(State state) {
        return flowCnecs.values().stream()
            .filter(cnec -> cnec.getState().equals(state))
            .collect(Collectors.toSet());
    }

    @Override
    public AngleCnec getAngleCnec(String angleCnecId) {
        return angleCnecs.get(angleCnecId);
    }

    @Override
    public Set<AngleCnec> getAngleCnecs() {
        return new HashSet<>(angleCnecs.values());
    }

    @Override
    public Set<AngleCnec> getAngleCnecs(State state) {
        return angleCnecs.values().stream()
            .filter(cnec -> cnec.getState().equals(state))
            .collect(Collectors.toSet());
    }

    @Override
    public VoltageCnec getVoltageCnec(String voltageCnecId) {
        return voltageCnecs.get(voltageCnecId);
    }

    @Override
    public Set<VoltageCnec> getVoltageCnecs() {
        return new HashSet<>(voltageCnecs.values());
    }

    @Override
    public Set<VoltageCnec> getVoltageCnecs(State state) {
        return voltageCnecs.values().stream()
            .filter(cnec -> cnec.getState().equals(state))
            .collect(Collectors.toSet());
    }

    @Override
    public void removeCnec(String cnecId) {
        removeFlowCnec(cnecId);
        removeAngleCnec(cnecId);
        removeVoltageCnec(cnecId);
    }

    @Override
    public void removeFlowCnec(String flowCnecId) {
        removeFlowCnecs(Collections.singleton(flowCnecId));
    }

    @Override
    public void removeFlowCnecs(Set<String> flowCnecsIds) {
        Set<FlowCnec> flowCnecsToRemove = flowCnecsIds.stream().map(flowCnecs::get).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> networkElementsToRemove = flowCnecsToRemove.stream().map(cnec -> cnec.getNetworkElement().getId()).collect(Collectors.toSet());
        Set<String> statesToRemove = flowCnecsToRemove.stream().map(Cnec::getState).map(State::getId).collect(Collectors.toSet());
        flowCnecsToRemove.forEach(flowCnecToRemove ->
            flowCnecs.remove(flowCnecToRemove.getId())
        );
        safeRemoveNetworkElements(networkElementsToRemove);
        safeRemoveStates(statesToRemove);
    }

    @Override
    public void removeAngleCnec(String angleCnecId) {
        removeAngleCnecs(Collections.singleton(angleCnecId));
    }

    @Override
    public void removeAngleCnecs(Set<String> angleCnecsIds) {
        Set<AngleCnec> angleCnecsToRemove = angleCnecsIds.stream().map(angleCnecs::get).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> networkElementsToRemove = angleCnecsToRemove.stream().map(Cnec::getNetworkElements)
            .flatMap(Set::stream).map(Identifiable::getId).collect(Collectors.toSet());
        Set<String> statesToRemove = angleCnecsToRemove.stream().map(Cnec::getState).map(State::getId).collect(Collectors.toSet());
        angleCnecsToRemove.forEach(angleCnecToRemove ->
            angleCnecs.remove(angleCnecToRemove.getId())
        );
        safeRemoveNetworkElements(networkElementsToRemove);
        safeRemoveStates(statesToRemove);
    }

    @Override
    public void removeVoltageCnec(String voltageCnecId) {
        removeVoltageCnecs(Collections.singleton(voltageCnecId));
    }

    @Override
    public void removeVoltageCnecs(Set<String> voltageCnecsIds) {
        Set<VoltageCnec> voltageCnecsToRemove = voltageCnecsIds.stream().map(voltageCnecs::get).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> networkElementsToRemove = voltageCnecsToRemove.stream().map(cnec -> cnec.getNetworkElement().getId()).collect(Collectors.toSet());
        Set<String> statesToRemove = voltageCnecsToRemove.stream().map(Cnec::getState).map(State::getId).collect(Collectors.toSet());
        voltageCnecsToRemove.forEach(voltageCnecToRemove ->
            voltageCnecs.remove(voltageCnecToRemove.getId())
        );
        safeRemoveNetworkElements(networkElementsToRemove);
        safeRemoveStates(statesToRemove);
    }

    void addFlowCnec(FlowCnec flowCnec) {
        flowCnecs.put(flowCnec.getId(), flowCnec);
    }

    void addAngleCnec(AngleCnec angleCnec) {
        angleCnecs.put(angleCnec.getId(), angleCnec);
    }

    void addVoltageCnec(VoltageCnec voltageCnec) {
        voltageCnecs.put(voltageCnec.getId(), voltageCnec);
    }

    // endregion
    // ========================================
    // region RemedialActions management
    // ========================================

    @Override
    public Set<RemedialAction<?>> getRemedialActions() {
        Set<RemedialAction<?>> remedialActions = new HashSet<>();
        remedialActions.addAll(pstRangeActions.values());
        remedialActions.addAll(hvdcRangeActions.values());
        remedialActions.addAll(injectionRangeActions.values());
        remedialActions.addAll(networkActions.values());
        return remedialActions;
    }

    @Override
    public RemedialAction<?> getRemedialAction(String remedialActionId) {
        RemedialAction<?> remedialAction = getNetworkAction(remedialActionId);
        if (!Objects.isNull(remedialAction)) {
            return remedialAction;
        } else {
            return getRangeAction(remedialActionId);
        }
    }

    @Override
    public void removeRemedialAction(String remedialActionId) {
        removeRangeAction(remedialActionId);
        removeNetworkAction(remedialActionId);
    }

    private Set<State> getAssociatedStates(RemedialAction<?> remedialAction) {
        return remedialAction.getUsageRules().stream()
            .filter(OnContingencyState.class::isInstance)
            .map(ur -> ((OnContingencyState) ur).getState())
            .collect(Collectors.toSet());
    }

    // endregion
    // ========================================
    // region RangeAction management
    // ========================================

    @Override
    public PstRangeActionAdder newPstRangeAction() {
        return new PstRangeActionAdderImpl(this);
    }

    @Override
    public HvdcRangeActionAdder newHvdcRangeAction() {
        return new HvdcRangeActionAdderImpl(this);
    }

    @Override
    public InjectionRangeActionAdder newInjectionRangeAction() {
        return new InjectionRangeActionAdderImpl(this);
    }

    @Override
    public Set<PstRangeAction> getPstRangeActions() {
        return new HashSet<>(pstRangeActions.values());
    }

    @Override
    public Set<HvdcRangeAction> getHvdcRangeActions() {
        return new HashSet<>(hvdcRangeActions.values());
    }

    @Override
    public Set<InjectionRangeAction> getInjectionRangeActions() {
        return new HashSet<>(injectionRangeActions.values());
    }

    @Override
    public PstRangeAction getPstRangeAction(String pstRangeActionId) {
        return pstRangeActions.get(pstRangeActionId);
    }

    @Override
    public HvdcRangeAction getHvdcRangeAction(String hvdcRangeActionId) {
        return hvdcRangeActions.get(hvdcRangeActionId);
    }

    @Override
    public InjectionRangeAction getInjectionRangeAction(String injectionRangActionId) {
        return injectionRangeActions.get(injectionRangActionId);
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        Set<RangeAction<?>> rangeActionsSet = new HashSet<>(pstRangeActions.values());
        rangeActionsSet.addAll(hvdcRangeActions.values());
        rangeActionsSet.addAll(injectionRangeActions.values());
        return rangeActionsSet;
    }

    @Override
    public Set<RangeAction<?>> getRangeActions(State state, UsageMethod... usageMethods) {
        Set<RangeAction<?>> pstRangeActionsSet = pstRangeActions.values().stream()
            .filter(rangeAction -> Arrays.stream(usageMethods).anyMatch(usageMethod -> rangeAction.getUsageMethod(state).equals(usageMethod)))
            .collect(Collectors.toSet());
        Set<RangeAction<?>> hvdcRangeActionsSet = hvdcRangeActions.values().stream()
            .filter(rangeAction -> Arrays.stream(usageMethods).anyMatch(usageMethod -> rangeAction.getUsageMethod(state).equals(usageMethod)))
            .collect(Collectors.toSet());
        Set<RangeAction<?>> injectionRangeActionSet = injectionRangeActions.values().stream()
            .filter(rangeAction -> Arrays.stream(usageMethods).anyMatch(usageMethod -> rangeAction.getUsageMethod(state).equals(usageMethod)))
            .collect(Collectors.toSet());
        Set<RangeAction<?>> rangeActionsSet = new HashSet<>(pstRangeActionsSet);
        rangeActionsSet.addAll(hvdcRangeActionsSet);
        rangeActionsSet.addAll(injectionRangeActionSet);
        return rangeActionsSet;
    }

    @Override
    public Set<RangeAction<?>> getPotentiallyAvailableRangeActions(State state) {
        return getRangeActions(state, UsageMethod.AVAILABLE, UsageMethod.FORCED, UsageMethod.TO_BE_EVALUATED);
    }

    @Override
    public RangeAction<?> getRangeAction(String id) {
        if (pstRangeActions.get(id) != null) {
            return pstRangeActions.get(id);
        } else if (hvdcRangeActions.get(id) != null) {
            return hvdcRangeActions.get(id);
        } else {
            return injectionRangeActions.get(id);
        }
    }

    public void removeRangeAction(String id) {
        if (pstRangeActions.get(id) != null) {
            removePstRangeAction(id);
        } else if (hvdcRangeActions.get(id) != null) {
            removeHvdcRangeAction(id);
        } else {
            removeInjectionRangeAction(id);
        }
    }

    @Override
    public void removePstRangeAction(String id) {
        PstRangeAction rangeActionToRemove = pstRangeActions.get(id);
        if (Objects.isNull(rangeActionToRemove)) {
            return;
        }

        Set<String> associatedNetworkElementsIds = rangeActionToRemove.getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet());
        Set<String> associatedStatesIds = getAssociatedStates(rangeActionToRemove).stream().map(State::getId).collect(Collectors.toSet());

        pstRangeActions.remove(id);

        safeRemoveNetworkElements(associatedNetworkElementsIds);
        safeRemoveStates(associatedStatesIds);
    }

    @Override
    public void removeHvdcRangeAction(String id) {
        HvdcRangeAction rangeActionToRemove = hvdcRangeActions.get(id);
        if (Objects.isNull(rangeActionToRemove)) {
            return;
        }

        Set<String> associatedNetworkElementsIds = rangeActionToRemove.getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet());
        Set<String> associatedStatesIds = getAssociatedStates(rangeActionToRemove).stream().map(State::getId).collect(Collectors.toSet());

        hvdcRangeActions.remove(id);

        safeRemoveNetworkElements(associatedNetworkElementsIds);
        safeRemoveStates(associatedStatesIds);

    }

    @Override
    public void removeInjectionRangeAction(String id) {
        InjectionRangeAction rangeActionToRemove = injectionRangeActions.get(id);
        if (Objects.isNull(rangeActionToRemove)) {
            return;
        }

        Set<String> associatedNetworkElementsIds = rangeActionToRemove.getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet());
        Set<String> associatedStatesIds = getAssociatedStates(rangeActionToRemove).stream().map(State::getId).collect(Collectors.toSet());

        injectionRangeActions.remove(id);

        safeRemoveNetworkElements(associatedNetworkElementsIds);
        safeRemoveStates(associatedStatesIds);
    }

    void addPstRangeAction(PstRangeAction pstRangeAction) {
        pstRangeActions.put(pstRangeAction.getId(), pstRangeAction);
    }

    void addHvdcRangeAction(HvdcRangeAction hvdcRangeAction) {
        hvdcRangeActions.put(hvdcRangeAction.getId(), hvdcRangeAction);
    }

    void addInjectionRangeAction(InjectionRangeAction injectionRangeAction) {
        injectionRangeActions.put(injectionRangeAction.getId(), injectionRangeAction);
    }

    // endregion
    // ========================================
    // region NetworkAction management
    // ========================================

    @Override
    public Set<NetworkAction> getNetworkActions() {
        return new HashSet<>(networkActions.values());
    }

    @Override
    public Set<NetworkAction> getNetworkActions(State state, UsageMethod... usageMethods) {
        return networkActions.values().stream()
            .filter(networkAction -> Arrays.stream(usageMethods).anyMatch(usageMethod -> networkAction.getUsageMethod(state).equals(usageMethod)))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<NetworkAction> getPotentiallyAvailableNetworkActions(State state) {
        return getNetworkActions(state, UsageMethod.AVAILABLE, UsageMethod.FORCED, UsageMethod.TO_BE_EVALUATED);
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
    public void removeNetworkAction(String id) {
        NetworkAction networkActionToRemove = networkActions.get(id);
        if (Objects.isNull(networkActionToRemove)) {
            return;
        }

        Set<String> associatedNetworkElementsIds = networkActionToRemove.getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet());
        Set<String> associatedStatesIds = getAssociatedStates(networkActionToRemove).stream().map(State::getId).collect(Collectors.toSet());

        networkActions.remove(id);

        safeRemoveNetworkElements(associatedNetworkElementsIds);
        safeRemoveStates(associatedStatesIds);
    }

    void addNetworkAction(NetworkAction networkAction) {
        networkActions.put(networkAction.getId(), networkAction);
    }
    // endregion
}
