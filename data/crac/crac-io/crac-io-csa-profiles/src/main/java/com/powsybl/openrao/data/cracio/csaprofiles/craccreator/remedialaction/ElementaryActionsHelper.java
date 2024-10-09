/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.openrao.data.cracio.csaprofiles.CsaProfileCrac;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialActionDependency;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialActionGroup;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RotatingMachineAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ShuntCompensatorModification;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.StaticPropertyRange;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.TapPositionAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.TopologyAction;

import java.util.Map;
import java.util.Set;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class ElementaryActionsHelper {
    private final Set<RemedialActionGroup> nativeRemedialActionGroups;
    private final Map<String, Set<RemedialActionDependency>> nativeRemedialActionDependencyPerNativeRemedialActionGroup;
    private final Map<String, Set<TopologyAction>> nativeTopologyActionsPerNativeRemedialAction;
    private final Map<String, Set<RotatingMachineAction>> nativeRotatingMachineActionsPerNativeRemedialAction;
    private final Map<String, Set<ShuntCompensatorModification>> nativeShuntCompensatorModificationsPerNativeRemedialAction;
    private final Map<String, Set<TapPositionAction>> nativeTapPositionActionsPerNativeRemedialAction;
    private final Map<String, Set<StaticPropertyRange>> nativeStaticPropertyRangesPerNativeGridStateAlteration;
    final Map<String, Set<ContingencyWithRemedialAction>> nativeContingencyWithRemedialActionPerNativeRemedialAction;

    public ElementaryActionsHelper(CsaProfileCrac nativeCrac) {
        this.nativeRemedialActionGroups = nativeCrac.getNativeObjects(RemedialActionGroup.class);
        this.nativeRemedialActionDependencyPerNativeRemedialActionGroup = CsaProfileCracUtils.aggregateBy(nativeCrac.getNativeObjects(RemedialActionDependency.class), RemedialActionDependency::dependingRemedialActionGroup);

        this.nativeContingencyWithRemedialActionPerNativeRemedialAction = CsaProfileCracUtils.aggregateBy(nativeCrac.getNativeObjects(ContingencyWithRemedialAction.class), ContingencyWithRemedialAction::remedialAction);
        this.nativeStaticPropertyRangesPerNativeGridStateAlteration = CsaProfileCracUtils.aggregateBy(nativeCrac.getNativeObjects(StaticPropertyRange.class), StaticPropertyRange::gridStateAlteration); // the id here is the id of the subclass of gridStateAlteration (tapPositionAction, RotatingMachine, ..)

        this.nativeTopologyActionsPerNativeRemedialAction = CsaProfileCracUtils.aggregateBy(nativeCrac.getNativeObjects(TopologyAction.class), TopologyAction::gridStateAlterationRemedialAction);
        this.nativeRotatingMachineActionsPerNativeRemedialAction = CsaProfileCracUtils.aggregateBy(nativeCrac.getNativeObjects(RotatingMachineAction.class), RotatingMachineAction::gridStateAlterationRemedialAction);
        this.nativeShuntCompensatorModificationsPerNativeRemedialAction = CsaProfileCracUtils.aggregateBy(nativeCrac.getNativeObjects(ShuntCompensatorModification.class), ShuntCompensatorModification::gridStateAlterationRemedialAction);
        this.nativeTapPositionActionsPerNativeRemedialAction = CsaProfileCracUtils.aggregateBy(nativeCrac.getNativeObjects(TapPositionAction.class), TapPositionAction::gridStateAlterationRemedialAction);
    }

    public Map<String, Set<RemedialActionDependency>> getNativeRemedialActionDependencyPerNativeRemedialActionGroup() {
        return nativeRemedialActionDependencyPerNativeRemedialActionGroup;
    }

    public Set<RemedialActionGroup> getRemedialActionGroups() {
        return nativeRemedialActionGroups;
    }

    public Map<String, Set<StaticPropertyRange>> getNativeStaticPropertyRangesPerNativeGridStateAlteration() {
        return nativeStaticPropertyRangesPerNativeGridStateAlteration;
    }

    public Map<String, Set<TopologyAction>> getTopologyActions() {
        return nativeTopologyActionsPerNativeRemedialAction;
    }

    public Map<String, Set<RotatingMachineAction>> getRotatingMachineActions() {
        return nativeRotatingMachineActionsPerNativeRemedialAction;
    }

    public Map<String, Set<ShuntCompensatorModification>> getShuntCompensatorModifications() {
        return nativeShuntCompensatorModificationsPerNativeRemedialAction;
    }

    public Map<String, Set<TapPositionAction>> getTapPositionActions() {
        return nativeTapPositionActionsPerNativeRemedialAction;
    }
}
