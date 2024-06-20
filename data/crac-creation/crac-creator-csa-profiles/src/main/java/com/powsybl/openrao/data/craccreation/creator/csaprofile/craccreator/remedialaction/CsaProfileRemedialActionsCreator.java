/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.CsaProfileCrac;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.NcAggregator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.constants.ElementCombinationConstraintKind;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.constants.RemedialActionKind;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionDependency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.SchemeRemedialAction;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CsaProfileRemedialActionsCreator {
    private final Crac crac;
    Map<String, CsaProfileElementaryCreationContext> contextByRaId = new TreeMap<>();
    private final ElementaryActionsHelper elementaryActionsHelper;
    private final NetworkActionCreator networkActionCreator;
    private final PstRangeActionCreator pstRangeActionCreator;
    private final Set<RemedialAction> nativeRemedialActions;

    public CsaProfileRemedialActionsCreator(Crac crac, Network network, CsaProfileCrac nativeCrac, CsaProfileCracCreationContext cracCreationContext, int spsMaxTimeToImplementThreshold, Set<CsaProfileElementaryCreationContext> cnecCreationContexts) {
        this.crac = crac;
        this.elementaryActionsHelper = new ElementaryActionsHelper(nativeCrac);
        this.networkActionCreator = new NetworkActionCreator(this.crac, network);
        this.pstRangeActionCreator = new PstRangeActionCreator(this.crac, network);
        Map<String, Set<AssessedElementWithRemedialAction>> linkedAeWithRa = new NcAggregator<>(AssessedElementWithRemedialAction::remedialAction).aggregate(nativeCrac.getAssessedElementWithRemedialActions());
        Map<String, Set<ContingencyWithRemedialAction>> linkedCoWithRa = new NcAggregator<>(ContingencyWithRemedialAction::remedialAction).aggregate(nativeCrac.getContingencyWithRemedialActions());
        this.nativeRemedialActions = new HashSet<>();
        this.nativeRemedialActions.addAll(nativeCrac.getGridStateAlterationRemedialActions());
        this.nativeRemedialActions.addAll(nativeCrac.getSchemeRemedialActions());
        createRemedialActions(nativeCrac.getAssessedElements(), linkedAeWithRa, linkedCoWithRa, spsMaxTimeToImplementThreshold, cnecCreationContexts);
        // standaloneRaIdsImplicatedIntoAGroup contain ids of Ra's depending on a group whether the group is imported or not
        Set<String> standaloneRaIdsImplicatedIntoAGroup = createRemedialActionGroups();
        standaloneRaIdsImplicatedIntoAGroup.forEach(crac::removeRemedialAction);
        standaloneRaIdsImplicatedIntoAGroup.forEach(importedRaId -> contextByRaId.remove(importedRaId));
        cracCreationContext.setRemedialActionCreationContexts(new HashSet<>(contextByRaId.values()));
    }

    private void createRemedialActions(Set<AssessedElement> nativeAssessedElements, Map<String, Set<AssessedElementWithRemedialAction>> linkedAeWithRa, Map<String, Set<ContingencyWithRemedialAction>> linkedCoWithRa, int spsMaxTimeToImplementThreshold, Set<CsaProfileElementaryCreationContext> cnecCreationContexts) {
        for (RemedialAction nativeRemedialAction : nativeRemedialActions) {
            List<String> alterations = new ArrayList<>();
            boolean isSchemeRemedialAction = nativeRemedialAction instanceof SchemeRemedialAction;
            try {
                checkKind(nativeRemedialAction, isSchemeRemedialAction);
                if (!nativeRemedialAction.normalAvailable()) {
                    throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "Remedial action " + nativeRemedialAction.mrid() + " will not be imported because normalAvailable is set to false");
                }
                String elementaryActionsAggregatorId = isSchemeRemedialAction ? elementaryActionsHelper.getGridStateAlterationCollection(nativeRemedialAction.mrid()) : nativeRemedialAction.mrid(); // collectionIdIfAutoOrElseRemedialActionId
                RemedialActionType remedialActionType = getRemedialActionType(nativeRemedialAction.mrid(), elementaryActionsAggregatorId, isSchemeRemedialAction);
                RemedialActionAdder<?> remedialActionAdder = getRemedialActionAdder(nativeRemedialAction.mrid(), elementaryActionsAggregatorId, remedialActionType, isSchemeRemedialAction, alterations);

                remedialActionAdder.withName(nativeRemedialAction.getUniqueName());
                if (nativeRemedialAction.operator() != null) {
                    remedialActionAdder.withOperator(CsaProfileCracUtils.getTsoNameFromUrl(nativeRemedialAction.operator()));
                }
                if (nativeRemedialAction.getTimeToImplementInSeconds() != null) {
                    remedialActionAdder.withSpeed(nativeRemedialAction.getTimeToImplementInSeconds());
                } else if (isSchemeRemedialAction && remedialActionType == RemedialActionType.PST_RANGE_ACTION) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeRemedialAction.mrid() + " will not be imported because an auto PST range action must have a speed defined");
                }

                InstantKind instantKind = getInstantKind(isSchemeRemedialAction, nativeRemedialAction, spsMaxTimeToImplementThreshold);
                crac.getInstants(instantKind).forEach(instant -> addTriggerConditions(nativeRemedialAction.mrid(), nativeAssessedElements, linkedAeWithRa.getOrDefault(nativeRemedialAction.mrid(), Set.of()), linkedCoWithRa.getOrDefault(nativeRemedialAction.mrid(), Set.of()), cnecCreationContexts, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType));
                remedialActionAdder.add();

                if (alterations.isEmpty()) {
                    contextByRaId.put(nativeRemedialAction.mrid(), CsaProfileElementaryCreationContext.imported(nativeRemedialAction.mrid(), nativeRemedialAction.mrid(), nativeRemedialAction.getUniqueName(), "", false));
                } else {
                    contextByRaId.put(nativeRemedialAction.mrid(), CsaProfileElementaryCreationContext.imported(nativeRemedialAction.mrid(), nativeRemedialAction.mrid(), nativeRemedialAction.getUniqueName(), String.join(". ", alterations), true));
                }

            } catch (OpenRaoImportException e) {
                contextByRaId.put(nativeRemedialAction.mrid(), CsaProfileElementaryCreationContext.notImported(nativeRemedialAction.mrid(), e.getImportStatus(), e.getMessage()));
            }
        }
    }

    private void addTriggerConditions(String remedialActionId, Set<AssessedElement> nativeAssessedElements, Set<AssessedElementWithRemedialAction> linkedAssessedElementWithRemedialActions, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, Set<CsaProfileElementaryCreationContext> cnecCreationContexts, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, Instant instant, boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
        if (addOnConstraintTriggerConditions(remedialActionId, nativeAssessedElements, linkedAssessedElementWithRemedialActions, linkedContingencyWithRemedialActions, cnecCreationContexts, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType)) {
            return;
        }
        if (addOnContingencyStateTriggerConditions(remedialActionId, linkedContingencyWithRemedialActions, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType)) {
            return;
        }
        addOnInstantTriggerConditions(remedialActionId, remedialActionAdder, instant);
    }

    private boolean addOnConstraintTriggerConditions(String remedialActionId, Set<AssessedElement> nativeAssessedElements, Set<AssessedElementWithRemedialAction> linkedAssessedElementWithRemedialActions, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, Set<CsaProfileElementaryCreationContext> cnecCreationContexts, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, Instant instant, boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
        Map<String, AssociationStatus> cnecStatusMap = OnConstraintTriggerConditionHelper.processCnecsLinkedToRemedialAction(crac, remedialActionId, nativeAssessedElements, linkedAssessedElementWithRemedialActions, linkedContingencyWithRemedialActions, cnecCreationContexts);
        cnecStatusMap.forEach((cnecId, cnecStatus) -> {
            if (cnecStatus.isValid()) {
                Cnec<?> cnec = crac.getCnec(cnecId);
                UsageMethod usageMethod = getUsageMethod(cnecStatus.elementCombinationConstraintKind(), isSchemeRemedialAction, instant, remedialActionType);
                if (isOnConstraintInstantCoherent(cnec.getState().getInstant(), instant)) {
                    remedialActionAdder.newTriggerCondition()
                        .withInstant(instant.getId())
                        .withCnec(cnecId)
                        .withUsageMethod(usageMethod)
                        .add();
                }
            } else {
                alterations.add(cnecStatus.statusDetails());
            }
        });
        return !linkedAssessedElementWithRemedialActions.isEmpty();
    }

    private boolean addOnContingencyStateTriggerConditions(String remedialActionId, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, Instant instant, boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
        Map<String, AssociationStatus> contingencyStatusMap = OnContingencyStateTriggerConditionHelper.processContingenciesLinkedToRemedialAction(crac, remedialActionId, linkedContingencyWithRemedialActions);
        if (instant.isPreventive() && !linkedContingencyWithRemedialActions.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action %s will not be imported because it is linked to a contingency but is not curative".formatted(remedialActionId));
        }
        contingencyStatusMap.forEach((contingencyId, contingencyStatus) -> {
            if (contingencyStatus.isValid()) {
                remedialActionAdder.newTriggerCondition()
                    .withInstant(instant.getId())
                    .withContingency(contingencyId)
                    .withUsageMethod(getUsageMethod(contingencyStatus.elementCombinationConstraintKind(), isSchemeRemedialAction, instant, remedialActionType))
                    .add();
            } else {
                alterations.add(contingencyStatus.statusDetails());
            }
        });
        return !linkedContingencyWithRemedialActions.isEmpty();
    }

    private void addOnInstantTriggerConditions(String remedialActionId, RemedialActionAdder<?> remedialActionAdder, Instant instant) {
        if (instant.isAuto()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action %s will not be imported because no contingency or assessed element is linked to the remedial action and this is nor supported for ARAs".formatted(remedialActionId));
        }
        remedialActionAdder.newTriggerCondition().withInstant(instant.getId()).withUsageMethod(UsageMethod.AVAILABLE).add();
    }

    private RemedialActionAdder<?> getRemedialActionAdder(String remedialActionId, String elementaryActionsAggregatorId, RemedialActionType remedialActionType, boolean isSchemeRemedialAction, List<String> alterations) {
        RemedialActionAdder<?> remedialActionAdder;
        if (remedialActionType.equals(RemedialActionType.NETWORK_ACTION)) {
            remedialActionAdder = networkActionCreator.getNetworkActionAdder(elementaryActionsHelper.getTopologyActions(isSchemeRemedialAction), elementaryActionsHelper.getRotatingMachineActions(isSchemeRemedialAction), elementaryActionsHelper.getShuntCompensatorModifications(isSchemeRemedialAction), elementaryActionsHelper.getNativeStaticPropertyRangesPerNativeGridStateAlteration(), remedialActionId, elementaryActionsAggregatorId, alterations);
        } else {
            remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(elementaryActionsHelper.getTapPositionActions(isSchemeRemedialAction), elementaryActionsHelper.getNativeStaticPropertyRangesPerNativeGridStateAlteration(), remedialActionId, elementaryActionsAggregatorId, alterations);
        }
        return remedialActionAdder;
    }

    private static boolean isOnConstraintInstantCoherent(Instant cnecInstant, Instant remedialInstant) {
        // TODO: add test to cover all the possible cases
        return remedialInstant.isAuto() ? cnecInstant.isAuto() : !cnecInstant.comesBefore(remedialInstant);
    }

    private InstantKind getInstantKind(boolean isSchemeRemedialAction, RemedialAction nativeRemedialAction, int durationLimit) {
        if (RemedialActionKind.PREVENTIVE.toString().equals(nativeRemedialAction.kind())) {
            return InstantKind.PREVENTIVE;
        }
        if (isSchemeRemedialAction) {
            return InstantKind.AUTO;
        }
        Integer timeToImplement = nativeRemedialAction.getTimeToImplementInSeconds();
        if (timeToImplement == null) {
            return InstantKind.CURATIVE;
        }
        return timeToImplement <= durationLimit ? InstantKind.AUTO : InstantKind.CURATIVE;
    }

    private UsageMethod getUsageMethod(ElementCombinationConstraintKind elementCombinationConstraintKind, boolean isSchemeRemedialAction, Instant instant, RemedialActionType remedialActionType) {
        boolean isPstRangeAuto = instant.isAuto() && remedialActionType == RemedialActionType.PST_RANGE_ACTION;
        return isSchemeRemedialAction || ElementCombinationConstraintKind.INCLUDED.equals(elementCombinationConstraintKind) || isPstRangeAuto ? UsageMethod.FORCED : UsageMethod.AVAILABLE;
    }

    private static void checkKind(RemedialAction nativeRemedialAction, boolean isSchemeRemedialAction) {
        if (isSchemeRemedialAction) {
            if (!RemedialActionKind.CURATIVE.toString().equals(nativeRemedialAction.kind())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeRemedialAction.mrid() + " will not be imported because auto remedial action must be of curative kind");
            }
        } else {
            if (!RemedialActionKind.CURATIVE.toString().equals(nativeRemedialAction.kind()) && !RemedialActionKind.PREVENTIVE.toString().equals(nativeRemedialAction.kind())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeRemedialAction.mrid() + " will not be imported because remedial action must be of curative or preventive kind");
            }
        }
    }

    private RemedialActionType getRemedialActionType(String remedialActionId, String elementaryActionsAggregatorId, boolean isSchemeRemedialAction) {
        RemedialActionType remedialActionType;
        if (elementaryActionsHelper.getTopologyActions(isSchemeRemedialAction).containsKey(elementaryActionsAggregatorId)) {
            remedialActionType = RemedialActionType.NETWORK_ACTION;
        } else if (elementaryActionsHelper.getRotatingMachineActions(isSchemeRemedialAction).containsKey(elementaryActionsAggregatorId)) {
            remedialActionType = RemedialActionType.NETWORK_ACTION;
        } else if (elementaryActionsHelper.getShuntCompensatorModifications(isSchemeRemedialAction).containsKey(elementaryActionsAggregatorId)) {
            remedialActionType = RemedialActionType.NETWORK_ACTION;
        } else if (elementaryActionsHelper.getTapPositionActions(isSchemeRemedialAction).containsKey(elementaryActionsAggregatorId)) {
            // StaticPropertyRanges not mandatory in case of tapPositionsActions
            remedialActionType = RemedialActionType.PST_RANGE_ACTION;
        } else {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "Remedial action " + remedialActionId + " will not be imported because it has no elementary action");
        }
        return remedialActionType;
    }

    enum RemedialActionType {
        PST_RANGE_ACTION,
        NETWORK_ACTION
    }

    private Set<String> createRemedialActionGroups() {
        Set<String> standaloneRasImplicatedIntoAGroup = new HashSet<>();
        Map<String, Set<RemedialActionDependency>> remedialActionDependenciesByGroup = elementaryActionsHelper.getNativeRemedialActionDependencyPerNativeRemedialActionGroup();
        elementaryActionsHelper.getRemedialActionGroupsPropertyBags().forEach(remedialActionGroup -> {

            String groupName = remedialActionGroup.name() == null ? remedialActionGroup.mrid() : remedialActionGroup.name();
            try {
                Set<RemedialActionDependency> dependingEnabledRemedialActions = remedialActionDependenciesByGroup.getOrDefault(remedialActionGroup.mrid(), Set.of()).stream().filter(RemedialActionDependency::normalEnabled).collect(Collectors.toSet());
                if (!dependingEnabledRemedialActions.isEmpty()) {

                    RemedialActionDependency refRemedialActionDependency = dependingEnabledRemedialActions.iterator().next();
                    if (!dependingEnabledRemedialActions.stream().allMatch(raDependency -> refRemedialActionDependency.kind().equals(raDependency.kind()))) {
                        standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.mrid() + " will not be imported because all related RemedialActionDependency must be of the same kind. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                    }

                    NetworkAction refNetworkAction = crac.getNetworkAction(refRemedialActionDependency.remedialAction());
                    if (refNetworkAction == null) {
                        standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.mrid() + " will not be imported because the remedial action " + refRemedialActionDependency.remedialAction() + " does not exist or not imported. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                    }
                    List<ElementaryAction> injectionSetpoints = new ArrayList<>();
                    List<ElementaryAction> pstSetPoints = new ArrayList<>();
                    List<ElementaryAction> topologicalActions = new ArrayList<>();
                    Set<String> operators = new HashSet<>();

                    dependingEnabledRemedialActions.forEach(remedialActionDependency -> {
                        if (crac.getNetworkAction(remedialActionDependency.remedialAction()) == null) {
                            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.mrid() + " will not be imported because the remedial action " + remedialActionDependency.remedialAction() + " does not exist or not imported. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                        }
                        if (!refNetworkAction.getTriggerConditions().equals(crac.getNetworkAction(remedialActionDependency.remedialAction()).getTriggerConditions())) {
                            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.mrid() + " will not be imported because all depending the remedial actions must have the same trigger conditions. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                        }
                        injectionSetpoints.addAll(crac.getNetworkAction(remedialActionDependency.remedialAction()).getElementaryActions().stream().filter(InjectionSetpoint.class::isInstance).toList());
                        pstSetPoints.addAll(crac.getNetworkAction(remedialActionDependency.remedialAction()).getElementaryActions().stream().filter(PstSetpoint.class::isInstance).toList());
                        topologicalActions.addAll(crac.getNetworkAction(remedialActionDependency.remedialAction()).getElementaryActions().stream().filter(TopologicalAction.class::isInstance).toList());
                        operators.add(crac.getNetworkAction(remedialActionDependency.remedialAction()).getOperator());
                    });

                    NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(remedialActionGroup.mrid()).withName(groupName);
                    if (operators.size() == 1) {
                        networkActionAdder.withOperator(operators.iterator().next());
                    }
                    addTriggerConditionsToGroup(networkActionAdder, refNetworkAction.getTriggerConditions());
                    addElementaryActionsToGroup(injectionSetpoints, pstSetPoints, topologicalActions, networkActionAdder);
                    networkActionAdder.add();
                    contextByRaId.put(remedialActionGroup.mrid(), CsaProfileElementaryCreationContext.imported(remedialActionGroup.mrid(), remedialActionGroup.mrid(), groupName, "The RemedialActionGroup with mRID " + remedialActionGroup.mrid() + " was turned into a remedial action from the following remedial actions: " + printRaIds(dependingEnabledRemedialActions), true));
                    standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                }

            } catch (OpenRaoImportException e) {
                contextByRaId.put(remedialActionGroup.mrid(), CsaProfileElementaryCreationContext.notImported(remedialActionGroup.mrid(), e.getImportStatus(), e.getMessage()));
            }
        });
        return standaloneRasImplicatedIntoAGroup;
    }

    private static void addElementaryActionsToGroup(List<ElementaryAction> injectionSetpoints, List<ElementaryAction> pstSetPoints, List<ElementaryAction> topologicalActions, NetworkActionAdder networkActionAdder) {
        injectionSetpoints.forEach(ea -> {
            InjectionSetpoint injectionSetPoint = (InjectionSetpoint) ea;
            networkActionAdder.newInjectionSetPoint()
                .withNetworkElement(injectionSetPoint.getNetworkElement().getId())
                .withSetpoint(injectionSetPoint.getSetpoint())
                .withUnit(injectionSetPoint.getUnit())
                .add();
        });
        pstSetPoints.forEach(ea -> {
            PstSetpoint pstSetPoint = (PstSetpoint) ea;
            networkActionAdder.newPstSetPoint()
                .withNetworkElement(pstSetPoint.getNetworkElement().getId())
                .withSetpoint(pstSetPoint.getSetpoint())
                .add();
        });
        topologicalActions.forEach(ea -> {
            TopologicalAction topologicalAction = (TopologicalAction) ea;
            networkActionAdder.newTopologicalAction()
                .withNetworkElement(topologicalAction.getNetworkElement().getId())
                .withActionType(topologicalAction.getActionType())
                .add();
        });
    }

    private static void addTriggerConditionsToGroup(NetworkActionAdder networkActionAdder, Set<TriggerCondition> triggerConditions) {
        triggerConditions.forEach(tc -> networkActionAdder.newTriggerCondition()
            .withInstant(tc.getInstant().getId())
            .withContingency(tc.getContingency().isPresent() ? tc.getContingency().get().getId() : null)
            .withCnec(tc.getCnec().isPresent() ? tc.getCnec().get().getId() : null)
            .withCountry(tc.getCountry().isPresent() ? tc.getCountry().get() : null)
            .withUsageMethod(tc.getUsageMethod())
            .add());
    }

    private static String printRaIds(Set<RemedialActionDependency> dependingEnabledRemedialActions) {
        return dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).sorted(String::compareTo).collect(Collectors.joining(", "));
    }
}
