/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.action.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.nc.NcCrac;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcAggregator;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.RemedialActionKind;
import com.powsybl.openrao.data.crac.io.nc.objects.*;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class NcRemedialActionsCreator {
    private final Crac crac;
    Map<String, ElementaryCreationContext> contextByRaId = new TreeMap<>();
    private final ElementaryActionsHelper elementaryActionsHelper;
    private final NetworkActionCreator networkActionCreator;
    private final PstRangeActionCreator pstRangeActionCreator;
    private final Set<GridStateAlterationRemedialAction> nativeRemedialActions;
    private final NcCracCreationParameters ncCracCreationParameters;

    public NcRemedialActionsCreator(Crac crac,
                                    Network network,
                                    NcCrac nativeCrac,
                                    NcCracCreationContext cracCreationContext,
                                    Set<ElementaryCreationContext> cnecCreationContexts,
                                    NcCracCreationParameters ncCracCreationParameters) {
        this.crac = crac;
        this.elementaryActionsHelper = new ElementaryActionsHelper(nativeCrac);
        this.networkActionCreator = new NetworkActionCreator(this.crac, network);
        this.ncCracCreationParameters = ncCracCreationParameters;
        Map<String, String> pstPerTapChanger = new NcAggregator<>(TapChanger::powerTransformer).aggregate(nativeCrac.getTapChangers()).entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getValue().iterator().next().mrid(), Map.Entry::getKey));
        this.pstRangeActionCreator = new PstRangeActionCreator(this.crac, network, pstPerTapChanger);
        Map<String, Set<AssessedElementWithRemedialAction>> linkedAeWithRa = new NcAggregator<>(AssessedElementWithRemedialAction::remedialAction)
            .aggregate(nativeCrac.getAssessedElementWithRemedialActions());
        Map<String, Set<ContingencyWithRemedialAction>> linkedCoWithRa = new NcAggregator<>(ContingencyWithRemedialAction::remedialAction)
            .aggregate(nativeCrac.getContingencyWithRemedialActions());
        this.nativeRemedialActions = new HashSet<>(nativeCrac.getGridStateAlterationRemedialActions());
        createRemedialActions(linkedAeWithRa, linkedCoWithRa, cnecCreationContexts);
        // standaloneRaIdsImplicatedIntoAGroup contain ids of Ra's depending on a group whether the group is imported or not
        Set<String> standaloneRaIdsImplicatedIntoAGroup = createRemedialActionGroups();
        standaloneRaIdsImplicatedIntoAGroup.forEach(crac::removeRemedialAction);
        standaloneRaIdsImplicatedIntoAGroup.forEach(importedRaId -> contextByRaId.remove(importedRaId));
        cracCreationContext.setRemedialActionCreationContexts(new HashSet<>(contextByRaId.values()));
    }

    private void createRemedialActions(Map<String, Set<AssessedElementWithRemedialAction>> linkedAeWithRa,
                                       Map<String, Set<ContingencyWithRemedialAction>> linkedCoWithRa,
                                       Set<ElementaryCreationContext> cnecCreationContexts) {
        for (GridStateAlterationRemedialAction nativeRemedialAction : nativeRemedialActions) {
            List<String> alterations = new ArrayList<>();
            try {
                checkKind(nativeRemedialAction);
                if (!nativeRemedialAction.normalAvailable()) {
                    throw new OpenRaoImportException(
                        ImportStatus.NOT_FOR_RAO,
                        String.format("Remedial action %s will not be imported because it is set as unavailable",
                                      nativeRemedialAction.mrid())
                    );
                }
                RemedialActionType remedialActionType = getRemedialActionType(nativeRemedialAction.mrid(), nativeRemedialAction.mrid());
                RemedialActionAdder<?> remedialActionAdder;
                if (remedialActionType.equals(RemedialActionType.NETWORK_ACTION)) {
                    remedialActionAdder = networkActionCreator.getNetworkActionAdder(
                        elementaryActionsHelper.getTopologyActions(),
                        elementaryActionsHelper.getRotatingMachineActions(),
                        elementaryActionsHelper.getShuntCompensatorModifications(),
                        elementaryActionsHelper.getNativeStaticPropertyRangesPerNativeGridStateAlteration(),
                        nativeRemedialAction.mrid(),
                        nativeRemedialAction.mrid(),
                        alterations
                    );
                    fillAndSaveRemedialActionAdderAndContext(
                        linkedAeWithRa,
                        linkedCoWithRa,
                        cnecCreationContexts,
                        nativeRemedialAction,
                        alterations,
                        remedialActionType,
                        remedialActionAdder,
                        nativeRemedialAction.getUniqueName()
                    );
                } else {
                    if (elementaryActionsHelper.getTapPositionActions().get(nativeRemedialAction.mrid()).size() > 1) {
                        // group TapPositionAction's
                        for (TapPositionAction nativeTapPositionAction : elementaryActionsHelper.getTapPositionActions().get(nativeRemedialAction.mrid())) {
                            try {
                                remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(
                                    true,
                                    nativeRemedialAction.mrid(),
                                    nativeTapPositionAction,
                                    elementaryActionsHelper.getNativeStaticPropertyRangesPerNativeGridStateAlteration(),
                                    nativeTapPositionAction.mrid()
                                );
                                fillAndSaveRemedialActionAdderAndContext(
                                    linkedAeWithRa,
                                    linkedCoWithRa,
                                    cnecCreationContexts,
                                    nativeRemedialAction,
                                    alterations,
                                    remedialActionType,
                                    remedialActionAdder,
                                    createNameFromTapPositionAction(
                                        nativeTapPositionAction.mrid(),
                                        nativeRemedialAction.operator()
                                    )
                                );
                            } catch (OpenRaoImportException e) {
                                if (e.getImportStatus().equals(ImportStatus.NOT_FOR_RAO)) {
                                    contextByRaId.put(nativeTapPositionAction.mrid(), StandardElementaryCreationContext.notImported(
                                        nativeTapPositionAction.mrid(), null, e.getImportStatus(), e.getMessage()
                                    ));
                                } else {
                                    throw e;
                                }
                            }
                        }
                    } else {
                        remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(
                            false,
                            nativeRemedialAction.mrid(),
                            elementaryActionsHelper.getTapPositionActions().get(nativeRemedialAction.mrid()).iterator().next(),
                            elementaryActionsHelper.getNativeStaticPropertyRangesPerNativeGridStateAlteration(),
                            nativeRemedialAction.mrid()
                        );
                        fillAndSaveRemedialActionAdderAndContext(
                            linkedAeWithRa,
                            linkedCoWithRa,
                            cnecCreationContexts,
                            nativeRemedialAction,
                            alterations,
                            remedialActionType,
                            remedialActionAdder,
                            nativeRemedialAction.getUniqueName()
                        );
                    }
                }

            } catch (OpenRaoImportException e) {
                contextByRaId.put(nativeRemedialAction.mrid(), StandardElementaryCreationContext.notImported(
                    nativeRemedialAction.mrid(), null, e.getImportStatus(), e.getMessage()
                ));
            }
        }
    }

    private String createNameFromTapPositionAction(String tapPositionId, String operator) {
        if (operator != null) {
            return NcCracUtils.getTsoNameFromUrl(operator) + "-" + tapPositionId;
        } else {
            return tapPositionId;
        }
    }

    private void fillAndSaveRemedialActionAdderAndContext(Map<String, Set<AssessedElementWithRemedialAction>> linkedAeWithRa,
                                                          Map<String, Set<ContingencyWithRemedialAction>> linkedCoWithRa,
                                                          Set<ElementaryCreationContext> cnecCreationContexts,
                                                          GridStateAlterationRemedialAction nativeRemedialAction,
                                                          List<String> alterations,
                                                          RemedialActionType remedialActionType,
                                                          RemedialActionAdder<?> remedialActionAdder,
                                                          String remedialActionName) {

        remedialActionAdder.withName(remedialActionName);
        if (nativeRemedialAction.operator() != null) {
            remedialActionAdder.withOperator(NcCracUtils.getTsoNameFromUrl(nativeRemedialAction.operator()));
        }
        if (nativeRemedialAction.getTimeToImplementInSeconds() != null) {
            remedialActionAdder.withSpeed(nativeRemedialAction.getTimeToImplementInSeconds());
        } else if (!nativeRemedialAction.isManual() && remedialActionType == RemedialActionType.PST_RANGE_ACTION) {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Remedial action %s will not be imported because an auto PST range action must have a speed defined",
                              nativeRemedialAction.mrid())
            );
        }

        InstantKind instantKind = getInstantKind(nativeRemedialAction);
        Set<Instant> instants = getInstants(instantKind, nativeRemedialAction.operator() == null ?
            null : NcCracUtils.getTsoNameFromUrl(nativeRemedialAction.operator()));
        instants.forEach(instant -> addUsageRules(
            nativeRemedialAction.mrid(),
            linkedAeWithRa.getOrDefault(nativeRemedialAction.mrid(), Set.of()),
            linkedCoWithRa.getOrDefault(nativeRemedialAction.mrid(), Set.of()),
            cnecCreationContexts,
            remedialActionAdder,
            alterations,
            instant
        ));
        remedialActionAdder.add();

        if (alterations.isEmpty()) {
            contextByRaId.put(nativeRemedialAction.mrid(), StandardElementaryCreationContext.imported(
                nativeRemedialAction.mrid(),
                null,
                nativeRemedialAction.mrid(),
                false,
                "")
            );
        } else {
            contextByRaId.put(nativeRemedialAction.mrid(), StandardElementaryCreationContext.imported(
                nativeRemedialAction.mrid(),
                null,
                nativeRemedialAction.mrid(),
                true,
                String.join(". ", alterations)
            ));
        }
    }

    private Set<Instant> getInstants(InstantKind instantKind, String operator) {
        Set<Instant> instants = crac.getInstants(instantKind);
        if (instantKind == InstantKind.CURATIVE && operator != null && ncCracCreationParameters.getRestrictedCurativeBatchesPerTso().containsKey(operator)) {
            return instants.stream()
                .filter(instant -> ncCracCreationParameters.getRestrictedCurativeBatchesPerTso().get(operator).contains(instant.getId()))
                .collect(Collectors.toSet());
        }
        return instants;
    }

    private void addUsageRules(String remedialActionId,
                               Set<AssessedElementWithRemedialAction> linkedAssessedElementWithRemedialActions,
                               Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions,
                               Set<ElementaryCreationContext> cnecCreationContexts,
                               RemedialActionAdder<?> remedialActionAdder,
                               List<String> alterations,
                               Instant instant) {
        if (addOnConstraintUsageRules(
            remedialActionId,
            linkedAssessedElementWithRemedialActions,
            linkedContingencyWithRemedialActions,
            cnecCreationContexts,
            remedialActionAdder,
            alterations,
            instant
        )) {
            return;
        }
        if (addOnContingencyStateUsageRules(
            remedialActionId,
            linkedContingencyWithRemedialActions,
            remedialActionAdder,
            alterations,
            instant
        )) {
            return;
        }
        addOnInstantUsageRules(remedialActionId, remedialActionAdder, instant);
    }

    private boolean addOnConstraintUsageRules(String remedialActionId,
                                              Set<AssessedElementWithRemedialAction> linkedAssessedElementWithRemedialActions,
                                              Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions,
                                              Set<ElementaryCreationContext> cnecCreationContexts,
                                              RemedialActionAdder<?> remedialActionAdder,
                                              List<String> alterations,
                                              Instant instant) {
        Map<String, AssociationStatus> cnecStatusMap = OnConstraintUsageRuleHelper.processCnecsLinkedToRemedialAction(
            crac,
            remedialActionId,
            linkedAssessedElementWithRemedialActions,
            linkedContingencyWithRemedialActions,
            cnecCreationContexts
        );
        cnecStatusMap.forEach((cnecId, cnecStatus) -> {
            if (cnecStatus.isValid()) {
                Cnec<?> cnec = crac.getCnec(cnecId);
                if (isOnConstraintInstantCoherent(cnec.getState().getInstant(), instant)) {
                    remedialActionAdder.newOnConstraintUsageRule()
                        .withInstant(instant.getId())
                        .withCnec(cnecId)
                        .add();
                }
            } else {
                alterations.add(cnecStatus.statusDetails());
            }
        });
        return !linkedAssessedElementWithRemedialActions.isEmpty();
    }

    private boolean addOnContingencyStateUsageRules(String remedialActionId,
                                                    Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions,
                                                    RemedialActionAdder<?> remedialActionAdder,
                                                    List<String> alterations,
                                                    Instant instant) {
        Map<String, AssociationStatus> contingencyStatusMap = OnContingencyStateUsageRuleHelper
            .processContingenciesLinkedToRemedialAction(crac, remedialActionId, linkedContingencyWithRemedialActions);
        if (instant.isPreventive() && !linkedContingencyWithRemedialActions.isEmpty()) {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                "Remedial action %s will not be imported because it is linked to a contingency but is not curative".formatted(remedialActionId)
            );
        }
        contingencyStatusMap.forEach((contingencyId, contingencyStatus) -> {
            if (contingencyStatus.isValid()) {
                remedialActionAdder.newOnContingencyStateUsageRule()
                    .withInstant(instant.getId())
                    .withContingency(contingencyId)
                    .add();
            } else {
                alterations.add(contingencyStatus.statusDetails());
            }
        });
        return !linkedContingencyWithRemedialActions.isEmpty();
    }

    private void addOnInstantUsageRules(String remedialActionId, RemedialActionAdder<?> remedialActionAdder, Instant instant) {
        if (instant.isAuto()) {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                ("Remedial action %s will not be imported because no contingency or assessed element is linked to the remedial action " +
                    "and this is nor supported for ARAs").formatted(remedialActionId)
            );
        }
        remedialActionAdder.newOnInstantUsageRule().withInstant(instant.getId()).add();
    }

    private static boolean isOnConstraintInstantCoherent(Instant cnecInstant, Instant remedialInstant) {
        return remedialInstant.isAuto() ? cnecInstant.isAuto() : !cnecInstant.comesBefore(remedialInstant);
    }

    private InstantKind getInstantKind(GridStateAlterationRemedialAction nativeRemedialAction) {
        if (RemedialActionKind.PREVENTIVE.toString().equals(nativeRemedialAction.kind())) {
            return InstantKind.PREVENTIVE;
        }
        if (!nativeRemedialAction.isManual()) {
            return InstantKind.AUTO;
        }
        return InstantKind.CURATIVE;
    }

    private static void checkKind(GridStateAlterationRemedialAction nativeRemedialAction) {
        if (!RemedialActionKind.CURATIVE.toString().equals(nativeRemedialAction.kind()) && !RemedialActionKind.PREVENTIVE.toString().equals(nativeRemedialAction.kind())) {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Remedial action %s will not be imported because remedial action must be of curative or preventive kind", nativeRemedialAction.mrid())
            );
        }
        if (RemedialActionKind.PREVENTIVE.toString().equals(nativeRemedialAction.kind()) && !nativeRemedialAction.isManual()) {
            throw new OpenRaoImportException(
                ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO,
                "OpenRAO does not support preventive automatons, remedial action %s will be ignored".formatted(nativeRemedialAction.mrid())
            );
        }
    }

    private RemedialActionType getRemedialActionType(String remedialActionId, String elementaryActionsAggregatorId) {
        RemedialActionType remedialActionType;
        if (elementaryActionsHelper.getTopologyActions().containsKey(elementaryActionsAggregatorId)) {
            remedialActionType = RemedialActionType.NETWORK_ACTION;
        } else if (elementaryActionsHelper.getRotatingMachineActions().containsKey(elementaryActionsAggregatorId)) {
            remedialActionType = RemedialActionType.NETWORK_ACTION;
        } else if (elementaryActionsHelper.getShuntCompensatorModifications().containsKey(elementaryActionsAggregatorId)) {
            remedialActionType = RemedialActionType.NETWORK_ACTION;
        } else if (elementaryActionsHelper.getTapPositionActions().containsKey(elementaryActionsAggregatorId)) {
            // StaticPropertyRanges not mandatory in case of tapPositionsActions
            remedialActionType = RemedialActionType.PST_RANGE_ACTION;
        } else {
            throw new OpenRaoImportException(
                ImportStatus.NOT_FOR_RAO,
                String.format("Remedial action %s will not be imported because it has no elementary action", remedialActionId)
            );
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
        for (RemedialActionGroup remedialActionGroup : elementaryActionsHelper.getRemedialActionGroupsPropertyBags()) {
            String groupName = remedialActionGroup.name() == null ? remedialActionGroup.mrid() : remedialActionGroup.name();
            try {
                Set<RemedialActionDependency> dependingEnabledRemedialActions = remedialActionDependenciesByGroup
                    .getOrDefault(remedialActionGroup.mrid(), Set.of())
                    .stream().filter(RemedialActionDependency::normalEnabled)
                    .collect(Collectors.toSet());
                if (!dependingEnabledRemedialActions.isEmpty()) {
                    RemedialActionDependency refRemedialActionDependency = dependingEnabledRemedialActions.iterator().next();
                    checkKindConsistency(remedialActionGroup, dependingEnabledRemedialActions, refRemedialActionDependency, standaloneRasImplicatedIntoAGroup);

                    NetworkAction refNetworkAction = crac.getNetworkAction(refRemedialActionDependency.remedialAction());
                    checkNetworkActionIsNonNull(remedialActionGroup, refNetworkAction, standaloneRasImplicatedIntoAGroup, dependingEnabledRemedialActions, refRemedialActionDependency);
                    List<UsageRule> onConstraintUsageRules = refNetworkAction.getUsageRules().stream().filter(OnConstraint.class::isInstance).toList();
                    List<UsageRule> onContingencyStateUsageRules = refNetworkAction.getUsageRules().stream().filter(OnContingencyState.class::isInstance).toList();
                    List<UsageRule> onInstantUsageRules = refNetworkAction.getUsageRules().stream().filter(OnInstant.class::isInstance).toList();

                    List<Action> elementaryActions = new ArrayList<>();
                    Set<String> operators = new HashSet<>();
                    dependingEnabledRemedialActions.forEach(remedialActionDependency -> {
                        NetworkAction networkAction = crac.getNetworkAction(remedialActionDependency.remedialAction());
                        checkNetworkActionIsNonNull(remedialActionGroup, networkAction, standaloneRasImplicatedIntoAGroup, dependingEnabledRemedialActions, remedialActionDependency);
                        checkUsageRulesConsistency(remedialActionGroup, networkAction, standaloneRasImplicatedIntoAGroup, dependingEnabledRemedialActions, refNetworkAction);
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
                    contextByRaId.put(
                        remedialActionGroup.mrid(),
                        StandardElementaryCreationContext.imported(
                             remedialActionGroup.mrid(), null, remedialActionGroup.mrid(), true,
                             "The RemedialActionGroup with mRID " + remedialActionGroup.mrid() +
                                 " was turned into a remedial action from the following remedial actions: " + printRaIds(dependingEnabledRemedialActions)
                        )
                    );
                    standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
                }
            } catch (OpenRaoImportException e) {
                contextByRaId.put(remedialActionGroup.mrid(), StandardElementaryCreationContext.notImported(remedialActionGroup.mrid(), null, e.getImportStatus(), e.getMessage()));
            }
        }
        return standaloneRasImplicatedIntoAGroup;
    }

    private static void checkKindConsistency(RemedialActionGroup remedialActionGroup,
                                             Set<RemedialActionDependency> dependingEnabledRemedialActions,
                                             RemedialActionDependency refRemedialActionDependency,
                                             Set<String> standaloneRasImplicatedIntoAGroup) {
        if (!dependingEnabledRemedialActions.stream().allMatch(raDependency -> refRemedialActionDependency.kind().equals(raDependency.kind()))) {
            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Remedial action group %s will not be imported because all related RemedialActionDependency must be of the same kind. " +
                                  "All RA's depending in that group will be ignored: %s",
                              remedialActionGroup.mrid(), printRaIds(dependingEnabledRemedialActions)
                )
            );
        }
    }

    private static void checkNetworkActionIsNonNull(RemedialActionGroup remedialActionGroup,
                                                    NetworkAction networkAction,
                                                    Set<String> standaloneRasImplicatedIntoAGroup,
                                                    Set<RemedialActionDependency> dependingEnabledRemedialActions,
                                                    RemedialActionDependency remedialActionDependency) {
        if (networkAction == null) {
            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Remedial action group %s will not be imported because the remedial action %s does not exist or not imported. " +
                                  "All RA's depending in that group will be ignored: %s",
                              remedialActionGroup.mrid(), remedialActionDependency.remedialAction(), printRaIds(dependingEnabledRemedialActions)
                )
            );
        }
    }

    private static void checkUsageRulesConsistency(RemedialActionGroup remedialActionGroup,
                                                   NetworkAction networkAction,
                                                   Set<String> standaloneRasImplicatedIntoAGroup,
                                                   Set<RemedialActionDependency> dependingEnabledRemedialActions,
                                                   NetworkAction refNetworkAction) {
        if (!refNetworkAction.getUsageRules().equals(networkAction.getUsageRules())) {
            standaloneRasImplicatedIntoAGroup.addAll(dependingEnabledRemedialActions.stream().map(RemedialActionDependency::remedialAction).collect(Collectors.toSet()));
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Remedial action group %s will not be imported because all depending remedial actions " +
                                  "must have the same usage rules. All RA's depending in that group will be ignored: %s",
                              remedialActionGroup.mrid(), printRaIds(dependingEnabledRemedialActions))
            );
        }
    }

    private static void addElementaryActionsToGroup(List<Action> elementaryActions, NetworkActionAdder networkActionAdder) {
        for (Action ea : elementaryActions) {
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
        }
    }

    private static void addUsageRulesToGroup(List<UsageRule> onConstraintUsageRules,
                                             List<UsageRule> onContingencyStateUsageRules,
                                             List<UsageRule> onInstantUsageRules,
                                             NetworkActionAdder networkActionAdder) {
        onConstraintUsageRules.forEach(ur -> {
            OnConstraint<?> onConstraintUsageRule = (OnConstraint<?>) ur;
            networkActionAdder.newOnConstraintUsageRule()
                .withInstant(onConstraintUsageRule.getInstant().getId())
                .withCnec(onConstraintUsageRule.getCnec().getId())
                .add();
        });
        onContingencyStateUsageRules.forEach(ur -> {
            OnContingencyState onContingencyStateUsageRule = (OnContingencyState) ur;
            networkActionAdder.newOnContingencyStateUsageRule()
                .withInstant(onContingencyStateUsageRule.getInstant().getId())
                .withContingency(onContingencyStateUsageRule.getContingency().getId())
                .add();
        });
        onInstantUsageRules.forEach(ur -> {
            OnInstant onInstantUsageRule = (OnInstant) ur;
            networkActionAdder.newOnInstantUsageRule()
                .withInstant(onInstantUsageRule.getInstant().getId())
                .add();
        });

    }

    private static String printRaIds(Set<RemedialActionDependency> dependingEnabledRemedialActions) {
        return dependingEnabledRemedialActions.stream()
            .map(RemedialActionDependency::remedialAction)
            .sorted(String::compareTo)
            .collect(Collectors.joining(", "));
    }
}
