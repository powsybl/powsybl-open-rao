/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.csaprofiles.CsaProfileCrac;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileConstants;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.NcAggregator;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.GridStateAlterationCollection;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.GridStateAlterationRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialActionDependency;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialActionGroup;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialActionScheme;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RotatingMachineAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.SchemeRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ShuntCompensatorModification;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.Stage;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.StaticPropertyRange;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.TapPositionAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.TopologyAction;
import com.powsybl.openrao.data.cracio.commons.OpenRaoImportException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class ElementaryActionsHelper {
    private final Set<RemedialActionGroup> nativeRemedialActionGroups;
    private final Set<GridStateAlterationRemedialAction> nativeGridStateAlterationRemedialActions;
    private final Set<SchemeRemedialAction> nativeSchemeRemedialActions;
    private final Set<RemedialActionScheme> nativeRemedialActionSchemes;
    private final Set<Stage> nativeStages;
    private final Set<GridStateAlterationCollection> nativeGridStateAlterationCollections;
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

    public ElementaryActionsHelper(CsaProfileCrac nativeCrac) {
        this.nativeRemedialActionGroups = nativeCrac.getRemedialActionGroups();
        this.nativeGridStateAlterationRemedialActions = nativeCrac.getGridStateAlterationRemedialActions();
        this.nativeSchemeRemedialActions = nativeCrac.getSchemeRemedialActions();
        this.nativeRemedialActionSchemes = nativeCrac.getRemedialActionSchemes();
        this.nativeStages = nativeCrac.getStages();
        this.nativeGridStateAlterationCollections = nativeCrac.getGridStateAlterationCollections();

        this.nativeRemedialActionDependencyPerNativeRemedialActionGroup = new NcAggregator<>(RemedialActionDependency::dependingRemedialActionGroup).aggregate(nativeCrac.getRemedialActionDependencies());

        this.nativeContingencyWithRemedialActionPerNativeRemedialAction = new NcAggregator<>(ContingencyWithRemedialAction::remedialAction).aggregate(nativeCrac.getContingencyWithRemedialActions());
        this.nativeStaticPropertyRangesPerNativeGridStateAlteration = new NcAggregator<>(StaticPropertyRange::gridStateAlteration).aggregate(nativeCrac.getStaticPropertyRanges()); // the id here is the id of the subclass of gridStateAlteration (tapPositionAction, RotatingMachine, ..)

        this.nativeTopologyActionsPerNativeRemedialAction = new NcAggregator<>(TopologyAction::gridStateAlterationRemedialAction).aggregate(nativeCrac.getTopologyActions());
        this.nativeRotatingMachineActionsPerNativeRemedialAction = new NcAggregator<>(RotatingMachineAction::gridStateAlterationRemedialAction).aggregate(nativeCrac.getRotatingMachineActions());
        this.nativeShuntCompensatorModificationsPerNativeRemedialAction = new NcAggregator<>(ShuntCompensatorModification::gridStateAlterationRemedialAction).aggregate(nativeCrac.getShuntCompensatorModifications());
        this.nativeTapPositionActionsPerNativeRemedialAction = new NcAggregator<>(TapPositionAction::gridStateAlterationRemedialAction).aggregate(nativeCrac.getTapPositionActions());

        this.nativeTopologyActionsPerNativeRemedialActionAuto = new NcAggregator<>(TopologyAction::gridStateAlterationCollection).aggregate(nativeCrac.getTopologyActions());
        this.nativeRotatingMachineActionsPerNativeRemedialActionAuto = new NcAggregator<>(RotatingMachineAction::gridStateAlterationCollection).aggregate(nativeCrac.getRotatingMachineActions());
        this.nativeShuntCompensatorModificationsPerNativeRemedialActionAuto = new NcAggregator<>(ShuntCompensatorModification::gridStateAlterationCollection).aggregate(nativeCrac.getShuntCompensatorModifications());
        this.nativeTapPositionActionsPerNativeRemedialActionAuto = new NcAggregator<>(TapPositionAction::gridStateAlterationCollection).aggregate(nativeCrac.getTapPositionActions());

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

    public Set<? extends RemedialAction> getParentRemedialAction(boolean isSchemeRemedialAction) {
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
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because it has no associated GridStateAlterationCollection", remedialActionId));
        }
        return nativeStage.gridStateAlterationCollection();
    }

    private Stage getAssociatedStagePropertyBag(String remedialActionId, String remedialActionScheme) {
        List<Stage> linkedStagePropertyBags = nativeStages.stream().filter(stage -> remedialActionScheme.equals(stage.remedialActionScheme())).toList();
        if (linkedStagePropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because it has no associated Stage", remedialActionId));
        } else if (linkedStagePropertyBags.size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because it has several conflictual Stages", remedialActionId));
        }
        return linkedStagePropertyBags.get(0);
    }

    private String getAssociatedRemedialActionScheme(String remedialActionId) {
        List<RemedialActionScheme> linkedRemedialActionSchemePropertyBags = nativeRemedialActionSchemes.stream().filter(nativeRemedialActionScheme -> remedialActionId.equals(nativeRemedialActionScheme.schemeRemedialAction())).toList();
        if (linkedRemedialActionSchemePropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because it has no associated RemedialActionScheme", remedialActionId));
        } else if (linkedRemedialActionSchemePropertyBags.size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because it has several conflictual RemedialActionSchemes", remedialActionId));
        }

        RemedialActionScheme nativeRemedialActionScheme = linkedRemedialActionSchemePropertyBags.get(0);
        if (!CsaProfileConstants.SIPS.equals(nativeRemedialActionScheme.kind())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because of an unsupported kind for remedial action schedule (only SIPS allowed)", remedialActionId));
        }
        if (!nativeRemedialActionScheme.normalArmed()) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, String.format("Remedial action %s will not be imported because RemedialActionScheme %s is not armed", remedialActionId, nativeRemedialActionScheme.mrid()));
        }
        return nativeRemedialActionScheme.mrid();
    }
}
