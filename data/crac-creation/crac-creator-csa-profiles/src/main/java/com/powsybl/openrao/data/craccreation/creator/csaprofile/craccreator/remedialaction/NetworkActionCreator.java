/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.List;
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

    public NetworkActionAdder getNetworkActionAdder(Map<String, Set<PropertyBag>> linkedTopologyActions, Map<String, Set<PropertyBag>> linkedRotatingMachineActions, Map<String, Set<PropertyBag>> linkedShuntCompensatorModifications, Map<String, Set<PropertyBag>> staticPropertyRanges, String remedialActionId, String elementaryActionsAggregatorId, List<String> alterations) {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(remedialActionId);
        boolean hasElementaryActions = false;

        if (linkedTopologyActions.containsKey(elementaryActionsAggregatorId)) {
            hasElementaryActions = processLinkedTopologyActions(linkedTopologyActions, staticPropertyRanges, remedialActionId, elementaryActionsAggregatorId, networkActionAdder, alterations);
        }

        if (linkedRotatingMachineActions.containsKey(elementaryActionsAggregatorId)) {
            hasElementaryActions = processLinkedRotatingMachineActions(linkedRotatingMachineActions, staticPropertyRanges, remedialActionId, elementaryActionsAggregatorId, networkActionAdder, alterations) || hasElementaryActions;
        }

        if (linkedShuntCompensatorModifications.containsKey(elementaryActionsAggregatorId)) {
            hasElementaryActions = processLinkedShuntCompensatorModifications(linkedShuntCompensatorModifications, staticPropertyRanges, remedialActionId, elementaryActionsAggregatorId, networkActionAdder, alterations) || hasElementaryActions;
        }

        if (!hasElementaryActions) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "Remedial action " + remedialActionId + " will not be imported because it has no elementary action");
        }
        return networkActionAdder;
    }

    private boolean processLinkedShuntCompensatorModifications(Map<String, Set<PropertyBag>> linkedShuntCompensatorModifications, Map<String, Set<PropertyBag>> staticPropertyRanges, String remedialActionId, String networkElementsAggregatorId, NetworkActionAdder networkActionAdder, List<String> alterations) {
        boolean hasShuntCompensatorModification = false;
        for (PropertyBag shuntCompensatorModificationPropertyBag : linkedShuntCompensatorModifications.get(networkElementsAggregatorId)) {
            checkNetworkActionHasExactlyOneStaticPropertyRange(staticPropertyRanges, remedialActionId, shuntCompensatorModificationPropertyBag);
            hasShuntCompensatorModification = addInjectionSetPointFromShuntCompensatorModification(
                staticPropertyRanges.get(shuntCompensatorModificationPropertyBag.getId(CsaProfileConstants.MRID)),
                remedialActionId, networkActionAdder, shuntCompensatorModificationPropertyBag, alterations)
                || hasShuntCompensatorModification;
        }
        return hasShuntCompensatorModification;
    }

    private boolean processLinkedRotatingMachineActions(Map<String, Set<PropertyBag>> linkedRotatingMachineActions, Map<String, Set<PropertyBag>> staticPropertyRanges, String remedialActionId, String networkElementsAggregatorId, NetworkActionAdder networkActionAdder, List<String> alterations) {
        boolean hasRotatingMachineAction = false;
        for (PropertyBag rotatingMachineActionPropertyBag : linkedRotatingMachineActions.get(networkElementsAggregatorId)) {
            checkNetworkActionHasExactlyOneStaticPropertyRange(staticPropertyRanges, remedialActionId, rotatingMachineActionPropertyBag);
            hasRotatingMachineAction = addInjectionSetPointFromRotatingMachineAction(
                staticPropertyRanges.get(rotatingMachineActionPropertyBag.getId(CsaProfileConstants.MRID)),
                remedialActionId, networkActionAdder, rotatingMachineActionPropertyBag, alterations)
                || hasRotatingMachineAction;
        }
        return hasRotatingMachineAction;
    }

    private boolean processLinkedTopologyActions(Map<String, Set<PropertyBag>> linkedTopologyActions, Map<String, Set<PropertyBag>> staticPropertyRanges, String remedialActionId, String networkElementsAggregatorId, NetworkActionAdder networkActionAdder, List<String> alterations) {
        boolean hasTopologyActions = false;
        for (PropertyBag topologyActionPropertyBag : linkedTopologyActions.get(networkElementsAggregatorId)) {
            checkNetworkActionHasExactlyOneStaticPropertyRange(staticPropertyRanges, remedialActionId, topologyActionPropertyBag);
            hasTopologyActions = addTopologicalElementaryAction(staticPropertyRanges.get(topologyActionPropertyBag.getId(CsaProfileConstants.MRID)),
                networkActionAdder, topologyActionPropertyBag, remedialActionId, alterations)
                || hasTopologyActions;
        }
        return hasTopologyActions;
    }

    private static void checkNetworkActionHasExactlyOneStaticPropertyRange(Map<String, Set<PropertyBag>> staticPropertyRanges, String remedialActionId, PropertyBag networkActionPropertyBag) {
        String elementaryActionId = networkActionPropertyBag.getId(CsaProfileConstants.MRID);
        if (!staticPropertyRanges.containsKey(elementaryActionId)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because there is no StaticPropertyRange linked to elementary action " + elementaryActionId);
        }
        if (staticPropertyRanges.get(elementaryActionId).size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because several conflictual StaticPropertyRanges are linked to that elementary action " + elementaryActionId);
        }
    }

    private boolean addInjectionSetPointFromRotatingMachineAction(Set<PropertyBag> staticPropertyRangesLinkedToRotatingMachineAction, String remedialActionId, NetworkActionAdder networkActionAdder, PropertyBag rotatingMachineActionPropertyBag, List<String> alterations) {
        CsaProfileCracUtils.checkPropertyReference(rotatingMachineActionPropertyBag, remedialActionId, "RotatingMachineAction", CsaProfileConstants.PropertyReference.ROTATING_MACHINE.toString());
        String rawId = rotatingMachineActionPropertyBag.get(CsaProfileConstants.ROTATING_MACHINE);
        String rotatingMachineId = rawId.substring(rawId.lastIndexOf("#_") + 2).replace("+", " ");
        float initialSetPoint = getInitialSetPointRotatingMachine(rotatingMachineId, remedialActionId);

        PropertyBag staticPropertyRangePropertyBag = staticPropertyRangesLinkedToRotatingMachineAction.iterator().next(); // get a random one because there is only one
        CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PropertyReference.ROTATING_MACHINE.toString());
        float setPointValue = getSetPointValue(staticPropertyRangePropertyBag, remedialActionId, false, initialSetPoint);

        Optional<String> normalEnabled = Optional.ofNullable(rotatingMachineActionPropertyBag.get(CsaProfileConstants.NORMAL_ENABLED));
        if (normalEnabled.isEmpty() || Boolean.parseBoolean(normalEnabled.get())) {
            networkActionAdder.newInjectionSetPoint()
                .withSetpoint(setPointValue)
                .withNetworkElement(rotatingMachineId)
                .withUnit(Unit.MEGAWATT)
                .add();
            return true;
        } else {
            alterations.add("Elementary rotating machine action on rotating machine %s for remedial action %s ignored because the RotatingMachineAction is disabled".formatted(rotatingMachineId, remedialActionId));
            return false;
        }
    }

    private boolean addInjectionSetPointFromShuntCompensatorModification(Set<PropertyBag> staticPropertyRangesLinkedToShuntCompensatorModification, String remedialActionId, NetworkActionAdder networkActionAdder, PropertyBag shuntCompensatorModificationPropertyBag, List<String> alterations) {
        CsaProfileCracUtils.checkPropertyReference(shuntCompensatorModificationPropertyBag, remedialActionId, "ShuntCompensatorModification", CsaProfileConstants.PropertyReference.SHUNT_COMPENSATOR.toString());
        String rawId = shuntCompensatorModificationPropertyBag.get(CsaProfileConstants.SHUNT_COMPENSATOR_ID);
        String shuntCompensatorId = rawId.substring(rawId.lastIndexOf("_") + 1);
        float initialSetPoint = getInitialSetPointShuntCompensator(shuntCompensatorId, remedialActionId);

        PropertyBag staticPropertyRangePropertyBag = staticPropertyRangesLinkedToShuntCompensatorModification.iterator().next(); // get a random one because there is only one
        CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PropertyReference.SHUNT_COMPENSATOR.toString());
        float setPointValue = getSetPointValue(staticPropertyRangePropertyBag, remedialActionId, true, initialSetPoint);

        Optional<String> normalEnabled = Optional.ofNullable(shuntCompensatorModificationPropertyBag.get(CsaProfileConstants.NORMAL_ENABLED));
        if (normalEnabled.isEmpty() || Boolean.parseBoolean(normalEnabled.get())) {
            networkActionAdder.newInjectionSetPoint()
                .withSetpoint(setPointValue)
                .withNetworkElement(shuntCompensatorId)
                .withUnit(Unit.SECTION_COUNT)
                .add();
            return true;
        } else {
            alterations.add("Elementary shunt compensator modification on shunt compensator %s for remedial action %s ignored because the ShuntCompensatorModification is disabled".formatted(shuntCompensatorId, remedialActionId));
            return false;
        }
    }

    private float getInitialSetPointRotatingMachine(String injectionSetPointActionId, String remedialActionId) {
        float initialSetPoint;
        Optional<Generator> optionalGenerator = network.getGeneratorStream().filter(gen -> gen.getId().equals(injectionSetPointActionId)).findAny();
        Optional<Load> optionalLoad = findLoad(injectionSetPointActionId);
        if (optionalGenerator.isEmpty() && optionalLoad.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action " + remedialActionId + " will not be imported because Network model does not contain a generator, neither a load with id of RotatingMachine: " + injectionSetPointActionId);
        } else {
            initialSetPoint = optionalGenerator.map(generator -> (float) generator.getTargetP()).orElseGet(() -> (float) optionalLoad.get().getP0());
        }
        return initialSetPoint;
    }

    private float getInitialSetPointShuntCompensator(String injectionSetPointActionId, String remedialActionId) {
        float initialSetPoint;
        ShuntCompensator shuntCompensator = network.getShuntCompensator(injectionSetPointActionId);
        if (shuntCompensator == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action " + remedialActionId + " will not be imported because Network model does not contain a shunt compensator with id of ShuntCompensator: " + injectionSetPointActionId);
        } else {
            initialSetPoint = shuntCompensator.getSectionCount();
        }
        return initialSetPoint;
    }

    private float getSetPointValue(PropertyBag staticPropertyRangePropertyBag, String remedialActionId, boolean mustValueBePositiveInteger, float initialSetPoint) {
        String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);
        String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
        checkCompatibility(remedialActionId, valueKind, direction);

        float normalValue;
        try {
            normalValue = Float.parseFloat(staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE));
        } catch (Exception e) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because StaticPropertyRange has a non float-castable normalValue so no set-point value was retrieved");
        }

        float setPointValue;
        if (CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind)) {
            setPointValue = normalValue;
        } else if (CsaProfileConstants.ValueOffsetKind.INCREMENTAL.toString().equals(valueKind)) {
            setPointValue = CsaProfileConstants.RelativeDirectionKind.UP.toString().equals(direction) ?
                initialSetPoint + normalValue :
                initialSetPoint - normalValue;
        } else {
            setPointValue = CsaProfileConstants.RelativeDirectionKind.UP.toString().equals(direction) ?
                initialSetPoint + (normalValue * initialSetPoint) / 100 :
                initialSetPoint - (normalValue * initialSetPoint) / 100;
        }

        if (mustValueBePositiveInteger) {
            if (setPointValue < 0) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because StaticPropertyRange has a negative normalValue so no set-point value was retrieved");
            }
            if (setPointValue != (int) setPointValue) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because StaticPropertyRange has a non integer-castable normalValue so no set-point value was retrieved");
            }
        }
        return setPointValue;
    }

    private static void checkCompatibility(String remedialActionId, String valueKind, String direction) {
        if (CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind) && !CsaProfileConstants.RelativeDirectionKind.NONE.toString().equals(direction)
            || !CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind) && CsaProfileConstants.RelativeDirectionKind.NONE.toString().equals(direction)
            || CsaProfileConstants.RelativeDirectionKind.UP_AND_DOWN.toString().equals(direction)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because StaticPropertyRange has wrong values of valueKind and direction");
        }
    }

    private Optional<Load> findLoad(String injectionSetPointId) {
        return network.getLoadStream().filter(load -> load.getId().equals(injectionSetPointId)).findAny();
    }

    private boolean addTopologicalElementaryAction(Set<PropertyBag> staticPropertyRangesLinkedToTopologicalElementaryAction, NetworkActionAdder networkActionAdder, PropertyBag topologyActionPropertyBag, String remedialActionId, List<String> alterations) {
        String switchId = topologyActionPropertyBag.getId(CsaProfileConstants.SWITCH);
        if (network.getSwitch(switchId) == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action " + remedialActionId + " will not be imported because network model does not contain a switch with id: " + switchId);
        }
        CsaProfileCracUtils.checkPropertyReference(topologyActionPropertyBag, remedialActionId, "TopologyAction", CsaProfileConstants.PropertyReference.SWITCH.toString());

        PropertyBag staticPropertyRangePropertyBag = staticPropertyRangesLinkedToTopologicalElementaryAction.iterator().next();
        String normalValue = staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE);
        if (!"0".equals(normalValue) && !"1".equals(normalValue)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because the normalValue is " + normalValue + " which does not define a proper action type (open 1 / close 0)");
        }

        String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);
        if (!CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because the ValueOffsetKind is " + valueKind + " but should be none.");
        }

        String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
        if (!CsaProfileConstants.RelativeDirectionKind.NONE.toString().equals(direction)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because the RelativeDirectionKind is " + direction + " but should be absolute.");
        }

        Optional<String> normalEnabled = Optional.ofNullable(topologyActionPropertyBag.get(CsaProfileConstants.NORMAL_ENABLED));
        if (normalEnabled.isEmpty() || Boolean.parseBoolean(normalEnabled.get())) {
            networkActionAdder.newTopologicalAction()
                .withNetworkElement(switchId)
                .withActionType("0".equals(normalValue) ? ActionType.CLOSE : ActionType.OPEN).add();
            return true;
        } else {
            alterations.add("Elementary topology action on switch %s for remedial action %s ignored because the TopologyAction is disabled".formatted(switchId, remedialActionId));
            return false;
        }
    }
}
