/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionGroup;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RotatingMachineAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ShuntCompensatorModification;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.StaticPropertyRange;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.TapPositionAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.TopologyAction;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

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
    private final PropertyBags gridStateAlterationRemedialActionPropertyBags;
    private final PropertyBags schemeRemedialActionsPropertyBags;
    private final PropertyBags remedialActionSchemePropertyBags;
    private final PropertyBags stagePropertyBags;
    private final PropertyBags gridStateAlterationCollectionPropertyBags;
    private final PropertyBags assessedElementWithRemedialActionPropertyBags;
    private final Map<String, Set<PropertyBag>> remedialActionDependenciesByGroup;
    private final Map<String, Set<TopologyAction>> nativeTopologyActionsPerNativeRemedialAction;
    private final Map<String, Set<TopologyAction>> nativeTopologyActionsPerNativeRemedialActionAuto;
    private final Map<String, Set<RotatingMachineAction>> nativeRotatingMachineActionsPerNativeRemedialAction;
    private final Map<String, Set<RotatingMachineAction>> nativeRotatingMachineActionsPerNativeRemedialActionAuto;
    private final Map<String, Set<ShuntCompensatorModification>> nativeShuntCompensatorModificationsPerNativeRemedialAction;
    private final Map<String, Set<ShuntCompensatorModification>> nativeShuntCompensatorModificationsPerNativeRemedialActionAuto;
    private final Map<String, Set<TapPositionAction>> nativeTapPositionActionsPerNativeRemedialAction;
    private final Map<String, Set<TapPositionAction>> nativeTapPositionActionsPerNativeRemedialActionAuto;
    private final Map<String, Set<StaticPropertyRange>> nativeStaticPropertyRangesPerNativeGridStateAlteration;
    final Map<String, Set<PropertyBag>> linkedContingencyWithRAs;

    public ElementaryActionsHelper(PropertyBags gridStateAlterationRemedialActionPropertyBags,
                                   PropertyBags schemeRemedialActionsPropertyBags,
                                   PropertyBags remedialActionSchemePropertyBags,
                                   PropertyBags stagePropertyBags,
                                   PropertyBags gridStateAlterationCollectionPropertyBags,
                                   PropertyBags assessedElementWithRemedialActionPropertyBags,
                                   PropertyBags contingencyWithRemedialActionsPropertyBags,
                                   Set<StaticPropertyRange> nativeStaticPropertyRanges,
                                   Set<TopologyAction> nativeTopologyActions,
                                   Set<RotatingMachineAction> nativeRotatingMachineActions,
                                   Set<ShuntCompensatorModification> nativeShuntCompensatorModifications,
                                   Set<TapPositionAction> nativeTapPositionActions,
                                   Set<RemedialActionGroup> nativeRemedialActionGroups,
                                   PropertyBags remedialActionDependenciesPropertyBags) {
        this.nativeRemedialActionGroups = nativeRemedialActionGroups;
        this.gridStateAlterationRemedialActionPropertyBags = gridStateAlterationRemedialActionPropertyBags;
        this.schemeRemedialActionsPropertyBags = schemeRemedialActionsPropertyBags;
        this.remedialActionSchemePropertyBags = remedialActionSchemePropertyBags;
        this.stagePropertyBags = stagePropertyBags;
        this.gridStateAlterationCollectionPropertyBags = gridStateAlterationCollectionPropertyBags;
        this.assessedElementWithRemedialActionPropertyBags = assessedElementWithRemedialActionPropertyBags;

        this.remedialActionDependenciesByGroup = CsaProfileCracUtils.getMappedPropertyBagsSet(remedialActionDependenciesPropertyBags, CsaProfileConstants.DEPENDING_REMEDIAL_ACTION_GROUP);

        this.linkedContingencyWithRAs = CsaProfileCracUtils.getMappedPropertyBagsSet(contingencyWithRemedialActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
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

    public Map<String, Set<PropertyBag>> getRemedialActionDependenciesByGroup() {
        return remedialActionDependenciesByGroup;
    }

    public Set<RemedialActionGroup> getRemedialActionGroupsPropertyBags() {
        return nativeRemedialActionGroups;
    }

    public Map<String, Set<StaticPropertyRange>> getNativeStaticPropertyRangesPerNativeGridStateAlteration() {
        return nativeStaticPropertyRangesPerNativeGridStateAlteration;
    }

    public Map<String, Set<PropertyBag>> getContingenciesByRemedialAction() {
        return linkedContingencyWithRAs;
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

    public PropertyBags getParentRemedialActionPropertyBags(boolean isSchemeRemedialAction) {
        return isSchemeRemedialAction ? schemeRemedialActionsPropertyBags : gridStateAlterationRemedialActionPropertyBags;
    }

    public String getGridStateAlterationCollection(String remedialActionId) {
        String remedialActionSchemeId = getAssociatedRemedialActionScheme(remedialActionId);
        return getAssociatedGridStateAlterationCollectionUsingStage(remedialActionId, remedialActionSchemeId);
    }

    private String getAssociatedGridStateAlterationCollectionUsingStage(String remedialActionId, String remedialActionScheme) {
        PropertyBag stagePropertyBag = getAssociatedStagePropertyBag(remedialActionId, remedialActionScheme);
        String gridStateAlterationCollection = stagePropertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        List<PropertyBag> linkedGridStateAlterationCollectionPropertyBags = gridStateAlterationCollectionPropertyBags.stream().filter(pb -> gridStateAlterationCollection.equals(pb.get(CsaProfileConstants.MRID))).toList();
        if (linkedGridStateAlterationCollectionPropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it has no associated GridStateAlterationCollection");
        }
        return gridStateAlterationCollection;
    }

    private PropertyBag getAssociatedStagePropertyBag(String remedialActionId, String remedialActionScheme) {
        List<PropertyBag> linkedStagePropertyBags = stagePropertyBags.stream().filter(pb -> remedialActionScheme.equals(pb.getId(CsaProfileConstants.REMEDIAL_ACTION_SCHEME))).toList();
        if (linkedStagePropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it has no associated Stage");
        } else if (linkedStagePropertyBags.size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it has several conflictual Stages");
        }
        return linkedStagePropertyBags.get(0);
    }

    private String getAssociatedRemedialActionScheme(String remedialActionId) {
        List<PropertyBag> linkedRemedialActionSchemePropertyBags = remedialActionSchemePropertyBags.stream().filter(pb -> remedialActionId.equals(pb.getId(CsaProfileConstants.SCHEME_REMEDIAL_ACTION))).toList();
        if (linkedRemedialActionSchemePropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it has no associated RemedialActionScheme");
        } else if (linkedRemedialActionSchemePropertyBags.size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because it has several conflictual RemedialActionSchemes");
        }

        PropertyBag remedialActionSchemePropertyBag = linkedRemedialActionSchemePropertyBags.get(0);
        String remedialActionSchemeId = remedialActionSchemePropertyBag.getId(CsaProfileConstants.MRID);

        String remedialActionSchemeKind = remedialActionSchemePropertyBag.get(CsaProfileConstants.KIND);
        if (!remedialActionSchemeKind.equals(CsaProfileConstants.SIPS)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because of an unsupported kind for remedial action schedule (only SIPS allowed)");
        }
        String remedialActionSchemeNormalArmed = remedialActionSchemePropertyBag.get(CsaProfileConstants.NORMAL_ARMED);
        if (!Boolean.parseBoolean(remedialActionSchemeNormalArmed)) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "Remedial action " + remedialActionId + " will not be imported because RemedialActionScheme " + remedialActionSchemeId + " is not armed");
        }
        return remedialActionSchemeId;
    }

    public boolean remedialActionIsLinkedToAssessedElements(String remedialActionId) {
        return assessedElementWithRemedialActionPropertyBags.stream().anyMatch(propertyBag -> remedialActionId.equals(propertyBag.getId("remedialAction")));
    }
}
