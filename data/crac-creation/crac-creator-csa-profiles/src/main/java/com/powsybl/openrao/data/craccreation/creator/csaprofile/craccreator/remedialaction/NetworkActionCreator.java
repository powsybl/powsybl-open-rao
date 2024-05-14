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
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.constants.PropertyReference;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.constants.RelativeDirectionKind;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.constants.ValueOffsetKind;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RotatingMachineAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ShuntCompensatorModification;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.StaticPropertyRange;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.TopologyAction;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;

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

    public NetworkActionAdder getNetworkActionAdder(Map<String, Set<TopologyAction>> linkedTopologyActions, Map<String, Set<RotatingMachineAction>> linkedRotatingMachineActions, Map<String, Set<ShuntCompensatorModification>> linkedShuntCompensatorModifications, Map<String, Set<StaticPropertyRange>> staticPropertyRanges, String remedialActionId, String elementaryActionsAggregatorId, List<String> alterations) {
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

    private boolean processLinkedShuntCompensatorModifications(Map<String, Set<ShuntCompensatorModification>> linkedShuntCompensatorModifications, Map<String, Set<StaticPropertyRange>> staticPropertyRanges, String remedialActionId, String networkElementsAggregatorId, NetworkActionAdder networkActionAdder, List<String> alterations) {
        boolean hasShuntCompensatorModification = false;
        for (ShuntCompensatorModification nativeShuntCompensatorModification : linkedShuntCompensatorModifications.get(networkElementsAggregatorId)) {
            checkNetworkActionHasExactlyOneStaticPropertyRange(staticPropertyRanges, remedialActionId, nativeShuntCompensatorModification.mrid());
            hasShuntCompensatorModification = addInjectionSetPointFromShuntCompensatorModification(
                staticPropertyRanges.get(nativeShuntCompensatorModification.mrid()),
                remedialActionId, networkActionAdder, nativeShuntCompensatorModification, alterations)
                || hasShuntCompensatorModification;
        }
        return hasShuntCompensatorModification;
    }

    private boolean processLinkedRotatingMachineActions(Map<String, Set<RotatingMachineAction>> linkedRotatingMachineActions, Map<String, Set<StaticPropertyRange>> staticPropertyRanges, String remedialActionId, String networkElementsAggregatorId, NetworkActionAdder networkActionAdder, List<String> alterations) {
        boolean hasRotatingMachineAction = false;
        for (RotatingMachineAction nativeRotatingMachineAction : linkedRotatingMachineActions.get(networkElementsAggregatorId)) {
            checkNetworkActionHasExactlyOneStaticPropertyRange(staticPropertyRanges, remedialActionId, nativeRotatingMachineAction.mrid());
            hasRotatingMachineAction = addInjectionSetPointFromRotatingMachineAction(
                staticPropertyRanges.get(nativeRotatingMachineAction.mrid()),
                remedialActionId, networkActionAdder, nativeRotatingMachineAction, alterations)
                || hasRotatingMachineAction;
        }
        return hasRotatingMachineAction;
    }

    private boolean processLinkedTopologyActions(Map<String, Set<TopologyAction>> linkedTopologyActions, Map<String, Set<StaticPropertyRange>> staticPropertyRanges, String remedialActionId, String networkElementsAggregatorId, NetworkActionAdder networkActionAdder, List<String> alterations) {
        boolean hasTopologyActions = false;
        for (TopologyAction nativeTopologyAction : linkedTopologyActions.get(networkElementsAggregatorId)) {
            checkNetworkActionHasExactlyOneStaticPropertyRange(staticPropertyRanges, remedialActionId, nativeTopologyAction.mrid());
            hasTopologyActions = addTopologicalElementaryAction(staticPropertyRanges.get(nativeTopologyAction.mrid()),
                networkActionAdder, nativeTopologyAction, remedialActionId, alterations)
                || hasTopologyActions;
        }
        return hasTopologyActions;
    }

    private static void checkNetworkActionHasExactlyOneStaticPropertyRange(Map<String, Set<StaticPropertyRange>> staticPropertyRanges, String remedialActionId, String elementaryActionId) {
        if (!staticPropertyRanges.containsKey(elementaryActionId)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because there is no StaticPropertyRange linked to elementary action " + elementaryActionId);
        }
        if (staticPropertyRanges.get(elementaryActionId).size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because several conflictual StaticPropertyRanges are linked to elementary action " + elementaryActionId);
        }
    }

    private boolean addInjectionSetPointFromRotatingMachineAction(Set<StaticPropertyRange> staticPropertyRangesLinkedToRotatingMachineAction, String remedialActionId, NetworkActionAdder networkActionAdder, RotatingMachineAction nativeRotatingMachineAction, List<String> alterations) {
        CsaProfileCracUtils.checkPropertyReference(remedialActionId, "RotatingMachineAction", PropertyReference.ROTATING_MACHINE, nativeRotatingMachineAction.propertyReference());
        float initialSetPoint = getInitialSetPointRotatingMachine(nativeRotatingMachineAction.rotatingMachineId(), remedialActionId);

        StaticPropertyRange nativeStaticPropertyRange = staticPropertyRangesLinkedToRotatingMachineAction.iterator().next(); // get a random one because there is only one
        CsaProfileCracUtils.checkPropertyReference(remedialActionId, "StaticPropertyRange", PropertyReference.ROTATING_MACHINE, nativeStaticPropertyRange.propertyReference());
        double setPointValue = getSetPointValue(nativeStaticPropertyRange, remedialActionId, false, initialSetPoint);

        if (nativeRotatingMachineAction.normalEnabled()) {
            networkActionAdder.newInjectionSetPoint()
                .withSetpoint(setPointValue)
                .withNetworkElement(nativeRotatingMachineAction.rotatingMachineId())
                .withUnit(Unit.MEGAWATT)
                .add();
            return true;
        } else {
            alterations.add("Elementary rotating machine action on rotating machine %s for remedial action %s ignored because the RotatingMachineAction is disabled".formatted(nativeRotatingMachineAction.rotatingMachineId(), remedialActionId));
            return false;
        }
    }

    private boolean addInjectionSetPointFromShuntCompensatorModification(Set<StaticPropertyRange> staticPropertyRangesLinkedToShuntCompensatorModification, String remedialActionId, NetworkActionAdder networkActionAdder, ShuntCompensatorModification nativeShuntCompensatorModification, List<String> alterations) {
        CsaProfileCracUtils.checkPropertyReference(remedialActionId, "ShuntCompensatorModification", PropertyReference.SHUNT_COMPENSATOR, nativeShuntCompensatorModification.propertyReference());
        float initialSetPoint = getInitialSetPointShuntCompensator(nativeShuntCompensatorModification.shuntCompensatorId(), remedialActionId);

        StaticPropertyRange nativeStaticPropertyRange = staticPropertyRangesLinkedToShuntCompensatorModification.iterator().next(); // get a random one because there is only one
        CsaProfileCracUtils.checkPropertyReference(remedialActionId, "StaticPropertyRange", PropertyReference.SHUNT_COMPENSATOR, nativeStaticPropertyRange.propertyReference());
        double setPointValue = getSetPointValue(nativeStaticPropertyRange, remedialActionId, true, initialSetPoint);

        if (nativeShuntCompensatorModification.normalEnabled()) {
            networkActionAdder.newInjectionSetPoint()
                .withSetpoint(setPointValue)
                .withNetworkElement(nativeShuntCompensatorModification.shuntCompensatorId())
                .withUnit(Unit.SECTION_COUNT)
                .add();
            return true;
        } else {
            alterations.add("Elementary shunt compensator modification on shunt compensator %s for remedial action %s ignored because the ShuntCompensatorModification is disabled".formatted(nativeShuntCompensatorModification.shuntCompensatorId(), remedialActionId));
            return false;
        }
    }

    private float getInitialSetPointRotatingMachine(String injectionSetPointActionId, String remedialActionId) {
        float initialSetPoint;
        Optional<Generator> optionalGenerator = network.getGeneratorStream().filter(gen -> gen.getId().equals(injectionSetPointActionId)).findAny();
        Optional<Load> optionalLoad = findLoad(injectionSetPointActionId);
        if (optionalGenerator.isEmpty() && optionalLoad.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action " + remedialActionId + " will not be imported because the network does not contain a generator, neither a load with id: " + injectionSetPointActionId);
        } else {
            initialSetPoint = optionalGenerator.map(generator -> (float) generator.getTargetP()).orElseGet(() -> (float) optionalLoad.get().getP0());
        }
        return initialSetPoint;
    }

    private float getInitialSetPointShuntCompensator(String injectionSetPointActionId, String remedialActionId) {
        float initialSetPoint;
        ShuntCompensator shuntCompensator = network.getShuntCompensator(injectionSetPointActionId);
        if (shuntCompensator == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action " + remedialActionId + " will not be imported because the network does not contain a shunt compensator with id: " + injectionSetPointActionId);
        } else {
            initialSetPoint = shuntCompensator.getSectionCount();
        }
        return initialSetPoint;
    }

    private double getSetPointValue(StaticPropertyRange nativeStaticPropertyRange, String remedialActionId, boolean mustValueBePositiveInteger, float initialSetPoint) {
        checkCompatibility(remedialActionId, nativeStaticPropertyRange);

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
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because of an incoherent negative number of sections");
            }
            if (setPointValue != (int) setPointValue) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because of a non integer-castable number of sections");
            }
        }
        return setPointValue;
    }

    // TODO: rename this method to make it more explicit what it does
    private static void checkCompatibility(String remedialActionId, StaticPropertyRange nativeStaticPropertyRange) {
        if (ValueOffsetKind.ABSOLUTE.toString().equals(nativeStaticPropertyRange.valueKind()) && !RelativeDirectionKind.NONE.toString().equals(nativeStaticPropertyRange.direction())
            || !ValueOffsetKind.ABSOLUTE.toString().equals(nativeStaticPropertyRange.valueKind()) && RelativeDirectionKind.NONE.toString().equals(nativeStaticPropertyRange.direction())
            || RelativeDirectionKind.UP_AND_DOWN.toString().equals(nativeStaticPropertyRange.direction())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because its StaticPropertyRange uses an illegal combination of ValueOffsetKind and RelativeDirectionKind");
        }
    }

    private Optional<Load> findLoad(String injectionSetPointId) {
        return network.getLoadStream().filter(load -> load.getId().equals(injectionSetPointId)).findAny();
    }

    private boolean addTopologicalElementaryAction(Set<StaticPropertyRange> staticPropertyRangesLinkedToTopologicalElementaryAction, NetworkActionAdder networkActionAdder, TopologyAction nativeTopologyAction, String remedialActionId, List<String> alterations) {
        if (network.getSwitch(nativeTopologyAction.switchId()) == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action " + remedialActionId + " will not be imported because the network does not contain a switch with id: " + nativeTopologyAction.switchId());
        }
        CsaProfileCracUtils.checkPropertyReference(remedialActionId, "TopologyAction", PropertyReference.SWITCH, nativeTopologyAction.propertyReference());

        StaticPropertyRange nativeStaticPropertyRange = staticPropertyRangesLinkedToTopologicalElementaryAction.iterator().next();
        CsaProfileCracUtils.checkPropertyReference(remedialActionId, "StaticPropertyRange", PropertyReference.SWITCH, nativeStaticPropertyRange.propertyReference());

        if (0d != nativeStaticPropertyRange.normalValue() && 1d != nativeStaticPropertyRange.normalValue()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because the normalValue is " + nativeStaticPropertyRange.normalValue() + " which does not define a proper action type (open 1 / close 0)");
        }

        if (!ValueOffsetKind.ABSOLUTE.toString().equals(nativeStaticPropertyRange.valueKind())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because the ValueOffsetKind is " + nativeStaticPropertyRange.valueKind() + " but should be none");
        }

        if (!RelativeDirectionKind.NONE.toString().equals(nativeStaticPropertyRange.direction())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because the RelativeDirectionKind is " + nativeStaticPropertyRange.direction() + " but should be absolute");
        }

        if (nativeTopologyAction.normalEnabled()) {
            networkActionAdder.newTopologicalAction()
                .withNetworkElement(nativeTopologyAction.switchId())
                .withActionType(nativeStaticPropertyRange.normalValue() == 0d ? ActionType.CLOSE : ActionType.OPEN).add();
            return true;
        } else {
            alterations.add("Elementary topology action on switch %s for remedial action %s ignored because the TopologyAction is disabled".formatted(nativeTopologyAction.switchId(), remedialActionId));
            return false;
        }
    }
}
