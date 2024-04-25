/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionDependency;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CsaProfileRemedialActionsCreator {
    private final Crac crac;
    private final CsaProfileCracCreationContext cracCreationContext;
    Map<String, CsaProfileElementaryCreationContext> contextByRaId = new TreeMap<>();
    private final OnConstraintUsageRuleHelper onConstraintUsageRuleHelper;
    private final ElementaryActionsHelper elementaryActionsHelper;
    private final NetworkActionCreator networkActionCreator;
    private final PstRangeActionCreator pstRangeActionCreator;

    public CsaProfileRemedialActionsCreator(Crac crac, Network network, CsaProfileCracCreationContext cracCreationContext, OnConstraintUsageRuleHelper onConstraintUsageRuleHelper, ElementaryActionsHelper elementaryActionsHelper, int spsMaxTimeToImplementThreshold) {
        this.crac = crac;
        this.cracCreationContext = cracCreationContext;
        this.onConstraintUsageRuleHelper = onConstraintUsageRuleHelper;
        this.elementaryActionsHelper = elementaryActionsHelper;
        this.networkActionCreator = new NetworkActionCreator(this.crac, network);
        this.pstRangeActionCreator = new PstRangeActionCreator(this.crac, network);
        createRemedialActions(false, spsMaxTimeToImplementThreshold);
        createRemedialActions(true, spsMaxTimeToImplementThreshold);
        // standaloneRaIdsImplicatedIntoAGroup contain ids of Ra's depending on a group whether the group is imported or not
        Set<String> standaloneRaIdsImplicatedIntoAGroup = createRemedialActionGroups();
        standaloneRaIdsImplicatedIntoAGroup.forEach(crac::removeRemedialAction);
        standaloneRaIdsImplicatedIntoAGroup.forEach(importedRaId -> contextByRaId.remove(importedRaId));
        this.cracCreationContext.setRemedialActionCreationContexts(new HashSet<>(contextByRaId.values()));
    }

    private void createRemedialActions(boolean isSchemeRemedialAction, int spsMaxTimeToImplementThreshold) {
        for (RemedialAction nativeRemedialAction : elementaryActionsHelper.getParentRemedialActionPropertyBags(isSchemeRemedialAction)) {
            List<String> alterations = new ArrayList<>();
            try {
                checkKind(nativeRemedialAction, isSchemeRemedialAction);
                if (!nativeRemedialAction.normalAvailable()) {
                    throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "Remedial action " + nativeRemedialAction.identifier() + " will not be imported because normalAvailable is set to false");
                }
                String elementaryActionsAggregatorId = isSchemeRemedialAction ? elementaryActionsHelper.getGridStateAlterationCollection(nativeRemedialAction.identifier()) : nativeRemedialAction.identifier(); // collectionIdIfAutoOrElseRemedialActionId
                RemedialActionType remedialActionType = getRemedialActionType(nativeRemedialAction.identifier(), elementaryActionsAggregatorId, isSchemeRemedialAction);
                RemedialActionAdder<?> remedialActionAdder = getRemedialActionAdder(nativeRemedialAction.identifier(), elementaryActionsAggregatorId, remedialActionType, isSchemeRemedialAction, alterations);

                remedialActionAdder.withName(nativeRemedialAction.getUniqueName());
                if (nativeRemedialAction.operator() != null) {
                    remedialActionAdder.withOperator(CsaProfileCracUtils.getTsoNameFromUrl(nativeRemedialAction.operator()));
                }
                if (nativeRemedialAction.getTimeToImplementInSeconds() != null) {
                    remedialActionAdder.withSpeed(nativeRemedialAction.getTimeToImplementInSeconds());
                } else if (isSchemeRemedialAction && remedialActionType == RemedialActionType.PST_RANGE_ACTION) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeRemedialAction.identifier() + " will not be imported because an auto PST range action must have a speed defined");
                }
                if (elementaryActionsHelper.getContingenciesByRemedialAction().containsKey(nativeRemedialAction.identifier())) {
                    addOnContingencyStateUsageRules(nativeRemedialAction, elementaryActionsHelper.getContingenciesByRemedialAction(), remedialActionAdder, alterations, isSchemeRemedialAction, spsMaxTimeToImplementThreshold, remedialActionType);
                } else {
                    if (!isSchemeRemedialAction) {
                        // no contingency linked to RA --> on instant usage rule if remedial action is not Auto
                        addOnInstantUsageRules(nativeRemedialAction, remedialActionAdder, alterations, spsMaxTimeToImplementThreshold);
                    } else {
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeRemedialAction.identifier() + " will not be imported because no contingency is linked to the remedial action");
                    }
                }
                remedialActionAdder.add();
                if (alterations.isEmpty()) {
                    contextByRaId.put(nativeRemedialAction.identifier(), CsaProfileElementaryCreationContext.imported(nativeRemedialAction.identifier(), nativeRemedialAction.identifier(), nativeRemedialAction.getUniqueName(), "", false));
                } else {
                    contextByRaId.put(nativeRemedialAction.identifier(), CsaProfileElementaryCreationContext.imported(nativeRemedialAction.identifier(), nativeRemedialAction.identifier(), nativeRemedialAction.getUniqueName(), String.join(". ", alterations), true));
                }

            } catch (OpenRaoImportException e) {
                contextByRaId.put(nativeRemedialAction.identifier(), CsaProfileElementaryCreationContext.notImported(nativeRemedialAction.identifier(), e.getImportStatus(), e.getMessage()));
            }
        }
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

    private void addOnInstantUsageRules(RemedialAction nativeRemedialAction, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, int durationLimit) {
        Instant instant = CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString().equals(nativeRemedialAction.kind()) ? crac.getPreventiveInstant() : crac.getInstant(InstantKind.CURATIVE);
        if (instant.isCurative()) {
            instant = defineInstant(false, nativeRemedialAction, durationLimit);
        }
        boolean isLinkedToAssessedElements = elementaryActionsHelper.remedialActionIsLinkedToAssessedElements(nativeRemedialAction.identifier());

        addOnConstraintUsageRules(instant, remedialActionAdder, nativeRemedialAction.identifier(), alterations);
        if (!isLinkedToAssessedElements) {
            remedialActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(instant.getId()).add();
        }
    }

    private void checkElementCombinationConstraintKindsCoherence(Set<ContingencyWithRemedialAction> linkedContingencyWithRAs, List<String> alterations, boolean isSchemeRemedialAction) {
        // The same contingency cannot have different Element Combination Constraint Kinds
        // The same contingency can appear several times, so we need to create a memory system
        Set<String> contingenciesWithIncluded = new HashSet<>();
        Set<String> contingenciesWithConsidered = new HashSet<>();
        for (ContingencyWithRemedialAction nativeContingencyWithRemedialAction : linkedContingencyWithRAs) {
            if (CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeContingencyWithRemedialAction.combinationConstraintKind())) {
                contingenciesWithIncluded.add(nativeContingencyWithRemedialAction.contingency());
            } else if (CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString().equals(nativeContingencyWithRemedialAction.combinationConstraintKind())) {
                if (isSchemeRemedialAction) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeContingencyWithRemedialAction.remedialAction() + " will not be imported because it must be linked to the contingency " + nativeContingencyWithRemedialAction.contingency() + " with an 'included' ElementCombinationConstraintKind");
                }
                contingenciesWithConsidered.add(nativeContingencyWithRemedialAction.contingency());
            }
            if (contingenciesWithIncluded.contains(nativeContingencyWithRemedialAction.contingency()) && contingenciesWithConsidered.contains(nativeContingencyWithRemedialAction.contingency())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeContingencyWithRemedialAction.remedialAction() + " will not be imported because the ElementCombinationConstraintKinds that link the remedial action to the contingency " + nativeContingencyWithRemedialAction.contingency() + " are different");
            }
        }
    }

    private void addOnConstraintUsageRules(Instant remedialActionInstant, RemedialActionAdder<?> remedialActionAdder, String importableRemedialActionId, List<String> alterations) {
        UsageMethod usageMethod = remedialActionInstant.isAuto() && remedialActionAdder instanceof PstRangeActionAdder ? UsageMethod.FORCED : UsageMethod.AVAILABLE;
        if (!onConstraintUsageRuleHelper.getExcludedCnecsByRemedialAction().containsKey(importableRemedialActionId)) {
            onConstraintUsageRuleHelper.getImportedCnecsCombinableWithRas().forEach(addOnConstraintUsageRuleForCnec(remedialActionInstant, remedialActionAdder, usageMethod));
        } else {
            alterations.add(String.format("The association 'RemedialAction'/'Cnecs' '%s'/'%s' will be ignored because 'excluded' combination constraint kind is not supported", importableRemedialActionId, String.join(". ", onConstraintUsageRuleHelper.getExcludedCnecsByRemedialAction().get(importableRemedialActionId))));
        }
        if (onConstraintUsageRuleHelper.getConsideredAndIncludedCnecsByRemedialAction().containsKey(importableRemedialActionId)) {
            onConstraintUsageRuleHelper.getConsideredAndIncludedCnecsByRemedialAction().get(importableRemedialActionId).forEach(addOnConstraintUsageRuleForCnec(remedialActionInstant, remedialActionAdder, usageMethod));
        }
    }

    private Consumer<String> addOnConstraintUsageRuleForCnec(Instant remedialActionInstant, RemedialActionAdder<?> remedialActionAdder, UsageMethod usageMethod) {
        return cnecId -> {
            Cnec<?> cnec = crac.getCnec(cnecId);
            if (isOnConstraintInstantCoherent(cnec.getState().getInstant(), remedialActionInstant)) {
                if (cnec instanceof FlowCnec) {
                    remedialActionAdder.newOnFlowConstraintUsageRule()
                        .withInstant(remedialActionInstant.getId())
                        .withFlowCnec(cnecId)
                        .withUsageMethod(usageMethod)
                        .add();
                } else if (cnec instanceof VoltageCnec) {
                    remedialActionAdder.newOnVoltageConstraintUsageRule()
                        .withInstant(remedialActionInstant.getId())
                        .withVoltageCnec(cnecId)
                        .withUsageMethod(usageMethod)
                        .add();
                } else if (cnec instanceof AngleCnec) {
                    remedialActionAdder.newOnAngleConstraintUsageRule()
                        .withInstant(remedialActionInstant.getId())
                        .withAngleCnec(cnecId)
                        .withUsageMethod(usageMethod)
                        .add();
                } else {
                    throw new OpenRaoException(String.format("Unsupported cnec type %s", cnec.getClass().toString()));
                }
            }
        };
    }

    private static boolean isOnConstraintInstantCoherent(Instant cnecInstant, Instant remedialInstant) {
        return remedialInstant.isAuto() ? cnecInstant.isAuto() : !cnecInstant.comesBefore(remedialInstant);
    }

    private void addOnContingencyStateUsageRules(RemedialAction nativeRemedialAction, Map<String, Set<ContingencyWithRemedialAction>> linkedContingencyWithRAs, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, boolean isSchemeRemedialAction, int durationLimit, RemedialActionType remedialActionType) {
        checkElementCombinationConstraintKindsCoherence(linkedContingencyWithRAs.getOrDefault(nativeRemedialAction.identifier(), Set.of()), alterations, isSchemeRemedialAction);
        List<Pair<String, CsaProfileConstants.ElementCombinationConstraintKind>> validContingencies = new ArrayList<>();
        List<String> ignoredContingenciesMessages = new ArrayList<>();
        for (ContingencyWithRemedialAction nativeContingencyWithRemedialAction : linkedContingencyWithRAs.getOrDefault(nativeRemedialAction.identifier(), Set.of())) {
            // TODO: remove this
            if (!nativeContingencyWithRemedialAction.normalEnabled()) {
                alterations.add(String.format("Association CO/RA '%s'/'%s' will be ignored because field 'normalEnabled' in ContingencyWithRemedialAction is set to false", nativeContingencyWithRemedialAction.contingency(), nativeRemedialAction.identifier()));
            }
            if (!CsaProfileConstants.RemedialActionKind.CURATIVE.toString().equals(nativeRemedialAction.kind())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeRemedialAction.identifier() + " will not be imported because it is linked to a contingency but is not curative");
            }
            if (!nativeContingencyWithRemedialAction.normalEnabled()) {
                ignoredContingenciesMessages.add("OnContingencyState usage rule for remedial action %s with contingency %s ignored because the link between the remedial action and the contingency is disabled or missing".formatted(nativeRemedialAction.identifier(), nativeContingencyWithRemedialAction.contingency()));
                continue;
            }
            if (!CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeContingencyWithRemedialAction.combinationConstraintKind()) && !CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString().equals(nativeContingencyWithRemedialAction.combinationConstraintKind())) {
                ignoredContingenciesMessages.add("OnContingencyState usage rule for remedial action %s with contingency %s ignored because of an illegal combinationConstraintKind".formatted(nativeRemedialAction.identifier(), nativeContingencyWithRemedialAction.contingency()));
                continue;
            }
            Optional<CsaProfileElementaryCreationContext> importedCsaProfileContingencyCreationContextOpt = cracCreationContext.getContingencyCreationContexts().stream().filter(co -> co.isImported() && co.getNativeId().equals(nativeContingencyWithRemedialAction.contingency())).findAny();
            if (importedCsaProfileContingencyCreationContextOpt.isEmpty()) {
                ignoredContingenciesMessages.add("OnContingencyState usage rule for remedial action %s with contingency %s ignored because this contingency does not exist or was not imported by Open RAO".formatted(nativeRemedialAction.identifier(), nativeContingencyWithRemedialAction.contingency()));
                continue;
            }
            validContingencies.add(Pair.of(importedCsaProfileContingencyCreationContextOpt.get().getElementId(), CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeContingencyWithRemedialAction.combinationConstraintKind()) ? CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED : CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED));
        }

        // If the remedial action is linked to an assessed element, no matter if this link or this assessed element is
        // valid or not, the remedial action cannot have an onContingencyState usage rule because it would make it more
        // available than what it was designed for
        Instant instant = defineInstant(isSchemeRemedialAction, nativeRemedialAction, durationLimit);
        addOnConstraintUsageRules(instant, remedialActionAdder, nativeRemedialAction.identifier(), alterations);
        boolean isLinkedToAssessedElements = elementaryActionsHelper.remedialActionIsLinkedToAssessedElements(nativeRemedialAction.identifier());
        if (!isLinkedToAssessedElements) {
            validContingencies.forEach(openRaoContingencyId -> remedialActionAdder.newOnContingencyStateUsageRule()
                .withInstant(instant.getId())
                .withContingency(openRaoContingencyId.getLeft())
                .withUsageMethod(getUsageMethod(openRaoContingencyId.getRight(), isSchemeRemedialAction, instant, remedialActionType)).add());

            alterations.addAll(ignoredContingenciesMessages);
        }
    }

    private Instant defineInstant(boolean isSchemeRemedialAction, RemedialAction nativeRemedialAction, int durationLimit) {
        if (isSchemeRemedialAction) {
            return crac.getInstant(InstantKind.AUTO);
        }
        Integer timeToImplement = nativeRemedialAction.getTimeToImplementInSeconds();
        if (timeToImplement == null) {
            return crac.getInstant(InstantKind.CURATIVE);
        }
        if (timeToImplement <= durationLimit) {
            return crac.getInstant(InstantKind.AUTO);
        } else {
            return crac.getInstant(InstantKind.CURATIVE);
        }
    }

    private UsageMethod getUsageMethod(CsaProfileConstants.ElementCombinationConstraintKind elementCombinationConstraintKind, boolean isSchemeRemedialAction, Instant instant, RemedialActionType remedialActionType) {
        boolean isPstRangeAuto = instant.isAuto() && remedialActionType == RemedialActionType.PST_RANGE_ACTION;
        return isSchemeRemedialAction || CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.equals(elementCombinationConstraintKind) || isPstRangeAuto ? UsageMethod.FORCED : UsageMethod.AVAILABLE;
    }

    private static void checkKind(RemedialAction nativeRemedialAction, boolean isSchemeRemedialAction) {
        if (isSchemeRemedialAction) {
            if (!CsaProfileConstants.RemedialActionKind.CURATIVE.toString().equals(nativeRemedialAction.kind())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeRemedialAction.identifier() + " will not be imported because auto remedial action must be of curative kind");
            }
        } else {
            if (!CsaProfileConstants.RemedialActionKind.CURATIVE.toString().equals(nativeRemedialAction.kind()) && !CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString().equals(nativeRemedialAction.kind())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + nativeRemedialAction.identifier() + " will not be imported because remedial action must be of curative or preventive kind");
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

            String groupName = remedialActionGroup.name() == null ? remedialActionGroup.identifier() : remedialActionGroup.name();
            try {
                Set<RemedialActionDependency> dependingEnabledRemedialActions = remedialActionDependenciesByGroup.getOrDefault(remedialActionGroup.identifier(), Set.of()).stream().filter(RemedialActionDependency::normalEnabled).collect(Collectors.toSet());
                if (!dependingEnabledRemedialActions.isEmpty()) {

                    RemedialActionDependency refRemedialActionDependency = dependingEnabledRemedialActions.iterator().next();
                    if (!dependingEnabledRemedialActions.stream().allMatch(raDependency -> refRemedialActionDependency.kind().equals(raDependency.kind()))) {
                        standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.identifier() + " will not be imported because all related RemedialActionDependency must be of the same kind. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                    }

                    NetworkAction refNetworkAction = crac.getNetworkAction(refRemedialActionDependency.remedialAction());
                    if (refRemedialActionDependency.remedialAction() == null) {
                        standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.identifier() + " will not be imported because the remedial action " + refRemedialActionDependency.remedialAction() + " does not exist or not imported. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
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
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.identifier() + " will not be imported because the remedial action " + remedialActionDependency.remedialAction() + " does not exist or not imported. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                        }
                        if (!refNetworkAction.getUsageRules().equals(crac.getNetworkAction(remedialActionDependency.remedialAction()).getUsageRules())) {
                            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + remedialActionGroup.identifier() + " will not be imported because all depending the remedial actions must have the same usage rules. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                        }
                        injectionSetpoints.addAll(crac.getNetworkAction(remedialActionDependency.remedialAction()).getElementaryActions().stream().filter(InjectionSetpoint.class::isInstance).toList());
                        pstSetPoints.addAll(crac.getNetworkAction(remedialActionDependency.remedialAction()).getElementaryActions().stream().filter(PstSetpoint.class::isInstance).toList());
                        topologicalActions.addAll(crac.getNetworkAction(remedialActionDependency.remedialAction()).getElementaryActions().stream().filter(TopologicalAction.class::isInstance).toList());
                        operators.add(crac.getNetworkAction(remedialActionDependency.remedialAction()).getOperator());
                    });

                    NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(remedialActionGroup.identifier()).withName(groupName);
                    if (operators.size() == 1) {
                        networkActionAdder.withOperator(operators.iterator().next());
                    }
                    addUsageRulesToGroup(onAngleConstraintUsageRules, onFlowConstraintUsageRules, onVoltageConstraintUsageRules, onContingencyStateUsageRules, onInstantUsageRules, injectionSetpoints, pstSetPoints, topologicalActions, networkActionAdder);
                    addElementaryActionsToGroup(injectionSetpoints, pstSetPoints, topologicalActions, networkActionAdder);
                    networkActionAdder.add();
                    contextByRaId.put(remedialActionGroup.identifier(), CsaProfileElementaryCreationContext.imported(remedialActionGroup.identifier(), remedialActionGroup.identifier(), groupName, "The RemedialActionGroup with mRID " + remedialActionGroup.identifier() + " was turned into a remedial action from the following remedial actions: " + printRaIds(dependingEnabledRemedialActions), true));
                    standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                }

            } catch (OpenRaoImportException e) {
                contextByRaId.put(remedialActionGroup.identifier(), CsaProfileElementaryCreationContext.notImported(remedialActionGroup.identifier(), e.getImportStatus(), e.getMessage()));
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
