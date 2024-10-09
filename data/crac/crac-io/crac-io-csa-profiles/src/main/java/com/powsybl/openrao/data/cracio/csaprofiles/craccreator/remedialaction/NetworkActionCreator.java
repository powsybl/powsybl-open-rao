/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.GridStateAlteration;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.PropertyReference;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RelativeDirectionKind;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ValueOffsetKind;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RotatingMachineAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ShuntCompensatorModification;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.StaticPropertyRange;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.TopologyAction;
import com.powsybl.openrao.data.cracio.commons.OpenRaoImportException;
import com.powsybl.openrao.data.cracapi.networkaction.SingleNetworkElementActionAdder;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;

import java.util.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class NetworkActionCreator {
    private final Crac crac;
    private final Network network;
    private static final String STATIC_PROPERTY_RANGE_STRING = "StaticPropertyRange";

    public NetworkActionCreator(Crac crac, Network network) {
        this.crac = crac;
        this.network = network;
    }

    private enum ElementaryActionType {
        ROTATING_MACHINE_ACTION("RotatingMachineAction", PropertyReference.ROTATING_MACHINE),
        SHUNT_COMPENSATOR_MODIFICATION("ShuntCompensatorModification", PropertyReference.SHUNT_COMPENSATOR),
        TOPOLOGY_ACTION("TopologyAction", PropertyReference.SWITCH);

        private final String gridStateAlterationType;
        private final PropertyReference propertyReference;

        ElementaryActionType(String gridStateAlterationType, PropertyReference propertyReference) {
            this.gridStateAlterationType = gridStateAlterationType;
            this.propertyReference = propertyReference;
        }

        String getGridStateAlterationType() {
            return gridStateAlterationType;
        }

        PropertyReference getPropertyReference() {
            return propertyReference;
        }
    }

    public NetworkActionAdder getNetworkActionAdder(Set<TopologyAction> linkedTopologyActions, Set<RotatingMachineAction> linkedRotatingMachineActions, Set<ShuntCompensatorModification> linkedShuntCompensatorModifications, Map<String, Set<StaticPropertyRange>> staticPropertyRanges, String remedialActionId, List<String> alterations) {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(remedialActionId);
        linkedRotatingMachineActions.forEach(rotatingMachineAction -> addElementaryAction(ElementaryActionType.ROTATING_MACHINE_ACTION, rotatingMachineAction, remedialActionId, networkActionAdder, staticPropertyRanges, alterations));
        linkedShuntCompensatorModifications.forEach(shuntCompensatorModification -> addElementaryAction(ElementaryActionType.SHUNT_COMPENSATOR_MODIFICATION, shuntCompensatorModification, remedialActionId, networkActionAdder, staticPropertyRanges, alterations));
        linkedTopologyActions.forEach(topologyAction -> addElementaryAction(ElementaryActionType.TOPOLOGY_ACTION, topologyAction, remedialActionId, networkActionAdder, staticPropertyRanges, alterations));
        if (linkedRotatingMachineActions.stream().noneMatch(RotatingMachineAction::normalEnabled) && linkedShuntCompensatorModifications.stream().noneMatch(ShuntCompensatorModification::normalEnabled) && linkedTopologyActions.stream().noneMatch(TopologyAction::normalEnabled)) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, String.format("Remedial action %s will not be imported because it has no elementary action", remedialActionId));
        }
        return networkActionAdder;
    }

    private void addElementaryAction(ElementaryActionType elementaryActionType, GridStateAlteration gridStateAlteration, String remedialActionId, NetworkActionAdder networkActionAdder, Map<String, Set<StaticPropertyRange>> staticPropertyRanges, List<String> alterations) {
        if (Boolean.TRUE.equals(gridStateAlteration.normalEnabled())) {
            checkNetworkActionHasExactlyOneStaticPropertyRange(staticPropertyRanges, remedialActionId, gridStateAlteration.mrid());

            CsaProfileCracUtils.checkPropertyReference(remedialActionId, elementaryActionType.getGridStateAlterationType(), elementaryActionType.getPropertyReference(), gridStateAlteration.propertyReference());

            StaticPropertyRange nativeStaticPropertyRange = staticPropertyRanges.get(gridStateAlteration.mrid()).iterator().next();
            CsaProfileCracUtils.checkPropertyReference(remedialActionId, STATIC_PROPERTY_RANGE_STRING, elementaryActionType.getPropertyReference(), nativeStaticPropertyRange.propertyReference());

            if (gridStateAlteration instanceof RotatingMachineAction rotatingMachineAction) {
                addRotatingMachineInjectionSetPoint(remedialActionId, networkActionAdder, nativeStaticPropertyRange, rotatingMachineAction);
            } else if (gridStateAlteration instanceof ShuntCompensatorModification shuntCompensatorModification) {
                addShuntCompensatorInjectionSetPoint(remedialActionId, networkActionAdder, nativeStaticPropertyRange, shuntCompensatorModification);
            } else if (gridStateAlteration instanceof TopologyAction topologyAction) {
                addTopologicalAction(remedialActionId, networkActionAdder, nativeStaticPropertyRange, topologyAction);
            } else {
                throw new OpenRaoException("Unexpected GridStateAlteration type: %s.".formatted(gridStateAlteration.getClass().getSimpleName()));
            }
        } else {
            alterations.add("Elementary %s %s of GridStateAlterationRemedialAction %s ignored because it is disabled".formatted(elementaryActionType.getGridStateAlterationType(), gridStateAlteration.mrid(), remedialActionId));
        }
    }

    private void addRotatingMachineInjectionSetPoint(String remedialActionId, NetworkActionAdder networkActionAdder, StaticPropertyRange nativeStaticPropertyRange, RotatingMachineAction rotatingMachineAction) {
        Generator generator = network.getGenerator(rotatingMachineAction.rotatingMachineId());
        Load load = network.getLoad(rotatingMachineAction.rotatingMachineId());
        if (generator == null && load == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("Remedial action %s will not be imported because the network does not contain a generator, neither a load with id: %s", remedialActionId, rotatingMachineAction.rotatingMachineId()));
        }
        SingleNetworkElementActionAdder<?> actionAdder;
        if (generator != null) {
            double setPointValue = getInjectionSetPointValue(nativeStaticPropertyRange, remedialActionId, false, (float) generator.getTargetP());
            actionAdder = networkActionAdder.newGeneratorAction().withActivePowerValue(setPointValue);
        } else {
            double setPointValue = getInjectionSetPointValue(nativeStaticPropertyRange, remedialActionId, false, (float) load.getP0());
            actionAdder = networkActionAdder.newLoadAction().withActivePowerValue(setPointValue);
        }
        actionAdder.withNetworkElement(rotatingMachineAction.rotatingMachineId()).add();
    }

    private void addShuntCompensatorInjectionSetPoint(String remedialActionId, NetworkActionAdder networkActionAdder, StaticPropertyRange nativeStaticPropertyRange, ShuntCompensatorModification shuntCompensatorModification) {
        ShuntCompensator shuntCompensator = network.getShuntCompensator(shuntCompensatorModification.shuntCompensatorId());
        if (shuntCompensator == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action %s will not be imported because the network does not contain a shunt compensator with id: %s".formatted(remedialActionId, shuntCompensatorModification.shuntCompensatorId()));
        }
        double setPointValue = getInjectionSetPointValue(nativeStaticPropertyRange, remedialActionId, true, shuntCompensator.getSectionCount());
        networkActionAdder.newShuntCompensatorPositionAction()
            .withSectionCount((int) setPointValue)
            .withNetworkElement(shuntCompensatorModification.shuntCompensatorId())
            .add();
    }

    private void addTopologicalAction(String remedialActionId, NetworkActionAdder networkActionAdder, StaticPropertyRange nativeStaticPropertyRange, TopologyAction topologyAction) {
        if (network.getSwitch(topologyAction.switchId()) == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("Remedial action %s will not be imported because the network does not contain a switch with id: %s", remedialActionId, topologyAction.switchId()));
        }

        if (0d != nativeStaticPropertyRange.normalValue() && 1d != nativeStaticPropertyRange.normalValue()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because the normalValue is %s which does not define a proper action type (open 1 / close 0)", remedialActionId, nativeStaticPropertyRange.normalValue()));
        }

        if (!ValueOffsetKind.ABSOLUTE.toString().equals(nativeStaticPropertyRange.valueKind())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because the ValueOffsetKind is %s but should be none", remedialActionId, nativeStaticPropertyRange.valueKind()));
        }

        if (!RelativeDirectionKind.NONE.toString().equals(nativeStaticPropertyRange.direction())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because the RelativeDirectionKind is %s but should be absolute", remedialActionId, nativeStaticPropertyRange.direction()));
        }

        networkActionAdder.newSwitchAction()
            .withNetworkElement(topologyAction.switchId())
            .withActionType(nativeStaticPropertyRange.normalValue() == 0d ? ActionType.CLOSE : ActionType.OPEN).add();
    }

    private static void checkNetworkActionHasExactlyOneStaticPropertyRange(Map<String, Set<StaticPropertyRange>> staticPropertyRanges, String remedialActionId, String elementaryActionId) {
        if (!staticPropertyRanges.containsKey(elementaryActionId)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because there is no StaticPropertyRange linked to elementary action %s", remedialActionId, elementaryActionId));
        }
        if (staticPropertyRanges.get(elementaryActionId).size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because several conflictual StaticPropertyRanges are linked to elementary action %s", remedialActionId, elementaryActionId));
        }
    }

    private double getInjectionSetPointValue(StaticPropertyRange nativeStaticPropertyRange, String remedialActionId, boolean mustValueBePositiveInteger, float initialSetPoint) {
        checkInjectionStaticPropertyRangeAdequacy(remedialActionId, nativeStaticPropertyRange);

        double setPointValue;
        if (ValueOffsetKind.ABSOLUTE.toString().equals(nativeStaticPropertyRange.valueKind())) {
            setPointValue = nativeStaticPropertyRange.normalValue();
        } else if (ValueOffsetKind.INCREMENTAL.toString().equals(nativeStaticPropertyRange.valueKind())) {
            setPointValue = RelativeDirectionKind.UP.toString().equals(nativeStaticPropertyRange.direction()) ?
                initialSetPoint + nativeStaticPropertyRange.normalValue() :
                initialSetPoint - nativeStaticPropertyRange.normalValue();
        } else {
            setPointValue = RelativeDirectionKind.UP.toString().equals(nativeStaticPropertyRange.direction()) ?
                initialSetPoint + (nativeStaticPropertyRange.normalValue() * initialSetPoint) / 100d :
                initialSetPoint - (nativeStaticPropertyRange.normalValue() * initialSetPoint) / 100d;
        }

        if (mustValueBePositiveInteger) {
            if (setPointValue < 0d) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because of an incoherent negative number of sections", remedialActionId));
            }
            if (setPointValue != (int) setPointValue) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because of a non integer-castable number of sections", remedialActionId));
            }
        }
        return setPointValue;
    }

    private static void checkInjectionStaticPropertyRangeAdequacy(String remedialActionId, StaticPropertyRange nativeStaticPropertyRange) {
        if (ValueOffsetKind.ABSOLUTE.toString().equals(nativeStaticPropertyRange.valueKind()) && !RelativeDirectionKind.NONE.toString().equals(nativeStaticPropertyRange.direction())
            || !ValueOffsetKind.ABSOLUTE.toString().equals(nativeStaticPropertyRange.valueKind()) && RelativeDirectionKind.NONE.toString().equals(nativeStaticPropertyRange.direction())
            || RelativeDirectionKind.UP_AND_DOWN.toString().equals(nativeStaticPropertyRange.direction())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because its StaticPropertyRange uses an illegal combination of ValueOffsetKind and RelativeDirectionKind", remedialActionId));
        }
    }
}
