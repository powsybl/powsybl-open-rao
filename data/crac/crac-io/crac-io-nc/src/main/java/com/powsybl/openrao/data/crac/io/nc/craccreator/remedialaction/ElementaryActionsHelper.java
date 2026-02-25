/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.openrao.data.crac.io.nc.NcCrac;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcAggregator;
import com.powsybl.openrao.data.crac.io.nc.objects.*;

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

    public ElementaryActionsHelper(NcCrac nativeCrac) {
        this.nativeRemedialActionGroups = nativeCrac.getRemedialActionGroups();

        this.nativeRemedialActionDependencyPerNativeRemedialActionGroup = new NcAggregator<>(RemedialActionDependency::dependingRemedialActionGroup)
            .aggregate(nativeCrac.getRemedialActionDependencies());

        this.nativeContingencyWithRemedialActionPerNativeRemedialAction = new NcAggregator<>(ContingencyWithRemedialAction::remedialAction)
            .aggregate(nativeCrac.getContingencyWithRemedialActions());
        this.nativeStaticPropertyRangesPerNativeGridStateAlteration = new NcAggregator<>(StaticPropertyRange::gridStateAlteration)
            .aggregate(nativeCrac.getStaticPropertyRanges()); // the id here is the id of the subclass of gridStateAlteration (tapPositionAction, RotatingMachine, ..)

        this.nativeTopologyActionsPerNativeRemedialAction = new NcAggregator<>(TopologyAction::gridStateAlterationRemedialAction)
            .aggregate(nativeCrac.getTopologyActions());
        this.nativeRotatingMachineActionsPerNativeRemedialAction = new NcAggregator<>(RotatingMachineAction::gridStateAlterationRemedialAction)
            .aggregate(nativeCrac.getRotatingMachineActions());
        this.nativeShuntCompensatorModificationsPerNativeRemedialAction = new NcAggregator<>(ShuntCompensatorModification::gridStateAlterationRemedialAction)
            .aggregate(nativeCrac.getShuntCompensatorModifications());
        this.nativeTapPositionActionsPerNativeRemedialAction = new NcAggregator<>(TapPositionAction::gridStateAlterationRemedialAction)
            .aggregate(nativeCrac.getTapPositionActions());
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
