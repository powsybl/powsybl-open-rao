/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.action.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RemedialActionAdder;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.cracio.csaprofiles.CsaProfileCrac;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.NcAggregator;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.ElementCombinationConstraintKind;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.RemedialActionKind;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElement;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialAction;
import com.powsybl.openrao.data.cracio.commons.OpenRaoImportException;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialActionDependency;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.SchemeRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.TapPositionAction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CsaProfileRemedialActionsCreator {
    private final Crac crac;
    Map<String, ElementaryCreationContext> contextByRaId = new TreeMap<>();
    private final ElementaryActionsHelper elementaryActionsHelper;
    private final NetworkActionCreator networkActionCreator;
    private final PstRangeActionCreator pstRangeActionCreator;
    private final Set<RemedialAction> nativeRemedialActions;

    public CsaProfileRemedialActionsCreator(Crac crac, Network network, CsaProfileCrac nativeCrac, CsaProfileCracCreationContext cracCreationContext, int spsMaxTimeToImplementThreshold, Set<ElementaryCreationContext> cnecCreationContexts) {
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

    private void createRemedialActions(Set<AssessedElement> nativeAssessedElements, Map<String, Set<AssessedElementWithRemedialAction>> linkedAeWithRa, Map<String, Set<ContingencyWithRemedialAction>> linkedCoWithRa, int spsMaxTimeToImplementThreshold, Set<ElementaryCreationContext> cnecCreationContexts) {
        for (RemedialAction nativeRemedialAction : nativeRemedialActions) {
            List<String> alterations = new ArrayList<>();
            boolean isSchemeRemedialAction = nativeRemedialAction instanceof SchemeRemedialAction;
            try {
                checkKind(nativeRemedialAction, isSchemeRemedialAction);
                if (!nativeRemedialAction.normalAvailable()) {
                    throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, String.format("Remedial action %s will not be imported because normalAvailable is set to false", nativeRemedialAction.mrid()));
                }
                String elementaryActionsAggregatorId = isSchemeRemedialAction ? elementaryActionsHelper.getGridStateAlterationCollection(nativeRemedialAction.mrid()) : nativeRemedialAction.mrid(); // collectionIdIfAutoOrElseRemedialActionId
                RemedialActionType remedialActionType = getRemedialActionType(nativeRemedialAction.mrid(), elementaryActionsAggregatorId, isSchemeRemedialAction);
                RemedialActionAdder<?> remedialActionAdder;
                if (remedialActionType.equals(RemedialActionType.NETWORK_ACTION)) {
                    remedialActionAdder = networkActionCreator.getNetworkActionAdder(elementaryActionsHelper.getTopologyActions(isSchemeRemedialAction), elementaryActionsHelper.getRotatingMachineActions(isSchemeRemedialAction), elementaryActionsHelper.getShuntCompensatorModifications(isSchemeRemedialAction), elementaryActionsHelper.getNativeStaticPropertyRangesPerNativeGridStateAlteration(), nativeRemedialAction.mrid(), elementaryActionsAggregatorId, alterations);
                    fillAndSaveRemedialActionAdderAndContext(nativeAssessedElements, linkedAeWithRa, linkedCoWithRa, spsMaxTimeToImplementThreshold, cnecCreationContexts, nativeRemedialAction, alterations, isSchemeRemedialAction, remedialActionType, remedialActionAdder, nativeRemedialAction.getUniqueName());
                } else {
                    if (elementaryActionsHelper.getTapPositionActions(isSchemeRemedialAction).get(elementaryActionsAggregatorId).size() > 1) {
                        // group TapPositionAction's
                        for (TapPositionAction nativeTapPositionAction : elementaryActionsHelper.getTapPositionActions(isSchemeRemedialAction).get(elementaryActionsAggregatorId)) {
                            try {
                                remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(true, elementaryActionsAggregatorId, nativeTapPositionAction, elementaryActionsHelper.getNativeStaticPropertyRangesPerNativeGridStateAlteration(), nativeTapPositionAction.mrid());
                                fillAndSaveRemedialActionAdderAndContext(nativeAssessedElements, linkedAeWithRa, linkedCoWithRa, spsMaxTimeToImplementThreshold, cnecCreationContexts, nativeRemedialAction, alterations, isSchemeRemedialAction, remedialActionType, remedialActionAdder, createNameFromTapPositionAction(nativeTapPositionAction.mrid(), nativeRemedialAction.operator()));
                            } catch (OpenRaoImportException e) {
                                if (e.getImportStatus().equals(ImportStatus.NOT_FOR_RAO)) {
                                    contextByRaId.put(nativeTapPositionAction.mrid(), StandardElementaryCreationContext.notImported(nativeTapPositionAction.mrid(), null, e.getImportStatus(), e.getMessage()));
                                } else {
                                    throw e;
                                }
                            }
                        }
                    } else {
                        remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(false, elementaryActionsAggregatorId, elementaryActionsHelper.getTapPositionActions(isSchemeRemedialAction).get(elementaryActionsAggregatorId).iterator().next(), elementaryActionsHelper.getNativeStaticPropertyRangesPerNativeGridStateAlteration(), nativeRemedialAction.mrid());
                        fillAndSaveRemedialActionAdderAndContext(nativeAssessedElements, linkedAeWithRa, linkedCoWithRa, spsMaxTimeToImplementThreshold, cnecCreationContexts, nativeRemedialAction, alterations, isSchemeRemedialAction, remedialActionType, remedialActionAdder, nativeRemedialAction.getUniqueName());
                    }
                }

            } catch (OpenRaoImportException e) {
                contextByRaId.put(nativeRemedialAction.mrid(), StandardElementaryCreationContext.notImported(nativeRemedialAction.mrid(), null, e.getImportStatus(), e.getMessage()));
            }
        }
    }

    private String createNameFromTapPositionAction(String tapPositionId, String operator) {
        if (operator != null) {
            return CsaProfileCracUtils.getTsoNameFromUrl(operator) + "-" + tapPositionId;
        } else {
            return tapPositionId;
        }
    }

    private void fillAndSaveRemedialActionAdderAndContext(Set<AssessedElement> nativeAssessedElements, Map<String, Set<AssessedElementWithRemedialAction>> linkedAeWithRa, Map<String, Set<ContingencyWithRemedialAction>> linkedCoWithRa,
                                                          int spsMaxTimeToImplementThreshold, Set<ElementaryCreationContext> cnecCreationContexts, RemedialAction nativeRemedialAction, List<String> alterations,
                                                          boolean isSchemeRemedialAction, RemedialActionType remedialActionType, RemedialActionAdder<?> remedialActionAdder, String remedialActionName) {

        remedialActionAdder.withName(remedialActionName);
        if (nativeRemedialAction.operator() != null) {
            remedialActionAdder.withOperator(CsaProfileCracUtils.getTsoNameFromUrl(nativeRemedialAction.operator()));
        }
        if (nativeRemedialAction.getTimeToImplementInSeconds() != null) {
            remedialActionAdder.withSpeed(nativeRemedialAction.getTimeToImplementInSeconds());
        } else if (isSchemeRemedialAction && remedialActionType == RemedialActionType.PST_RANGE_ACTION) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because an auto PST range action must have a speed defined", nativeRemedialAction.mrid()));
        }

        InstantKind instantKind = getInstantKind(isSchemeRemedialAction, nativeRemedialAction, spsMaxTimeToImplementThreshold);
        crac.getInstants(instantKind).forEach(instant -> addUsageRules(nativeRemedialAction.mrid(), nativeAssessedElements, linkedAeWithRa.getOrDefault(nativeRemedialAction.mrid(), Set.of()), linkedCoWithRa.getOrDefault(nativeRemedialAction.mrid(), Set.of()), cnecCreationContexts, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType));
        remedialActionAdder.add();

        if (alterations.isEmpty()) {
            contextByRaId.put(nativeRemedialAction.mrid(), StandardElementaryCreationContext.imported(nativeRemedialAction.mrid(), null, nativeRemedialAction.mrid(), false, ""));
        } else {
            contextByRaId.put(nativeRemedialAction.mrid(), StandardElementaryCreationContext.imported(nativeRemedialAction.mrid(), null, nativeRemedialAction.mrid(), true, String.join(". ", alterations)));
        }
    }

    private void addUsageRules(String
                                   remedialActionId, Set<AssessedElement> nativeAssessedElements, Set<AssessedElementWithRemedialAction> linkedAssessedElementWithRemedialActions, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, Set<ElementaryCreationContext> cnecCreationContexts, RemedialActionAdder<?>
                                   remedialActionAdder, List<String> alterations, Instant instant,
                               boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
        if (addOnConstraintUsageRules(remedialActionId, nativeAssessedElements, linkedAssessedElementWithRemedialActions, linkedContingencyWithRemedialActions, cnecCreationContexts, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType)) {
            return;
        }
        if (addOnContingencyStateUsageRules(remedialActionId, linkedContingencyWithRemedialActions, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType)) {
            return;
        }
        addOnInstantUsageRules(remedialActionId, remedialActionAdder, instant);
    }

    private boolean addOnConstraintUsageRules(String
                                                  remedialActionId, Set<AssessedElement> nativeAssessedElements, Set<AssessedElementWithRemedialAction> linkedAssessedElementWithRemedialActions, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, Set<ElementaryCreationContext> cnecCreationContexts, RemedialActionAdder<?>
                                                  remedialActionAdder, List<String> alterations, Instant instant,
                                              boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
        Map<String, AssociationStatus> cnecStatusMap = OnConstraintUsageRuleHelper.processCnecsLinkedToRemedialAction(crac, remedialActionId, nativeAssessedElements, linkedAssessedElementWithRemedialActions, linkedContingencyWithRemedialActions, cnecCreationContexts);
        cnecStatusMap.forEach((cnecId, cnecStatus) -> {
            if (cnecStatus.isValid()) {
                Cnec<?> cnec = crac.getCnec(cnecId);
                UsageMethod usageMethod = getUsageMethod(cnecStatus.elementCombinationConstraintKind(), isSchemeRemedialAction, instant, remedialActionType);
                if (isOnConstraintInstantCoherent(cnec.getState().getInstant(), instant)) {
                    remedialActionAdder.newOnConstraintUsageRule()
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

    private boolean addOnContingencyStateUsageRules(String
                                                        remedialActionId, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, RemedialActionAdder<?>
                                                        remedialActionAdder, List<String> alterations, Instant instant,
                                                    boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
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

    private void addOnInstantUsageRules(String remedialActionId, RemedialActionAdder<?>
        remedialActionAdder, Instant instant) {
        if (instant.isAuto()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action %s will not be imported because no contingency or assessed element is linked to the remedial action and this is nor supported for ARAs".formatted(remedialActionId));
        }
        remedialActionAdder.newOnInstantUsageRule().withInstant(instant.getId()).withUsageMethod(UsageMethod.AVAILABLE).add();
    }

    private static boolean isOnConstraintInstantCoherent(Instant cnecInstant, Instant remedialInstant) {
        return remedialInstant.isAuto() ? cnecInstant.isAuto() : !cnecInstant.comesBefore(remedialInstant);
    }

    private InstantKind getInstantKind(boolean isSchemeRemedialAction, RemedialAction nativeRemedialAction,
                                       int durationLimit) {
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

    private UsageMethod getUsageMethod(ElementCombinationConstraintKind elementCombinationConstraintKind,
                                       boolean isSchemeRemedialAction, Instant instant, RemedialActionType remedialActionType) {
        boolean isPstRangeAuto = instant.isAuto() && remedialActionType == RemedialActionType.PST_RANGE_ACTION;
        return isSchemeRemedialAction || ElementCombinationConstraintKind.INCLUDED.equals(elementCombinationConstraintKind) || isPstRangeAuto ? UsageMethod.FORCED : UsageMethod.AVAILABLE;
    }

    private static void checkKind(RemedialAction nativeRemedialAction, boolean isSchemeRemedialAction) {
        if (isSchemeRemedialAction) {
            if (!RemedialActionKind.CURATIVE.toString().equals(nativeRemedialAction.kind())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because auto remedial action must be of curative kind", nativeRemedialAction.mrid()));
            }
        } else {
            if (!RemedialActionKind.CURATIVE.toString().equals(nativeRemedialAction.kind()) && !RemedialActionKind.PREVENTIVE.toString().equals(nativeRemedialAction.kind())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because remedial action must be of curative or preventive kind", nativeRemedialAction.mrid()));
            }
        }
    }

    private RemedialActionType getRemedialActionType(String remedialActionId, String elementaryActionsAggregatorId,
                                                     boolean isSchemeRemedialAction) {
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
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, String.format("Remedial action %s will not be imported because it has no elementary action", remedialActionId));
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
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action group %s will not be imported because all related RemedialActionDependency must be of the same kind. All RA's depending in that group will be ignored: %s", remedialActionGroup.mrid(), printRaIds(dependingEnabledRemedialActions)));
                    }

                    NetworkAction refNetworkAction = crac.getNetworkAction(refRemedialActionDependency.remedialAction());
                    if (refNetworkAction == null) {
                        standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action group %s will not be imported because the remedial action %s does not exist or not imported. All RA's depending in that group will be ignored: %s", remedialActionGroup.mrid(), refRemedialActionDependency.remedialAction(), printRaIds(dependingEnabledRemedialActions)));
                    }
                    List<UsageRule> onConstraintUsageRules = refNetworkAction.getUsageRules().stream().filter(OnConstraint.class::isInstance).toList();
                    List<UsageRule> onContingencyStateUsageRules = refNetworkAction.getUsageRules().stream().filter(OnContingencyState.class::isInstance).toList();
                    List<UsageRule> onInstantUsageRules = refNetworkAction.getUsageRules().stream().filter(OnInstant.class::isInstance).toList();

                    List<Action> elementaryActions = new ArrayList<>();
                    Set<String> operators = new HashSet<>();

                    dependingEnabledRemedialActions.forEach(remedialActionDependency -> {
                        if (crac.getNetworkAction(remedialActionDependency.remedialAction()) == null) {
                            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action group %s will not be imported because the remedial action %s does not exist or not imported. All RA's depending in that group will be ignored: %s", remedialActionGroup.mrid(), remedialActionDependency.remedialAction(), printRaIds(dependingEnabledRemedialActions)));
                        }
                        if (!refNetworkAction.getUsageRules().equals(crac.getNetworkAction(remedialActionDependency.remedialAction()).getUsageRules())) {
                            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action group %s will not be imported because all depending remedial actions must have the same usage rules. All RA's depending in that group will be ignored: %s", remedialActionGroup.mrid(), printRaIds(dependingEnabledRemedialActions)));
                        }
                        elementaryActions.addAll(crac.getNetworkAction(remedialActionDependency.remedialAction()).getElementaryActions());
                        operators.add(crac.getNetworkAction(remedialActionDependency.remedialAction()).getOperator());
                    });

                    NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(remedialActionGroup.mrid()).withName(groupName);
                    if (operators.size() == 1) {
                        networkActionAdder.withOperator(operators.iterator().next());
                    }
                    addUsageRulesToGroup(onConstraintUsageRules, onContingencyStateUsageRules, onInstantUsageRules, networkActionAdder);
                    addElementaryActionsToGroup(elementaryActions, networkActionAdder);
                    networkActionAdder.add();
                    contextByRaId.put(remedialActionGroup.mrid(), StandardElementaryCreationContext.imported(remedialActionGroup.mrid(), null, remedialActionGroup.mrid(), true, "The RemedialActionGroup with mRID " + remedialActionGroup.mrid() + " was turned into a remedial action from the following remedial actions: " + printRaIds(dependingEnabledRemedialActions)));
                    standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                }

            } catch (OpenRaoImportException e) {
                contextByRaId.put(remedialActionGroup.mrid(), StandardElementaryCreationContext.notImported(remedialActionGroup.mrid(), null, e.getImportStatus(), e.getMessage()));
            }
        });
        return standaloneRasImplicatedIntoAGroup;
    }

    private static void addElementaryActionsToGroup(List<Action> elementaryActions, NetworkActionAdder networkActionAdder) {
        elementaryActions.forEach(ea -> {
            if (ea instanceof GeneratorAction generatorAction) {
                networkActionAdder.newGeneratorAction()
                    .withNetworkElement(generatorAction.getGeneratorId())
                    .withActivePowerValue(generatorAction.getActivePowerValue().getAsDouble())
                    .add();
            } else if (ea instanceof LoadAction loadAction) {
                networkActionAdder.newLoadAction()
                    .withNetworkElement(loadAction.getLoadId())
                    .withActivePowerValue(loadAction.getActivePowerValue().getAsDouble())
                    .add();
            } else if (ea instanceof ShuntCompensatorPositionAction shuntCompensatorPositionAction) {
                networkActionAdder.newShuntCompensatorPositionAction()
                    .withNetworkElement(shuntCompensatorPositionAction.getShuntCompensatorId())
                    .withSectionCount(shuntCompensatorPositionAction.getSectionCount())
                    .add();
            } else if (ea instanceof SwitchAction switchAction) {
                networkActionAdder.newSwitchAction()
                    .withNetworkElement(switchAction.getSwitchId())
                    .withActionType(switchAction.isOpen() ? ActionType.OPEN : ActionType.CLOSE)
                    .add();
            }
        });
    }

    private static void addUsageRulesToGroup(List<UsageRule> onConstraintUsageRules, List<UsageRule> onContingencyStateUsageRules, List<UsageRule> onInstantUsageRules, NetworkActionAdder networkActionAdder) {
        onConstraintUsageRules.forEach(ur -> {
            OnConstraint<?> onConstraintUsageRule = (OnConstraint<?>) ur;
            networkActionAdder.newOnConstraintUsageRule()
                .withInstant(onConstraintUsageRule.getInstant().getId())
                .withUsageMethod(onConstraintUsageRule.getUsageMethod())
                .withCnec(onConstraintUsageRule.getCnec().getId())
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
