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
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class ElementaryActionsHelper {
    private final PropertyBags remedialActionGroupsPropertyBags;
    private final PropertyBags gridStateAlterationRemedialActionPropertyBags;
    private final PropertyBags schemeRemedialActionsPropertyBags;
    private final PropertyBags remedialActionSchemePropertyBags;
    private final PropertyBags stagePropertyBags;
    private final PropertyBags gridStateAlterationCollectionPropertyBags;
    private final PropertyBags assessedElementWithRemedialActionPropertyBags;
    private final Map<String, Set<PropertyBag>> remedialActionDependenciesByGroup;
    private final Map<String, Set<PropertyBag>> linkedTopologyActions;
    private final Map<String, Set<PropertyBag>> linkedTopologyActionsAuto;
    private final Map<String, Set<PropertyBag>> linkedRotatingMachineActions;
    private final Map<String, Set<PropertyBag>> linkedRotatingMachineActionsAuto;
    private final Map<String, Set<PropertyBag>> linkedShuntCompensatorModification;
    private final Map<String, Set<PropertyBag>> linkedShuntCompensatorModificationAuto;
    private final Map<String, Set<PropertyBag>> linkedTapPositionActions;
    private final Map<String, Set<PropertyBag>> linkedTapPositionActionsAuto;
    private final Map<String, Set<PropertyBag>> linkedStaticPropertyRanges;
    final Map<String, Set<PropertyBag>> linkedContingencyWithRAs;

    public ElementaryActionsHelper(PropertyBags gridStateAlterationRemedialActionPropertyBags,
                                   PropertyBags schemeRemedialActionsPropertyBags,
                                   PropertyBags remedialActionSchemePropertyBags,
                                   PropertyBags stagePropertyBags,
                                   PropertyBags gridStateAlterationCollectionPropertyBags,
                                   PropertyBags assessedElementWithRemedialActionPropertyBags,
                                   PropertyBags contingencyWithRemedialActionsPropertyBags,
                                   PropertyBags staticPropertyRangesPropertyBags,
                                   PropertyBags topologyActionsPropertyBags,
                                   PropertyBags rotatingMachineActionsPropertyBags,
                                   PropertyBags shuntCompensatorModificationPropertyBags,
                                   PropertyBags tapPositionActionsPropertyBags,
                                   PropertyBags remedialActionGroupsPropertyBags,
                                   PropertyBags remedialActionDependenciesPropertyBags) {
        this.remedialActionGroupsPropertyBags = remedialActionGroupsPropertyBags;
        this.gridStateAlterationRemedialActionPropertyBags = gridStateAlterationRemedialActionPropertyBags;
        this.schemeRemedialActionsPropertyBags = schemeRemedialActionsPropertyBags;
        this.remedialActionSchemePropertyBags = remedialActionSchemePropertyBags;
        this.stagePropertyBags = stagePropertyBags;
        this.gridStateAlterationCollectionPropertyBags = gridStateAlterationCollectionPropertyBags;
        this.assessedElementWithRemedialActionPropertyBags = assessedElementWithRemedialActionPropertyBags;

        this.remedialActionDependenciesByGroup = CsaProfileCracUtils.groupPropertyBagsBy(remedialActionDependenciesPropertyBags, CsaProfileConstants.DEPENDING_REMEDIAL_ACTION_GROUP);

        this.linkedContingencyWithRAs = CsaProfileCracUtils.groupPropertyBagsBy(contingencyWithRemedialActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        this.linkedStaticPropertyRanges = CsaProfileCracUtils.groupPropertyBagsBy(staticPropertyRangesPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION); // the id here is the id of the subclass of gridStateAlteration (tapPositionAction, RotatingMachine, ..)

        this.linkedTopologyActions = CsaProfileCracUtils.groupPropertyBagsBy(filterElementaryActions(topologyActionsPropertyBags, false), CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        this.linkedRotatingMachineActions = CsaProfileCracUtils.groupPropertyBagsBy(filterElementaryActions(rotatingMachineActionsPropertyBags, false), CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        this.linkedShuntCompensatorModification = CsaProfileCracUtils.groupPropertyBagsBy(filterElementaryActions(shuntCompensatorModificationPropertyBags, false), CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        this.linkedTapPositionActions = CsaProfileCracUtils.groupPropertyBagsBy(filterElementaryActions(tapPositionActionsPropertyBags, false), CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);

        this.linkedTopologyActionsAuto = CsaProfileCracUtils.groupPropertyBagsBy(filterElementaryActions(topologyActionsPropertyBags, true), CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        this.linkedRotatingMachineActionsAuto = CsaProfileCracUtils.groupPropertyBagsBy(filterElementaryActions(rotatingMachineActionsPropertyBags, true), CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        this.linkedShuntCompensatorModificationAuto = CsaProfileCracUtils.groupPropertyBagsBy(filterElementaryActions(shuntCompensatorModificationPropertyBags, true), CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        this.linkedTapPositionActionsAuto = CsaProfileCracUtils.groupPropertyBagsBy(filterElementaryActions(tapPositionActionsPropertyBags, true), CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);

    }

    private PropertyBags filterElementaryActions(PropertyBags elementaryActionsPropertyBags, boolean autoRemedialAction) {
        String parentRemedialActionField = autoRemedialAction ? CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION : CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION;
        Set<PropertyBag> relevantElementaryActionsPropertyBags = elementaryActionsPropertyBags.stream().filter(propertyBag -> Optional.ofNullable(propertyBag.get(parentRemedialActionField)).isPresent()).collect(Collectors.toSet());
        return new PropertyBags(relevantElementaryActionsPropertyBags);
    }

    public Map<String, Set<PropertyBag>> getRemedialActionDependenciesByGroup() {
        return remedialActionDependenciesByGroup;
    }

    public PropertyBags getRemedialActionGroupsPropertyBags() {
        return remedialActionGroupsPropertyBags;
    }

    public Map<String, Set<PropertyBag>> getStaticPropertyRangesByElementaryActionsAggregator() {
        return linkedStaticPropertyRanges;
    }

    public Map<String, Set<PropertyBag>> getContingenciesByRemedialAction() {
        return linkedContingencyWithRAs;
    }

    public Map<String, Set<PropertyBag>> getTopologyActions(boolean isSchemeRemedialAction) {
        return isSchemeRemedialAction ? linkedTopologyActionsAuto : linkedTopologyActions;
    }

    public Map<String, Set<PropertyBag>> getRotatingMachineActions(boolean isSchemeRemedialAction) {
        return isSchemeRemedialAction ? linkedRotatingMachineActionsAuto : linkedRotatingMachineActions;
    }

    public Map<String, Set<PropertyBag>> getShuntCompensatorModifications(boolean isSchemeRemedialAction) {
        return isSchemeRemedialAction ? linkedShuntCompensatorModificationAuto : linkedShuntCompensatorModification;
    }

    public Map<String, Set<PropertyBag>> getTapPositionActions(boolean isSchemeRemedialAction) {
        return isSchemeRemedialAction ? linkedTapPositionActionsAuto : linkedTapPositionActions;
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
