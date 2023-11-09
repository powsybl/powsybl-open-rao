/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.util.FaraoImportException;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class NetworkActionCreator {
    private final Crac crac;
    private final Network network;

    public NetworkActionCreator(Crac crac, Network network) {
        this.crac = crac;
        this.network = network;
    }

    public NetworkActionAdder getNetworkActionAdder(Map<String, Set<PropertyBag>> linkedTopologyActions, Map<String, Set<PropertyBag>> linkedRotatingMachineActions, Map<String, Set<PropertyBag>> linkedShuntCompensatorModifications, Map<String, Set<PropertyBag>> staticPropertyRanges, String remedialActionId) {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(remedialActionId);
        if (linkedTopologyActions.containsKey(remedialActionId)) {
            for (PropertyBag topologyActionPropertyBag : linkedTopologyActions.get(remedialActionId)) {
                addTopologicalElementaryAction(networkActionAdder, topologyActionPropertyBag, remedialActionId);
            }
        }

        if (linkedRotatingMachineActions.containsKey(remedialActionId)) {
            for (PropertyBag rotatingMachineActionPropertyBag : linkedRotatingMachineActions.get(remedialActionId)) {
                if (staticPropertyRanges.containsKey(rotatingMachineActionPropertyBag.getId(CsaProfileConstants.MRID))) {
                    addInjectionSetPointFromRotatingMachineAction(
                        staticPropertyRanges.get(rotatingMachineActionPropertyBag.getId(CsaProfileConstants.MRID)),
                        remedialActionId, networkActionAdder, rotatingMachineActionPropertyBag);
                } else {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because there is no StaticPropertyRange linked to that RA");
                }
            }
        }
        if (linkedShuntCompensatorModifications.containsKey(remedialActionId)) {
            for (PropertyBag shuntCompensatorModificationPropertyBag : linkedShuntCompensatorModifications.get(remedialActionId)) {
                if (staticPropertyRanges.containsKey(shuntCompensatorModificationPropertyBag.getId(CsaProfileConstants.MRID))) {
                    addInjectionSetPointFromShuntCompensatorModification(
                        staticPropertyRanges.get(shuntCompensatorModificationPropertyBag.getId(CsaProfileConstants.MRID)),
                        remedialActionId, networkActionAdder, shuntCompensatorModificationPropertyBag);
                } else {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because there is no StaticPropertyRange linked to that RA");
                }
            }
        }
        return networkActionAdder;
    }

    private void addInjectionSetPointFromRotatingMachineAction(Set<PropertyBag> staticPropertyRangesLinkedToRotatingMachineAction, String remedialActionId, NetworkActionAdder networkActionAdder, PropertyBag rotatingMachineActionPropertyBag) {
        CsaProfileCracUtils.checkNormalEnabled(rotatingMachineActionPropertyBag, remedialActionId, "RotatingMachineAction");
        CsaProfileCracUtils.checkPropertyReference(rotatingMachineActionPropertyBag, remedialActionId, "RotatingMachineAction", CsaProfileConstants.PropertyReference.ROTATING_MACHINE.toString());
        String rawId = rotatingMachineActionPropertyBag.get(CsaProfileConstants.ROTATING_MACHINE);
        String rotatingMachineId = rawId.substring(rawId.lastIndexOf("_") + 1);
        float initialSetPoint = getInitialSetPoint(rotatingMachineId, remedialActionId);

        PropertyBag staticPropertyRangePropertyBag = staticPropertyRangesLinkedToRotatingMachineAction.iterator().next(); // get a random one because there is only one
        CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PropertyReference.ROTATING_MACHINE.toString());
        float setPointValue = getSetPointValue(staticPropertyRangePropertyBag, remedialActionId, false, initialSetPoint);

        networkActionAdder.newInjectionSetPoint()
            .withSetpoint(setPointValue)
            .withNetworkElement(rotatingMachineId)
            .withUnit(Unit.MEGAWATT)
            .add();
    }

    private void addInjectionSetPointFromShuntCompensatorModification(Set<PropertyBag> staticPropertyRangesLinkedToShuntCompensatorModification, String remedialActionId, NetworkActionAdder networkActionAdder, PropertyBag shuntCompensatorModificationPropertyBag) {
        CsaProfileCracUtils.checkNormalEnabled(shuntCompensatorModificationPropertyBag, remedialActionId, "ShuntCompensatorModification");
        CsaProfileCracUtils.checkPropertyReference(shuntCompensatorModificationPropertyBag, remedialActionId, "ShuntCompensatorModification", CsaProfileConstants.PropertyReference.SHUNT_COMPENSATOR.toString());
        String rawId = shuntCompensatorModificationPropertyBag.get(CsaProfileConstants.SHUNT_COMPENSATOR_ID);
        String shuntCompensatorId = rawId.substring(rawId.lastIndexOf("_") + 1);
        float initialSetPoint = getInitialSetPoint(shuntCompensatorId, remedialActionId);

        PropertyBag staticPropertyRangePropertyBag = staticPropertyRangesLinkedToShuntCompensatorModification.iterator().next(); // get a random one because there is only one
        CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PropertyReference.SHUNT_COMPENSATOR.toString());
        float setPointValue = getSetPointValue(staticPropertyRangePropertyBag, remedialActionId, true, initialSetPoint);
        networkActionAdder.newInjectionSetPoint()
            .withSetpoint(setPointValue)
            .withNetworkElement(shuntCompensatorId)
            .withUnit(Unit.MEGAWATT)
            .add();
    }

    private float getInitialSetPoint(String injectionSetPointActionId, String remedialActionId) {
        float initialSetPoint = 0f;
        Optional<Generator> optionalGenerator = network.getGeneratorStream().filter(gen -> gen.getId().equals(injectionSetPointActionId)).findAny();
        Optional<Load> optionalLoad = findLoad(injectionSetPointActionId);
        ShuntCompensator shuntCompensator = network.getShuntCompensator(injectionSetPointActionId);
        if (optionalGenerator.isEmpty() && optionalLoad.isEmpty() && shuntCompensator == null) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because Network model contains nor a generator, nor a load, neither a shunt compensator, with id of injection set point action: " + injectionSetPointActionId);
        } else if (optionalGenerator.isPresent()) {
            initialSetPoint = (float) optionalGenerator.get().getTargetP();
        } else if (optionalLoad.isPresent()) {
            initialSetPoint = (float) optionalLoad.get().getP0();
        } else {
            initialSetPoint = (float) shuntCompensator.getSectionCount();
        }
        return initialSetPoint;
    }

    private float getSetPointValue(PropertyBag staticPropertyRangePropertyBag, String remedialActionId, boolean mustValueBePositiveInteger, float initialSetPoint) {
        String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);
        String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
        if ((CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind) && !CsaProfileConstants.RelativeDirectionKind.NONE.toString().equals(direction))
            || (!CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind) && CsaProfileConstants.RelativeDirectionKind.NONE.toString().equals(direction))
            || CsaProfileConstants.RelativeDirectionKind.UP_AND_DOWN.toString().equals(direction)) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has wrong values of valueKind and direction");
        }

        float normalValue = 0f;
        float setPointValue = 0f;
        try {
            normalValue = Float.parseFloat(staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE));
        } catch (Exception e) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a non float-castable normalValue so no set-point value was retrieved");
        }

        if (CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind)) {
            setPointValue = normalValue;
        } else {
            if (normalValue < 0) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a negative normalValue so no set-point value was retrieved");
            }
            if (CsaProfileConstants.ValueOffsetKind.INCREMENTAL.toString().equals(valueKind)) {
                setPointValue = CsaProfileConstants.RelativeDirectionKind.UP.toString().equals(direction) ?
                    initialSetPoint + normalValue :
                    initialSetPoint - normalValue;
            } else {
                setPointValue = CsaProfileConstants.RelativeDirectionKind.UP.toString().equals(direction) ?
                    initialSetPoint + (normalValue * initialSetPoint) / 100 :
                    initialSetPoint - (normalValue * initialSetPoint) / 100;
            }
        }

        if (mustValueBePositiveInteger) {
            if (setPointValue < 0) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a negative normalValue so no set-point value was retrieved");
            }
            if (setPointValue != (int) setPointValue) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a non integer-castable normalValue so no set-point value was retrieved");
            }
        }
        return setPointValue;
    }

    private Optional<Load> findLoad(String injectionSetPointId) {
        return network.getLoadStream().filter(load -> load.getId().equals(injectionSetPointId)).findAny();
    }

    private void addTopologicalElementaryAction(NetworkActionAdder networkActionAdder, PropertyBag
        topologyActionPropertyBag, String remedialActionId) {
        String switchId = topologyActionPropertyBag.getId(CsaProfileConstants.SWITCH);
        if (network.getSwitch(switchId) == null) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because network model does not contain a switch with id: " + switchId);
        }
        CsaProfileCracUtils.checkPropertyReference(topologyActionPropertyBag, remedialActionId, "TopologyAction", CsaProfileConstants.PropertyReference.SWITCH.toString());
        networkActionAdder.newTopologicalAction()
            .withNetworkElement(switchId)
            // todo this is a temporary behaviour closing switch will be implemented in a later version
            .withActionType(ActionType.OPEN).add();
    }
}
