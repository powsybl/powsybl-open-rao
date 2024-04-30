/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.CsaProfileCrac;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.NcAggregator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionDependency;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CsaProfileRemedialActionsCreator {
    private final Crac crac;
    private final CsaProfileCracCreationContext cracCreationContext;
    Map<String, CsaProfileElementaryCreationContext> contextByRaId = new TreeMap<>();
    private final ElementaryActionsHelper elementaryActionsHelper;
    private final NetworkActionCreator networkActionCreator;
    private final PstRangeActionCreator pstRangeActionCreator;

    public CsaProfileRemedialActionsCreator(Crac crac, Network network, CsaProfileCracCreationContext cracCreationContext, CsaProfileCrac nativeCrac, int spsMaxTimeToImplementThreshold, Set<CsaProfileElementaryCreationContext> cnecCreationContexts) {
        this.crac = crac;
        this.cracCreationContext = cracCreationContext;
        this.elementaryActionsHelper = new ElementaryActionsHelper(nativeCrac);
        this.networkActionCreator = new NetworkActionCreator(this.crac, network);
        this.pstRangeActionCreator = new PstRangeActionCreator(this.crac, network);
        Map<String, Set<AssessedElementWithRemedialAction>> linkedAeWithRa = new NcAggregator<>(AssessedElementWithRemedialAction::remedialAction).aggregate(nativeCrac.getAssessedElementWithRemedialActions());
        Map<String, Set<ContingencyWithRemedialAction>> linkedCoWithRa = new NcAggregator<>(ContingencyWithRemedialAction::remedialAction).aggregate(nativeCrac.getContingencyWithRemedialActions());
        createRemedialActions(nativeCrac.getAssessedElements(), linkedAeWithRa, linkedCoWithRa, false, spsMaxTimeToImplementThreshold, cnecCreationContexts);
        createRemedialActions(nativeCrac.getAssessedElements(), linkedAeWithRa, linkedCoWithRa, true, spsMaxTimeToImplementThreshold, cnecCreationContexts);
        // standaloneRaIdsImplicatedIntoAGroup contain ids of Ra's depending on a group whether the group is imported or not
        Set<String> standaloneRaIdsImplicatedIntoAGroup = createRemedialActionGroups();
        standaloneRaIdsImplicatedIntoAGroup.forEach(crac::removeRemedialAction);
        standaloneRaIdsImplicatedIntoAGroup.forEach(importedRaId -> contextByRaId.remove(importedRaId));
        this.cracCreationContext.setRemedialActionCreationContexts(new HashSet<>(contextByRaId.values()));
    }

    private void createRemedialActions(Set<AssessedElement> nativeAssessedElements, Map<String, Set<AssessedElementWithRemedialAction>> linkedAeWithRa, Map<String, Set<ContingencyWithRemedialAction>> linkedCoWithRa, boolean isSchemeRemedialAction, int spsMaxTimeToImplementThreshold, Set<CsaProfileElementaryCreationContext> cnecCreationContexts) {
        for (RemedialAction nativeRemedialAction : elementaryActionsHelper.getParentRemedialAction(isSchemeRemedialAction)) {
            List<String> alterations = new ArrayList<>();
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

                Instant instant = defineInstant(isSchemeRemedialAction, nativeRemedialAction, spsMaxTimeToImplementThreshold);
                addUsageRules(nativeRemedialAction.mrid(), nativeAssessedElements, linkedAeWithRa.getOrDefault(nativeRemedialAction.mrid(), Set.of()), linkedCoWithRa.getOrDefault(nativeRemedialAction.mrid(), Set.of()), cnecCreationContexts, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType);
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

    private void addUsageRules(String remedialActionId, Set<AssessedElement> nativeAssessedElements, Set<AssessedElementWithRemedialAction> linkedAssessedElementWithRemedialActions, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, Set<CsaProfileElementaryCreationContext> cnecCreationContexts, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, Instant instant, boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
        if (addOnConstraintUsageRules(remedialActionId, nativeAssessedElements, linkedAssessedElementWithRemedialActions, linkedContingencyWithRemedialActions, cnecCreationContexts, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType)) {
            return;
        }
        if (addOnContingencyStateUsageRules(remedialActionId, linkedContingencyWithRemedialActions, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType)) {
            return;
        }
        addOnInstantUsageRules(remedialActionId, remedialActionAdder, instant);
    }

    private boolean addOnConstraintUsageRules(String remedialActionId, Set<AssessedElement> nativeAssessedElements, Set<AssessedElementWithRemedialAction> linkedAssessedElementWithRemedialActions, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, Set<CsaProfileElementaryCreationContext> cnecCreationContexts, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, Instant instant, boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
        Map<String, AssociationStatus> cnecStatusMap = OnConstraintUsageRuleHelper.processCnecsLinkedToRemedialAction(crac, remedialActionId, nativeAssessedElements, linkedAssessedElementWithRemedialActions, linkedContingencyWithRemedialActions, cnecCreationContexts);
        cnecStatusMap.forEach((cnecId, cnecStatus) -> {
            if (cnecStatus.isValid()) {
                Cnec<?> cnec = crac.getCnec(cnecId);
                UsageMethod usageMethod = getUsageMethod(cnecStatus.elementCombinationConstraintKind(), isSchemeRemedialAction, instant, remedialActionType);
                if (isOnConstraintInstantCoherent(cnec.getState().getInstant(), instant)) {
                    if (cnec instanceof FlowCnec) {
                        remedialActionAdder.newOnFlowConstraintUsageRule()
                            .withInstant(instant.getId())
                            .withFlowCnec(cnecId)
                            .withUsageMethod(usageMethod)
                            .add();
                    } else if (cnec instanceof VoltageCnec) {
                        remedialActionAdder.newOnVoltageConstraintUsageRule()
                            .withInstant(instant.getId())
                            .withVoltageCnec(cnecId)
                            .withUsageMethod(usageMethod)
                            .add();
                    } else {
                        remedialActionAdder.newOnAngleConstraintUsageRule()
                            .withInstant(instant.getId())
                            .withAngleCnec(cnecId)
                            .withUsageMethod(usageMethod)
                            .add();
                    }
                }
            } else {
                alterations.add(cnecStatus.statusDetails());
            }
        });
        return !linkedAssessedElementWithRemedialActions.isEmpty();
    }

    private boolean addOnContingencyStateUsageRules(String remedialActionId, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, Instant instant, boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
        Map<String, AssociationStatus> contingencyStatusMap = OnContingencyStateUsageRuleHelper.processContingenciesLinkedToRemedialAction(crac, remedialActionId, linkedContingencyWithRemedialActions);
        if (instant.isPreventive() && !linkedContingencyWithRemedialActions.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action %s will not be imported because it is linked to a contingency but is not curative".formatted(remedialActionId));
        }
        contingencyStatusMap.forEach((contingencyId, contingencyStatus) -> {
            if (contingencyStatus.isValid()) {
                remedialActionAdder.newOnContingencyStateUsageRule()
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

    private void addOnInstantUsageRules(String remedialActionId, RemedialActionAdder<?> remedialActionAdder, Instant instant) {
        if (instant.isAuto()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action %s will not be imported because no contingency or assessed element is linked to the remedial action and this is nor supported for ARAs".formatted(remedialActionId));
        }
        remedialActionAdder.newOnInstantUsageRule().withInstant(instant.getId()).withUsageMethod(UsageMethod.AVAILABLE).add();
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
        return remedialInstant.isAuto() ? cnecInstant.isAuto() : !cnecInstant.comesBefore(remedialInstant);
    }

    private Instant defineInstant(boolean isSchemeRemedialAction, RemedialAction nativeRemedialAction, int durationLimit) {
        if (CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString().equals(nativeRemedialAction.kind())) {
            return crac.getPreventiveInstant();
        }
        if (isSchemeRemedialAction) {
            return crac.getInstant(InstantKind.AUTO);
        }
        Integer timeToImplement = nativeRemedialAction.getTimeToImplementInSeconds();
        if (timeToImplement == null) {
            return crac.getInstant(InstantKind.CURATIVE);
        }
        return timeToImplement <= durationLimit ? crac.getInstant(InstantKind.AUTO) : crac.getInstant(InstantKind.CURATIVE);
    }

    private UsageMethod getUsageMethod(CsaProfileConstants.ElementCombinationConstraintKind elementCombinationConstraintKind, boolean isSchemeRemedialAction, Instant instant, RemedialActionType remedialActionType) {
        boolean isPstRangeAuto = instant.isAuto() && remedialActionType == RemedialActionType.PST_RANGE_ACTION;
        return isSchemeRemedialAction || CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.equals(elementCombinationConstraintKind) || isPstRangeAuto ? UsageMethod.FORCED : UsageMethod.AVAILABLE;
    }

    private static void checkKind(RemedialAction nativeRemedialAction, boolean isSchemeRemedialAction) {
        if (isSchemeRemedialAction) {
            if (!CsaProfileConstants.RemedialActionKind.CURATIVE.toString().equals(nativeRemedialAction.kind())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeRemedialAction.mrid() + " will not be imported because auto remedial action must be of curative kind");
            }
        } else {
            if (!CsaProfileConstants.RemedialActionKind.CURATIVE.toString().equals(nativeRemedialAction.kind()) && !CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString().equals(nativeRemedialAction.kind())) {
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
                    if (refRemedialActionDependency.remedialAction() == null) {
                        standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.mrid() + " will not be imported because the remedial action " + refRemedialActionDependency.remedialAction() + " does not exist or not imported. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                    }
                    List<UsageRule> onAngleConstraintUsageRules = refNetworkAction.getUsageRules().stream().filter(OnAngleConstraint.class::isInstance).toList();
                    List<UsageRule> onFlowConstraintUsageRules = refNetworkAction.getUsageRules().stream().filter(OnFlowConstraint.class::isInstance).toList();
                    List<UsageRule> onVoltageConstraintUsageRules = refNetworkAction.getUsageRules().stream().filter(OnVoltageConstraint.class::isInstance).toList();
                    List<UsageRule> onContingencyStateUsageRules = refNetworkAction.getUsageRules().stream().filter(OnContingencyState.class::isInstance).toList();
                    List<UsageRule> onInstantUsageRules = refNetworkAction.getUsageRules().stream().filter(OnInstant.class::isInstance).toList();

                    List<ElementaryAction> injectionSetpoints = new ArrayList<>();
                    List<ElementaryAction> pstSetPoints = new ArrayList<>();
                    List<ElementaryAction> topologicalActions = new ArrayList<>();
                    Set<String> operators = new HashSet<>();

                    dependingEnabledRemedialActions.forEach(remedialActionDependency -> {
                        if (crac.getNetworkAction(remedialActionDependency.remedialAction()) == null) {
                            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.mrid() + " will not be imported because the remedial action " + remedialActionDependency.remedialAction() + " does not exist or not imported. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                        }
                        if (!refNetworkAction.getUsageRules().equals(crac.getNetworkAction(remedialActionDependency.remedialAction()).getUsageRules())) {
                            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.mrid() + " will not be imported because all depending the remedial actions must have the same usage rules. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
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
                    addUsageRulesToGroup(onAngleConstraintUsageRules, onFlowConstraintUsageRules, onVoltageConstraintUsageRules, onContingencyStateUsageRules, onInstantUsageRules, injectionSetpoints, pstSetPoints, topologicalActions, networkActionAdder);
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

    private static void addUsageRulesToGroup(List<UsageRule> onAngleConstraintUsageRules, List<UsageRule> onFlowConstraintUsageRules, List<UsageRule> onVoltageConstraintUsageRules, List<UsageRule> onContingencyStateUsageRules, List<UsageRule> onInstantUsageRules, List<ElementaryAction> injectionSetpoints, List<ElementaryAction> pstSetPoints, List<ElementaryAction> topologicalActions, NetworkActionAdder networkActionAdder) {
        onAngleConstraintUsageRules.forEach(ur -> {
            OnAngleConstraint onAngleConstraintUsageRule = (OnAngleConstraint) ur;
            networkActionAdder.newOnAngleConstraintUsageRule()
                .withInstant(onAngleConstraintUsageRule.getInstant().getId())
                .withUsageMethod(onAngleConstraintUsageRule.getUsageMethod())
                .withAngleCnec(onAngleConstraintUsageRule.getAngleCnec().getId())
                .add();
        });
        onFlowConstraintUsageRules.forEach(ur -> {
            OnFlowConstraint onFlowConstraintUsageRule = (OnFlowConstraint) ur;
            networkActionAdder.newOnFlowConstraintUsageRule()
                .withInstant(onFlowConstraintUsageRule.getInstant().getId())
                .withUsageMethod(onFlowConstraintUsageRule.getUsageMethod())
                .withFlowCnec(onFlowConstraintUsageRule.getFlowCnec().getId())
                .add();
        });
        onVoltageConstraintUsageRules.forEach(ur -> {
            OnVoltageConstraint onVoltageConstraintUsageRule = (OnVoltageConstraint) ur;
            networkActionAdder.newOnVoltageConstraintUsageRule()
                .withInstant(onVoltageConstraintUsageRule.getInstant().getId())
                .withUsageMethod(onVoltageConstraintUsageRule.getUsageMethod())
                .withVoltageCnec(onVoltageConstraintUsageRule.getVoltageCnec().getId())
                .add();
        });
        onContingencyStateUsageRules.forEach(ur -> {
            OnContingencyState onContingencyStateUsageRule = (OnContingencyState) ur;
            networkActionAdder.newOnContingencyStateUsageRule()
                .withInstant(onContingencyStateUsageRule.getInstant().getId())
                .withUsageMethod(onContingencyStateUsageRule.getUsageMethod())
                .withContingency(onContingencyStateUsageRule.getContingency().getId())
                .add();
        });
        onInstantUsageRules.forEach(ur -> {
            OnInstant onInstantUsageRule = (OnInstant) ur;
            networkActionAdder.newOnInstantUsageRule()
                .withInstant(onInstantUsageRule.getInstant().getId())
                .withUsageMethod(onInstantUsageRule.getUsageMethod())
                .add();
        });

    }

    private static String printRaIds(Set<RemedialActionDependency> dependingEnabledRemedialActions) {
        return dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).sorted(String::compareTo).collect(Collectors.joining(", "));
    }
}
