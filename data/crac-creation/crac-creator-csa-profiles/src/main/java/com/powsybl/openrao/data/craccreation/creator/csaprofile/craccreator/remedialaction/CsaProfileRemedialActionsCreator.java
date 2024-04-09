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
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.*;

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
        for (PropertyBag parentRemedialActionPropertyBag : elementaryActionsHelper.getParentRemedialActionPropertyBags(isSchemeRemedialAction)) {
            List<String> alterations = new ArrayList<>();
            String remedialActionId = parentRemedialActionPropertyBag.get(MRID);
            try {
                checkKind(parentRemedialActionPropertyBag, remedialActionId, isSchemeRemedialAction);
                checkAvailability(parentRemedialActionPropertyBag, remedialActionId);
                String elementaryActionsAggregatorId = isSchemeRemedialAction ? elementaryActionsHelper.getGridStateAlterationCollection(remedialActionId) : remedialActionId; // collectionIdIfAutoOrElseRemedialActionId
                RemedialActionType remedialActionType = getRemedialActionType(remedialActionId, elementaryActionsAggregatorId, isSchemeRemedialAction);
                RemedialActionAdder<?> remedialActionAdder = getRemedialActionAdder(remedialActionId, elementaryActionsAggregatorId, remedialActionType, isSchemeRemedialAction, alterations);

                String nativeRaName = parentRemedialActionPropertyBag.get(REMEDIAL_ACTION_NAME);
                String tsoName = parentRemedialActionPropertyBag.get(TSO);
                Optional<String> targetRemedialActionNameOpt = CsaProfileCracUtils.createElementName(nativeRaName, tsoName);
                Optional<Integer> speedOpt = getSpeedOpt(remedialActionType, parentRemedialActionPropertyBag.get(TIME_TO_IMPLEMENT), remedialActionId, isSchemeRemedialAction);
                targetRemedialActionNameOpt.ifPresent(remedialActionAdder::withName);
                if (tsoName != null) {
                    remedialActionAdder.withOperator(CsaProfileCracUtils.getTsoNameFromUrl(tsoName));
                }
                speedOpt.ifPresent(remedialActionAdder::withSpeed);
                if (elementaryActionsHelper.getContingenciesByRemedialAction().containsKey(remedialActionId)) {
                    addOnContingencyStateUsageRules(parentRemedialActionPropertyBag, remedialActionId, elementaryActionsHelper.getContingenciesByRemedialAction(), remedialActionAdder, alterations, isSchemeRemedialAction, spsMaxTimeToImplementThreshold, remedialActionType);
                } else {
                    if (!isSchemeRemedialAction) {
                        // no contingency linked to RA --> on instant usage rule if remedial action is not Auto
                        addOnInstantUsageRules(parentRemedialActionPropertyBag, remedialActionAdder, remedialActionId, alterations, spsMaxTimeToImplementThreshold);
                    } else {
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because no contingency is linked to the remedial action");
                    }
                }
                remedialActionAdder.add();
                if (alterations.isEmpty()) {
                    contextByRaId.put(remedialActionId, CsaProfileElementaryCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), "", false));
                } else {
                    contextByRaId.put(remedialActionId, CsaProfileElementaryCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), String.join(". ", alterations), true));
                }

            } catch (OpenRaoImportException e) {
                contextByRaId.put(remedialActionId, CsaProfileElementaryCreationContext.notImported(remedialActionId, e.getImportStatus(), e.getMessage()));
            }
        }
    }

    private RemedialActionAdder<?> getRemedialActionAdder(String remedialActionId, String elementaryActionsAggregatorId, RemedialActionType remedialActionType, boolean isSchemeRemedialAction, List<String> alterations) {
        RemedialActionAdder<?> remedialActionAdder;
        if (remedialActionType.equals(RemedialActionType.NETWORK_ACTION)) {
            remedialActionAdder = networkActionCreator.getNetworkActionAdder(elementaryActionsHelper.getTopologyActions(isSchemeRemedialAction), elementaryActionsHelper.getRotatingMachineActions(isSchemeRemedialAction), elementaryActionsHelper.getShuntCompensatorModifications(isSchemeRemedialAction), elementaryActionsHelper.getStaticPropertyRangesByElementaryActionsAggregator(), remedialActionId, elementaryActionsAggregatorId, alterations);
        } else {
            remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(elementaryActionsHelper.getTapPositionActions(isSchemeRemedialAction), elementaryActionsHelper.getStaticPropertyRangesByElementaryActionsAggregator(), remedialActionId, elementaryActionsAggregatorId, alterations);
        }
        return remedialActionAdder;
    }

    private void addOnInstantUsageRules(PropertyBag parentRemedialActionPropertyBag, RemedialActionAdder<?> remedialActionAdder, String remedialActionId, List<String> alterations, int durationLimit) {
        Instant instant = parentRemedialActionPropertyBag.get(KIND).equals(RemedialActionKind.PREVENTIVE.toString()) ? crac.getPreventiveInstant() : crac.getInstant(InstantKind.CURATIVE);
        if (instant.isCurative()) {
            instant = defineInstant(false, parentRemedialActionPropertyBag, remedialActionId, durationLimit);
        }
        boolean isLinkedToAssessedElements = elementaryActionsHelper.remedialActionIsLinkedToAssessedElements(remedialActionId);

        addOnConstraintUsageRules(instant, remedialActionAdder, remedialActionId, alterations);
        if (!isLinkedToAssessedElements) {
            remedialActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(instant.getId()).add();
        }

    }

    private void checkElementCombinationConstraintKindsCoherence(String remedialActionId, Map<String, Set<PropertyBag>> linkedContingencyWithRAs, List<String> alterations, boolean isSchemeRemedialAction) {
        // The same contingency cannot have different Element Combination Constraint Kinds
        // The same contingency can appear several times, so we need to create a memory system
        Set<String> contingenciesWithIncluded = new HashSet<>();
        Set<String> contingenciesWithConsidered = new HashSet<>();
        Set<PropertyBag> linkedContingencyWithRA = linkedContingencyWithRAs.get(remedialActionId);
        for (PropertyBag propertyBag : linkedContingencyWithRA) {
            String combinationKind = propertyBag.get(COMBINATION_CONSTRAINT_KIND);
            String contingencyId = propertyBag.getId(REQUEST_CONTINGENCY);
            if (combinationKind.equals(ElementCombinationConstraintKind.INCLUDED.toString())) {
                contingenciesWithIncluded.add(contingencyId);
            } else if (combinationKind.equals(ElementCombinationConstraintKind.CONSIDERED.toString())) {
                if (isSchemeRemedialAction) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it must be linked to the contingency " + contingencyId + " with an 'included' ElementCombinationConstraintKind");
                }
                contingenciesWithConsidered.add(contingencyId);
            }
            if (contingenciesWithIncluded.contains(contingencyId) && contingenciesWithConsidered.contains(contingencyId)) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because the ElementCombinationConstraintKinds that link the remedial action to the contingency " + contingencyId + " are different");
            }
        }
    }

    private static void checkAvailability(PropertyBag remedialActionPropertyBag, String remedialActionId) {
        boolean normalAvailable = Boolean.parseBoolean(remedialActionPropertyBag.get(NORMAL_AVAILABLE));
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
        // TODO: add test to cover all the possible cases
        return remedialInstant.isAuto() ? cnecInstant.isAuto() : !cnecInstant.comesBefore(remedialInstant);
    }

    private void addOnContingencyStateUsageRules(PropertyBag parentRemedialActionPropertyBag, String remedialActionId, Map<String, Set<PropertyBag>> linkedContingencyWithRAs, RemedialActionAdder<?> remedialActionAdder, List<String> alterations, boolean isSchemeRemedialAction, int durationLimit, RemedialActionType remedialActionType) {
        checkElementCombinationConstraintKindsCoherence(remedialActionId, linkedContingencyWithRAs, alterations, isSchemeRemedialAction);
        List<Pair<String, ElementCombinationConstraintKind>> validContingencies = new ArrayList<>();
        List<String> ignoredContingenciesMessages = new ArrayList<>();
        for (PropertyBag contingencyWithRemedialActionPropertyBag : linkedContingencyWithRAs.get(remedialActionId)) {
            Optional<String> normalEnabledOpt = Optional.ofNullable(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.NORMAL_ENABLED));
            if (normalEnabledOpt.isPresent() && !Boolean.parseBoolean(normalEnabledOpt.get())) {
                alterations.add(String.format("Association CO/RA '%s'/'%s' will be ignored because field 'normalEnabled' in ContingencyWithRemedialAction is set to false", contingencyWithRemedialActionPropertyBag.getId(REQUEST_CONTINGENCY), remedialActionId));
            }
            if (!parentRemedialActionPropertyBag.get(KIND).equals(RemedialActionKind.CURATIVE.toString())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it is linked to a contingency but is not curative");
            }
            String contingencyId = contingencyWithRemedialActionPropertyBag.getId(REQUEST_CONTINGENCY);
            Optional<String> normalEnabled = Optional.ofNullable(contingencyWithRemedialActionPropertyBag.get(NORMAL_ENABLED));
            if (normalEnabled.isPresent() && !Boolean.parseBoolean(normalEnabled.get())) {
                ignoredContingenciesMessages.add("OnContingencyState usage rule for remedial action %s with contingency %s ignored because the link between the remedial action and the contingency is disabled or missing".formatted(remedialActionId, contingencyId));
                continue;
            }
            String combinationConstraintKind = contingencyWithRemedialActionPropertyBag.get(COMBINATION_CONSTRAINT_KIND);
            if (!combinationConstraintKind.equals(ElementCombinationConstraintKind.INCLUDED.toString()) && !combinationConstraintKind.equals(ElementCombinationConstraintKind.CONSIDERED.toString())) {
                ignoredContingenciesMessages.add("OnContingencyState usage rule for remedial action %s with contingency %s ignored because of an illegal combinationConstraintKind".formatted(remedialActionId, contingencyId));
                continue;
            }
            Optional<CsaProfileElementaryCreationContext> importedCsaProfileContingencyCreationContextOpt = cracCreationContext.getContingencyCreationContexts().stream().filter(co -> co.isImported() && co.getNativeId().equals(contingencyId)).findAny();
            if (importedCsaProfileContingencyCreationContextOpt.isEmpty()) {
                ignoredContingenciesMessages.add("OnContingencyState usage rule for remedial action %s with contingency %s ignored because this contingency does not exist or was not imported by Open RAO".formatted(remedialActionId, contingencyId));
                continue;
            }
            validContingencies.add(Pair.of(importedCsaProfileContingencyCreationContextOpt.get().getElementId(), combinationConstraintKind.equals(ElementCombinationConstraintKind.INCLUDED.toString()) ? ElementCombinationConstraintKind.INCLUDED : ElementCombinationConstraintKind.CONSIDERED));
        }

        // If the remedial action is linked to an assessed element, no matter if this link or this assessed element is
        // valid or not, the remedial action cannot have an onContingencyState usage rule because it would make it more
        // available than what it was designed for
        crac.getInstants(InstantKind.CURATIVE).forEach(instant -> addOnConstraintUsageRules(instant, remedialActionAdder, remedialActionId, alterations));

        boolean isLinkedToAssessedElements = elementaryActionsHelper.remedialActionIsLinkedToAssessedElements(remedialActionId);
        if (!isLinkedToAssessedElements) {
            Instant instant = defineInstant(isSchemeRemedialAction, parentRemedialActionPropertyBag, remedialActionId, durationLimit);

            validContingencies.forEach(openRaoContingencyId -> remedialActionAdder.newOnContingencyStateUsageRule()
                .withInstant(instant.getId())
                .withContingency(openRaoContingencyId.getLeft())
                .withUsageMethod(getUsageMethod(openRaoContingencyId.getRight(), isSchemeRemedialAction, instant, remedialActionType)).add());

            alterations.addAll(ignoredContingenciesMessages);
        }
    }

    private Instant defineInstant(boolean isSchemeRemedialAction, PropertyBag parentRemedialActionPropertyBag, String remedialActionId, int durationLimit) {
        if (isSchemeRemedialAction) {
            return crac.getInstant(InstantKind.AUTO);
        }
        String timeToImplement = parentRemedialActionPropertyBag.get(TIME_TO_IMPLEMENT);
        if (timeToImplement == null) {
            return crac.getInstant(InstantKind.CURATIVE);
        }
        int durationInSeconds = 0;
        try {
            durationInSeconds = CsaProfileCracUtils.convertDurationToSeconds(timeToImplement);
        } catch (RuntimeException e) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because of an irregular timeToImplement pattern");
        }
        if (durationInSeconds <= durationLimit) {
            return crac.getInstant(InstantKind.AUTO);
        } else {
            return crac.getInstant(InstantKind.CURATIVE);
        }
    }

    private UsageMethod getUsageMethod(ElementCombinationConstraintKind elementCombinationConstraintKind, boolean isSchemeRemedialAction, Instant instant, RemedialActionType remedialActionType) {
        boolean isPstRangeAuto = instant.isAuto() && remedialActionType == RemedialActionType.PST_RANGE_ACTION;
        return isSchemeRemedialAction || ElementCombinationConstraintKind.INCLUDED.equals(elementCombinationConstraintKind) || isPstRangeAuto ? UsageMethod.FORCED : UsageMethod.AVAILABLE;
    }

    private static void checkKind(PropertyBag remedialActionPropertyBag, String remedialActionId, boolean isSchemeRemedialAction) {
        String kind = remedialActionPropertyBag.get(KIND);
        if (isSchemeRemedialAction) {
            if (!kind.equals(RemedialActionKind.CURATIVE.toString())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because auto remedial action must be of curative kind");
            }
        } else {
            if (!kind.equals(RemedialActionKind.CURATIVE.toString()) && !kind.equals(RemedialActionKind.PREVENTIVE.toString())) {
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

            String groupId = propertyBag.get(MRID);
            String groupName = propertyBag.get(REMEDIAL_ACTION_NAME) == null ? groupId : propertyBag.get(REMEDIAL_ACTION_NAME);
            try {
                Set<PropertyBag> dependingEnabledRemedialActions = remedialActionDependenciesByGroup.get(groupId).stream().filter(raDependency -> Boolean.parseBoolean(raDependency.get(NORMAL_ENABLED)) || raDependency.get(NORMAL_ENABLED) == null).collect(Collectors.toSet());
                if (!dependingEnabledRemedialActions.isEmpty()) {

                    PropertyBag refRemedialActionDependency = dependingEnabledRemedialActions.iterator().next();
                    String refRemedialActionDependencyKind = refRemedialActionDependency.get(KIND);
                    if (!dependingEnabledRemedialActions.stream().allMatch(raDependency -> raDependency.get(KIND).equals(refRemedialActionDependencyKind))) {
                        standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(getRaId()).collect(Collectors.toSet()));
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + groupId + " will not be imported because all related RemedialActionDependency must be of the same kind. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
                    }

                    NetworkAction refNetworkAction = crac.getNetworkAction(refRemedialActionDependency.getId(REQUEST_REMEDIAL_ACTION));
                    if (refNetworkAction == null) {
                        standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(getRaId()).collect(Collectors.toSet()));
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action group " + groupId + " will not be imported because the remedial action " + refRemedialActionDependency.getId(REQUEST_REMEDIAL_ACTION) + " does not exist or not imported. All RA's depending in that group will be ignored: " + printRaIds(dependingEnabledRemedialActions));
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
                        String remedialActionId = remedialActionDependency.getId(REQUEST_REMEDIAL_ACTION);
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
        return dependingRa -> dependingRa.getId(REQUEST_REMEDIAL_ACTION);
    }
}
