/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.GridStateAlterationCollection;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionDependency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionGroup;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionScheme;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RotatingMachineAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ShuntCompensatorModification;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.Stage;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.StaticPropertyRange;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.TapPositionAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.TopologyAction;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class ElementaryActionsHelper {
    private final Set<RemedialActionGroup> nativeRemedialActionGroups;
    private final Set<RemedialAction> nativeGridStateAlterationRemedialActions;
    private final Set<RemedialAction> nativeSchemeRemedialActions;
    private final Set<RemedialActionScheme> nativeRemedialActionSchemes;
    private final Set<Stage> nativeStages;
    private final Set<GridStateAlterationCollection> nativeGridStateAlterationCollections;
    private final Set<AssessedElementWithRemedialAction> nativeAssessedElementWithRemedialActions;
    private final Map<String, Set<RemedialActionDependency>> nativeRemedialActionDependencyPerNativeRemedialActionGroup;
    private final Map<String, Set<TopologyAction>> nativeTopologyActionsPerNativeRemedialAction;
    private final Map<String, Set<TopologyAction>> nativeTopologyActionsPerNativeRemedialActionAuto;
    private final Map<String, Set<RotatingMachineAction>> nativeRotatingMachineActionsPerNativeRemedialAction;
    private final Map<String, Set<RotatingMachineAction>> nativeRotatingMachineActionsPerNativeRemedialActionAuto;
    private final Map<String, Set<ShuntCompensatorModification>> nativeShuntCompensatorModificationsPerNativeRemedialAction;
    private final Map<String, Set<ShuntCompensatorModification>> nativeShuntCompensatorModificationsPerNativeRemedialActionAuto;
    private final Map<String, Set<TapPositionAction>> nativeTapPositionActionsPerNativeRemedialAction;
    private final Map<String, Set<TapPositionAction>> nativeTapPositionActionsPerNativeRemedialActionAuto;
    private final Map<String, Set<StaticPropertyRange>> nativeStaticPropertyRangesPerNativeGridStateAlteration;
    final Map<String, Set<ContingencyWithRemedialAction>> nativeContingencyWithRemedialActionPerNativeRemedialAction;

    public ElementaryActionsHelper(Set<RemedialAction> nativeGridStateAlterationRemedialActions,
                                   Set<RemedialAction> nativeSchemeRemedialActions,
                                   Set<RemedialActionScheme> nativeRemedialActionSchemes,
                                   Set<Stage> nativeStages,
                                   Set<GridStateAlterationCollection> nativeGridStateAlterationCollections,
                                   Set<AssessedElementWithRemedialAction> nativeAssessedElementWithRemedialActions,
                                   Set<ContingencyWithRemedialAction> nativeContingencyWithRemedialActions,
                                   Set<StaticPropertyRange> nativeStaticPropertyRanges,
                                   Set<TopologyAction> nativeTopologyActions,
                                   Set<RotatingMachineAction> nativeRotatingMachineActions,
                                   Set<ShuntCompensatorModification> nativeShuntCompensatorModifications,
                                   Set<TapPositionAction> nativeTapPositionActions,
                                   Set<RemedialActionGroup> nativeRemedialActionGroups,
                                   Set<RemedialActionDependency> nativeRemedialActionDependency) {
        this.nativeRemedialActionGroups = nativeRemedialActionGroups;
        this.nativeGridStateAlterationRemedialActions = nativeGridStateAlterationRemedialActions;
        this.nativeSchemeRemedialActions = nativeSchemeRemedialActions;
        this.nativeRemedialActionSchemes = nativeRemedialActionSchemes;
        this.nativeStages = nativeStages;
        this.nativeGridStateAlterationCollections = nativeGridStateAlterationCollections;
        this.nativeAssessedElementWithRemedialActions = nativeAssessedElementWithRemedialActions;

        this.nativeRemedialActionDependencyPerNativeRemedialActionGroup = mapRemedialActionDependenciesToRemedialActionGroups(nativeRemedialActionDependency);

        this.nativeContingencyWithRemedialActionPerNativeRemedialAction = mapContingencyWithRemedialActionToRemedialAction(nativeContingencyWithRemedialActions);
        this.nativeStaticPropertyRangesPerNativeGridStateAlteration = mapStaticPropertyRangesToGridStateAlterations(nativeStaticPropertyRanges); // the id here is the id of the subclass of gridStateAlteration (tapPositionAction, RotatingMachine, ..)

        this.nativeTopologyActionsPerNativeRemedialAction = mapTopologyActionsToRemedialActions(nativeTopologyActions, false);
        this.nativeRotatingMachineActionsPerNativeRemedialAction = mapRotatingMachineActionsToRemedialActions(nativeRotatingMachineActions, false);
        this.nativeShuntCompensatorModificationsPerNativeRemedialAction = mapShuntCompensatorModificationsToRemedialActions(nativeShuntCompensatorModifications, false);
        this.nativeTapPositionActionsPerNativeRemedialAction = mapTapPositionActionsToRemedialActions(nativeTapPositionActions, false);

        this.nativeTopologyActionsPerNativeRemedialActionAuto = mapTopologyActionsToRemedialActions(nativeTopologyActions, true);
        this.nativeRotatingMachineActionsPerNativeRemedialActionAuto = mapRotatingMachineActionsToRemedialActions(nativeRotatingMachineActions, true);
        this.nativeShuntCompensatorModificationsPerNativeRemedialActionAuto = mapShuntCompensatorModificationsToRemedialActions(nativeShuntCompensatorModifications, true);
        this.nativeTapPositionActionsPerNativeRemedialActionAuto = mapTapPositionActionsToRemedialActions(nativeTapPositionActions, true);

    }

    private Map<String, Set<StaticPropertyRange>> mapStaticPropertyRangesToGridStateAlterations(Set<StaticPropertyRange> nativeStaticPropertyRanges) {
        Map<String, Set<StaticPropertyRange>> staticPropertyRangePerGridStateAlteration = new HashMap<>();
        for (StaticPropertyRange nativeStaticPropertyRange : nativeStaticPropertyRanges) {
            Set<StaticPropertyRange> staticPropertyRanges = staticPropertyRangePerGridStateAlteration.computeIfAbsent(nativeStaticPropertyRange.gridStateAlteration(), k -> new HashSet<>());
            staticPropertyRanges.add(nativeStaticPropertyRange);
        }
        return staticPropertyRangePerGridStateAlteration;
    }

    private Map<String, Set<TopologyAction>> mapTopologyActionsToRemedialActions(Set<TopologyAction> nativeTopologyActions, boolean autoRemedialAction) {
        Map<String, Set<TopologyAction>> topologyActionPerRemedialAction = new HashMap<>();
        for (TopologyAction nativeTopologyAction : nativeTopologyActions) {
            String parentRemedialActionId = autoRemedialAction ? nativeTopologyAction.gridStateAlterationCollection() : nativeTopologyAction.gridStateAlterationRemedialAction();
            if (parentRemedialActionId != null) {
                Set<TopologyAction> topologyActions = topologyActionPerRemedialAction.computeIfAbsent(parentRemedialActionId, k -> new HashSet<>());
                topologyActions.add(nativeTopologyAction);
            }
        }
        return topologyActionPerRemedialAction;
    }

    private Map<String, Set<RotatingMachineAction>> mapRotatingMachineActionsToRemedialActions(Set<RotatingMachineAction> nativeRotatingMachineActions, boolean autoRemedialAction) {
        Map<String, Set<RotatingMachineAction>> rotatingMachineActionPerRemedialAction = new HashMap<>();
        for (RotatingMachineAction nativeRotatingMachineAction : nativeRotatingMachineActions) {
            String parentRemedialActionId = autoRemedialAction ? nativeRotatingMachineAction.gridStateAlterationCollection() : nativeRotatingMachineAction.gridStateAlterationRemedialAction();
            if (parentRemedialActionId != null) {
                Set<RotatingMachineAction> rotatingMachineActions = rotatingMachineActionPerRemedialAction.computeIfAbsent(parentRemedialActionId, k -> new HashSet<>());
                rotatingMachineActions.add(nativeRotatingMachineAction);
            }
        }
        return rotatingMachineActionPerRemedialAction;
    }

    private Map<String, Set<ShuntCompensatorModification>> mapShuntCompensatorModificationsToRemedialActions(Set<ShuntCompensatorModification> nativeShuntCompensatorModifications, boolean autoRemedialAction) {
        Map<String, Set<ShuntCompensatorModification>> shuntCompensatorModificationsPerRemedialAction = new HashMap<>();
        for (ShuntCompensatorModification nativeShuntCompensatorModification : nativeShuntCompensatorModifications) {
            String parentRemedialActionId = autoRemedialAction ? nativeShuntCompensatorModification.gridStateAlterationCollection() : nativeShuntCompensatorModification.gridStateAlterationRemedialAction();
            if (parentRemedialActionId != null) {
                Set<ShuntCompensatorModification> shuntCompensatorModifications = shuntCompensatorModificationsPerRemedialAction.computeIfAbsent(parentRemedialActionId, k -> new HashSet<>());
                shuntCompensatorModifications.add(nativeShuntCompensatorModification);
            }
        }
        return shuntCompensatorModificationsPerRemedialAction;
    }

    private Map<String, Set<TapPositionAction>> mapTapPositionActionsToRemedialActions(Set<TapPositionAction> nativeTapPositionActions, boolean autoRemedialAction) {
        Map<String, Set<TapPositionAction>> tapPositionActionPerRemedialAction = new HashMap<>();
        for (TapPositionAction nativeTapPositionAction : nativeTapPositionActions) {
            String parentRemedialActionId = autoRemedialAction ? nativeTapPositionAction.gridStateAlterationCollection() : nativeTapPositionAction.gridStateAlterationRemedialAction();
            if (parentRemedialActionId != null) {
                Set<TapPositionAction> tapPositionActions = tapPositionActionPerRemedialAction.computeIfAbsent(parentRemedialActionId, k -> new HashSet<>());
                tapPositionActions.add(nativeTapPositionAction);
            }
        }
        return tapPositionActionPerRemedialAction;
    }

    private Map<String, Set<ContingencyWithRemedialAction>> mapContingencyWithRemedialActionToRemedialAction(Set<ContingencyWithRemedialAction> nativeContingencyWithRemedialActions) {
        Map<String, Set<ContingencyWithRemedialAction>> contingencyWithRemedialActionsPerRemedialAction = new HashMap<>();
        for (ContingencyWithRemedialAction nativeContingencyWithRemedialAction : nativeContingencyWithRemedialActions) {
            Set<ContingencyWithRemedialAction> contingencies = contingencyWithRemedialActionsPerRemedialAction.computeIfAbsent(nativeContingencyWithRemedialAction.remedialAction(), k -> new HashSet<>());
            contingencies.add(nativeContingencyWithRemedialAction);
        }
        return contingencyWithRemedialActionsPerRemedialAction;
    }

    private Map<String, Set<RemedialActionDependency>> mapRemedialActionDependenciesToRemedialActionGroups(Set<RemedialActionDependency> nativeRemedialActionDependencies) {
        Map<String, Set<RemedialActionDependency>> remedialActionDependenciesPerRemedialActionGroup = new HashMap<>();
        for (RemedialActionDependency nativeRemedialActionDependency : nativeRemedialActionDependencies) {
            Set<RemedialActionDependency> remedialActionDependencies = remedialActionDependenciesPerRemedialActionGroup.computeIfAbsent(nativeRemedialActionDependency.dependingRemedialActionGroup(), k -> new HashSet<>());
            remedialActionDependencies.add(nativeRemedialActionDependency);
        }
        return remedialActionDependenciesPerRemedialActionGroup;
    }

    public Map<String, Set<RemedialActionDependency>> getNativeRemedialActionDependencyPerNativeRemedialActionGroup() {
        return nativeRemedialActionDependencyPerNativeRemedialActionGroup;
    }

    public Set<RemedialActionGroup> getRemedialActionGroupsPropertyBags() {
        return nativeRemedialActionGroups;
    }

    public Map<String, Set<StaticPropertyRange>> getNativeStaticPropertyRangesPerNativeGridStateAlteration() {
        return nativeStaticPropertyRangesPerNativeGridStateAlteration;
    }

    public Map<String, Set<ContingencyWithRemedialAction>> getContingenciesByRemedialAction() {
        return nativeContingencyWithRemedialActionPerNativeRemedialAction;
    }

    public Map<String, Set<TopologyAction>> getTopologyActions(boolean isSchemeRemedialAction) {
        return isSchemeRemedialAction ? nativeTopologyActionsPerNativeRemedialActionAuto : nativeTopologyActionsPerNativeRemedialAction;
    }

    public Map<String, Set<RotatingMachineAction>> getRotatingMachineActions(boolean isSchemeRemedialAction) {
        return isSchemeRemedialAction ? nativeRotatingMachineActionsPerNativeRemedialActionAuto : nativeRotatingMachineActionsPerNativeRemedialAction;
    }

    public Map<String, Set<ShuntCompensatorModification>> getShuntCompensatorModifications(boolean isSchemeRemedialAction) {
        return isSchemeRemedialAction ? nativeShuntCompensatorModificationsPerNativeRemedialActionAuto : nativeShuntCompensatorModificationsPerNativeRemedialAction;
    }

    public Map<String, Set<TapPositionAction>> getTapPositionActions(boolean isSchemeRemedialAction) {
        return isSchemeRemedialAction ? nativeTapPositionActionsPerNativeRemedialActionAuto : nativeTapPositionActionsPerNativeRemedialAction;
    }

    public Set<RemedialAction> getParentRemedialActionPropertyBags(boolean isSchemeRemedialAction) {
        return isSchemeRemedialAction ? nativeSchemeRemedialActions : nativeGridStateAlterationRemedialActions;
    }

    public String getGridStateAlterationCollection(String remedialActionId) {
        String remedialActionSchemeId = getAssociatedRemedialActionScheme(remedialActionId);
        return getAssociatedGridStateAlterationCollectionUsingStage(remedialActionId, remedialActionSchemeId);
    }

    private String getAssociatedGridStateAlterationCollectionUsingStage(String remedialActionId, String remedialActionScheme) {
        Stage nativeStage = getAssociatedStagePropertyBag(remedialActionId, remedialActionScheme);
        List<GridStateAlterationCollection> linkedGridStateAlterationCollectionPropertyBags = nativeGridStateAlterationCollections.stream().filter(nativeGridStateAlterationCollection -> nativeStage.gridStateAlterationCollection().equals(nativeGridStateAlterationCollection.mrid())).toList();
        if (linkedGridStateAlterationCollectionPropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it has no associated GridStateAlterationCollection");
        }
        return nativeStage.gridStateAlterationCollection();
    }

    private Stage getAssociatedStagePropertyBag(String remedialActionId, String remedialActionScheme) {
        List<Stage> linkedStagePropertyBags = nativeStages.stream().filter(stage -> remedialActionScheme.equals(stage.remedialActionScheme())).toList();
        if (linkedStagePropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it has no associated Stage");
        } else if (linkedStagePropertyBags.size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it has several conflictual Stages");
        }
        return linkedStagePropertyBags.get(0);
    }

    private String getAssociatedRemedialActionScheme(String remedialActionId) {
        List<RemedialActionScheme> linkedRemedialActionSchemePropertyBags = nativeRemedialActionSchemes.stream().filter(nativeRemedialActionScheme -> remedialActionId.equals(nativeRemedialActionScheme.schemeRemedialAction())).toList();
        if (linkedRemedialActionSchemePropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it has no associated RemedialActionScheme");
        } else if (linkedRemedialActionSchemePropertyBags.size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it has several conflictual RemedialActionSchemes");
        }

        RemedialActionScheme nativeRemedialActionScheme = linkedRemedialActionSchemePropertyBags.get(0);
        if (!CsaProfileConstants.SIPS.equals(nativeRemedialActionScheme.kind())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because of an unsupported kind for remedial action schedule (only SIPS allowed)");
        }
        if (!nativeRemedialActionScheme.normalArmed()) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "Remedial action " + remedialActionId + " will not be imported because RemedialActionScheme " + nativeRemedialActionScheme.mrid() + " is not armed");
        }
        return nativeRemedialActionScheme.mrid();
    }

    public boolean remedialActionIsLinkedToAssessedElements(String remedialActionId) {
        return nativeAssessedElementWithRemedialActions.stream().anyMatch(nativeAssessedElementWithRemedialAction -> nativeAssessedElementWithRemedialAction.remedialAction().equals(remedialActionId));
    }
}
