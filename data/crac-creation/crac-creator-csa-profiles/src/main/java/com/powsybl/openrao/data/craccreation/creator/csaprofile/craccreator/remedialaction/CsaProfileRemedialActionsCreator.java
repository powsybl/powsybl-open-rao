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
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;
import java.util.function.Function;
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

    public CsaProfileRemedialActionsCreator(Crac crac, Network network, CsaProfileCracCreationContext cracCreationContext, ElementaryActionsHelper elementaryActionsHelper, int spsMaxTimeToImplementThreshold, PropertyBags assessedElementPropertyBags, PropertyBags assessedElementWithRemedialActions, PropertyBags contingencyWithRemedialActions, Set<CsaProfileElementaryCreationContext> cnecCreationContexts) {
        this.crac = crac;
        this.cracCreationContext = cracCreationContext;
        this.elementaryActionsHelper = elementaryActionsHelper;
        this.networkActionCreator = new NetworkActionCreator(this.crac, network);
        this.pstRangeActionCreator = new PstRangeActionCreator(this.crac, network);
        Map<String, Set<PropertyBag>> linkedAeWithRa = CsaProfileCracUtils.groupPropertyBagsBy(assessedElementWithRemedialActions, CsaProfileConstants.REQUEST_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedCoWithRa = CsaProfileCracUtils.groupPropertyBagsBy(contingencyWithRemedialActions, CsaProfileConstants.REQUEST_REMEDIAL_ACTION);
        createRemedialActions(false, spsMaxTimeToImplementThreshold, assessedElementPropertyBags, linkedAeWithRa, linkedCoWithRa, cnecCreationContexts);
        createRemedialActions(true, spsMaxTimeToImplementThreshold, assessedElementPropertyBags, linkedAeWithRa, linkedCoWithRa, cnecCreationContexts);
        // standaloneRaIdsImplicatedIntoAGroup contain ids of Ra's depending on a group whether the group is imported or not
        Set<String> standaloneRaIdsImplicatedIntoAGroup = createRemedialActionGroups();
        standaloneRaIdsImplicatedIntoAGroup.forEach(crac::removeRemedialAction);
        standaloneRaIdsImplicatedIntoAGroup.forEach(importedRaId -> contextByRaId.remove(importedRaId));
        this.cracCreationContext.setRemedialActionCreationContexts(new HashSet<>(contextByRaId.values()));
    }

    private void createRemedialActions(boolean isSchemeRemedialAction, int spsMaxTimeToImplementThreshold, PropertyBags assessedElementPropertyBags, Map<String, Set<PropertyBag>> linkedAeWithRa, Map<String, Set<PropertyBag>> linkedCoWithRa, Set<CsaProfileElementaryCreationContext> cnecCreationContexts) {
        for (PropertyBag parentRemedialActionPropertyBag : elementaryActionsHelper.getParentRemedialActionPropertyBags(isSchemeRemedialAction)) {
            List<String> alterations = new ArrayList<>();
            String remedialActionId = parentRemedialActionPropertyBag.get(CsaProfileConstants.MRID);
            try {
                checkKind(parentRemedialActionPropertyBag, remedialActionId, isSchemeRemedialAction);
                checkAvailability(parentRemedialActionPropertyBag, remedialActionId);
                String elementaryActionsAggregatorId = isSchemeRemedialAction ? elementaryActionsHelper.getGridStateAlterationCollection(remedialActionId) : remedialActionId; // collectionIdIfAutoOrElseRemedialActionId
                RemedialActionType remedialActionType = getRemedialActionType(remedialActionId, elementaryActionsAggregatorId, isSchemeRemedialAction);
                RemedialActionAdder<?> remedialActionAdder = getRemedialActionAdder(remedialActionId, elementaryActionsAggregatorId, remedialActionType, isSchemeRemedialAction, alterations);

                String nativeRaName = parentRemedialActionPropertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME);
                String tsoName = parentRemedialActionPropertyBag.get(CsaProfileConstants.TSO);
                Optional<String> targetRemedialActionNameOpt = CsaProfileCracUtils.createElementName(nativeRaName, tsoName);
                Optional<Integer> speedOpt = getSpeedOpt(remedialActionType, parentRemedialActionPropertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT), remedialActionId, isSchemeRemedialAction);
                targetRemedialActionNameOpt.ifPresent(remedialActionAdder::withName);
                if (tsoName != null) {
                    remedialActionAdder.withOperator(CsaProfileCracUtils.getTsoNameFromUrl(tsoName));
                }
                speedOpt.ifPresent(remedialActionAdder::withSpeed);

                Instant instant = defineInstant(isSchemeRemedialAction, parentRemedialActionPropertyBag, remedialActionId, spsMaxTimeToImplementThreshold);
                addUsageRules(remedialActionId, assessedElementPropertyBags, linkedAeWithRa.getOrDefault(remedialActionId, Set.of()), linkedCoWithRa.getOrDefault(remedialActionId, Set.of()), cnecCreationContexts, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType);
                remedialActionAdder.add();

                if (alterations.isEmpty()) {
                    contextByRaId.put(remedialActionId, CsaProfileElementaryCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), "", false));
                } else {
                    contextByRaId.put(remedialActionId, CsaProfileElementaryCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), String.join(" ", alterations), true));
                }

            } catch (OpenRaoImportException e) {
                contextByRaId.put(remedialActionId, CsaProfileElementaryCreationContext.notImported(remedialActionId, e.getImportStatus(), e.getMessage()));
            }
        }
    }

    private void addUsageRules(String remedialActionId, PropertyBags assessedElementPropertyBags, Set<PropertyBag> linkedAssessedElementWithRemedialActions, Set<PropertyBag> linkedContingencyWithRemedialActions, Set<CsaProfileElementaryCreationContext> cnecCreationContexts, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, Instant instant, boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
        if (addOnConstraintUsageRules(remedialActionId, assessedElementPropertyBags, linkedAssessedElementWithRemedialActions, linkedContingencyWithRemedialActions, cnecCreationContexts, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType)) {
            return;
        }
        if (addOnContingencyStateUsageRules(remedialActionId, linkedContingencyWithRemedialActions, remedialActionAdder, alterations, instant, isSchemeRemedialAction, remedialActionType)) {
            return;
        }
        addOnInstantUsageRules(remedialActionId, remedialActionAdder, instant);
    }

    private boolean addOnConstraintUsageRules(String remedialActionId, PropertyBags assessedElementPropertyBags, Set<PropertyBag> linkedAssessedElementWithRemedialActions, Set<PropertyBag> linkedContingencyWithRemedialActions, Set<CsaProfileElementaryCreationContext> cnecCreationContexts, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, Instant instant, boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
        Map<String, AssociationStatus> cnecStatusMap = OnConstraintUsageRuleHelper.processCnecsLinkedToRemedialAction(crac, remedialActionId, assessedElementPropertyBags, linkedAssessedElementWithRemedialActions, linkedContingencyWithRemedialActions, cnecCreationContexts);
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

    private boolean addOnContingencyStateUsageRules(String remedialActionId, Set<PropertyBag> linkedContingencyWithRemedialActions, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, Instant instant, boolean isSchemeRemedialAction, RemedialActionType remedialActionType) {
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
            remedialActionAdder = networkActionCreator.getNetworkActionAdder(elementaryActionsHelper.getTopologyActions(isSchemeRemedialAction), elementaryActionsHelper.getRotatingMachineActions(isSchemeRemedialAction), elementaryActionsHelper.getShuntCompensatorModifications(isSchemeRemedialAction), elementaryActionsHelper.getStaticPropertyRangesByElementaryActionsAggregator(), remedialActionId, elementaryActionsAggregatorId, alterations);
        } else {
            remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(elementaryActionsHelper.getTapPositionActions(isSchemeRemedialAction), elementaryActionsHelper.getStaticPropertyRangesByElementaryActionsAggregator(), remedialActionId, elementaryActionsAggregatorId);
        }
        return remedialActionAdder;
    }

    private static void checkAvailability(PropertyBag remedialActionPropertyBag, String remedialActionId) {
        boolean normalAvailable = Boolean.parseBoolean(remedialActionPropertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE));
        if (!normalAvailable) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "Remedial action " + remedialActionId + " will not be imported because normalAvailable is set to false");
        }
    }

    private Optional<Integer> getSpeedOpt(RemedialActionType remedialActionType, String timeToImplement, String remedialActionId, boolean isSchemeRemedialAction) {
        if (timeToImplement != null) {
            try {
                return Optional.of(CsaProfileCracUtils.convertDurationToSeconds(timeToImplement));
            } catch (RuntimeException e) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because of an irregular timeToImplement pattern");
            }
        } else {
            if (remedialActionType == RemedialActionType.PST_RANGE_ACTION && isSchemeRemedialAction) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because an auto PST range action must have a speed defined");
            }
            return Optional.empty();
        }
    }

    private static boolean isOnConstraintInstantCoherent(Instant cnecInstant, Instant remedialInstant) {
        return remedialInstant.isAuto() ? cnecInstant.isAuto() : !cnecInstant.comesBefore(remedialInstant);
    }

    private Instant defineInstant(boolean isSchemeRemedialAction, PropertyBag parentRemedialActionPropertyBag, String remedialActionId, int durationLimit) {
        if (CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString().equals(parentRemedialActionPropertyBag.get(CsaProfileConstants.KIND))) {
            return crac.getPreventiveInstant();
        }
        if (isSchemeRemedialAction) {
            return crac.getInstant(InstantKind.AUTO);
        }
        String timeToImplement = parentRemedialActionPropertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT);
        if (timeToImplement == null) {
            return crac.getInstant(InstantKind.CURATIVE);
        }
        int durationInSeconds;
        try {
            durationInSeconds = CsaProfileCracUtils.convertDurationToSeconds(timeToImplement);
        } catch (RuntimeException e) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because of an irregular timeToImplement pattern");
        }
        return durationInSeconds <= durationLimit ? crac.getInstant(InstantKind.AUTO) : crac.getInstant(InstantKind.CURATIVE);
    }

    private UsageMethod getUsageMethod(CsaProfileConstants.ElementCombinationConstraintKind elementCombinationConstraintKind, boolean isSchemeRemedialAction, Instant instant, RemedialActionType remedialActionType) {
        boolean isPstRangeAuto = instant.isAuto() && remedialActionType == RemedialActionType.PST_RANGE_ACTION;
        return isSchemeRemedialAction || CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.equals(elementCombinationConstraintKind) || isPstRangeAuto ? UsageMethod.FORCED : UsageMethod.AVAILABLE;
    }

    private static void checkKind(PropertyBag remedialActionPropertyBag, String remedialActionId, boolean isSchemeRemedialAction) {
        String kind = remedialActionPropertyBag.get(CsaProfileConstants.KIND);
        if (isSchemeRemedialAction) {
            if (!kind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because auto remedial action must be of curative kind");
            }
        } else {
            if (!kind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString()) && !kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because remedial action must be of curative or preventive kind");
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
        Map<String, Set<PropertyBag>> remedialActionDependenciesByGroup = elementaryActionsHelper.getRemedialActionDependenciesByGroup();
        elementaryActionsHelper.getRemedialActionGroupsPropertyBags().forEach(propertyBag -> {

            String groupId = propertyBag.get(CsaProfileConstants.MRID);
            String groupName = propertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME) == null ? groupId : propertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME);
            try {
                Set<PropertyBag> dependingEnabledRemedialActions = remedialActionDependenciesByGroup.getOrDefault(groupId, Set.of()).stream().filter(raDependency -> Boolean.parseBoolean(raDependency.get(CsaProfileConstants.NORMAL_ENABLED)) || raDependency.get(CsaProfileConstants.NORMAL_ENABLED) == null).collect(Collectors.toSet());
                if (!dependingEnabledRemedialActions.isEmpty()) {

                    PropertyBag refRemedialActionDependency = dependingEnabledRemedialActions.iterator().next();
                    String refRemedialActionDependencyKind = refRemedialActionDependency.get(CsaProfileConstants.KIND);
                    if (!dependingEnabledRemedialActions.stream().allMatch(raDependency -> raDependency.get(CsaProfileConstants.KIND).equals(refRemedialActionDependencyKind))) {
                        standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(getRaId()).collect(Collectors.toSet()));
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + groupId + " will not be imported because all related RemedialActionDependency must be of the same kind. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                    }

                    NetworkAction refNetworkAction = crac.getNetworkAction(refRemedialActionDependency.getId(CsaProfileConstants.REQUEST_REMEDIAL_ACTION));
                    if (refNetworkAction == null) {
                        standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(getRaId()).collect(Collectors.toSet()));
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + groupId + " will not be imported because the remedial action " + refRemedialActionDependency.getId(CsaProfileConstants.REQUEST_REMEDIAL_ACTION) + " does not exist or not imported. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
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
                        String remedialActionId = remedialActionDependency.getId(CsaProfileConstants.REQUEST_REMEDIAL_ACTION);
                        if (crac.getNetworkAction(remedialActionId) == null) {
                            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(getRaId()).collect(Collectors.toSet()));
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + groupId + " will not be imported because the remedial action " + remedialActionId + " does not exist or not imported. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                        }
                        if (!refNetworkAction.getUsageRules().equals(crac.getNetworkAction(remedialActionId).getUsageRules())) {
                            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(getRaId()).collect(Collectors.toSet()));
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + groupId + " will not be imported because all depending the remedial actions must have the same usage rules. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                        }
                        injectionSetpoints.addAll(crac.getNetworkAction(remedialActionId).getElementaryActions().stream().filter(InjectionSetpoint.class::isInstance).toList());
                        pstSetPoints.addAll(crac.getNetworkAction(remedialActionId).getElementaryActions().stream().filter(PstSetpoint.class::isInstance).toList());
                        topologicalActions.addAll(crac.getNetworkAction(remedialActionId).getElementaryActions().stream().filter(TopologicalAction.class::isInstance).toList());
                        operators.add(crac.getNetworkAction(remedialActionId).getOperator());
                    });

                    NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(groupId).withName(groupName);
                    if (operators.size() == 1) {
                        networkActionAdder.withOperator(operators.iterator().next());
                    }
                    addUsageRulesToGroup(onAngleConstraintUsageRules, onFlowConstraintUsageRules, onVoltageConstraintUsageRules, onContingencyStateUsageRules, onInstantUsageRules, injectionSetpoints, pstSetPoints, topologicalActions, networkActionAdder);
                    addElementaryActionsToGroup(injectionSetpoints, pstSetPoints, topologicalActions, networkActionAdder);
                    networkActionAdder.add();
                    contextByRaId.put(groupId, CsaProfileElementaryCreationContext.imported(groupId, groupId, groupName, "The RemedialActionGroup with mRID " + groupId + " was turned into a remedial action from the following remedial actions: " + printRaIds(dependingEnabledRemedialActions), true));
                    standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(getRaId()).collect(Collectors.toSet()));
                }

            } catch (OpenRaoImportException e) {
                contextByRaId.put(groupId, CsaProfileElementaryCreationContext.notImported(groupId, e.getImportStatus(), e.getMessage()));
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

    private static String printRaIds(Set<PropertyBag> dependingEnabledRemedialActions) {
        return dependingEnabledRemedialActions.stream().map(getRaId()).sorted(String::compareTo).collect(Collectors.joining(", "));
    }

    private static Function<PropertyBag, String> getRaId() {
        return dependingRa -> dependingRa.getId(CsaProfileConstants.REQUEST_REMEDIAL_ACTION);
    }
}
