/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleCnecCreator extends AbstractCnecCreator {

    public AngleCnecCreator(Crac crac, Network network, AssessedElement nativeAssessedElement, PropertyBag angleLimitPropertyBag, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, String rejectedLinksAssessedElementContingency, boolean aeSecuredForRegion, boolean aeScannedForRegion) {
        super(crac, network, nativeAssessedElement, angleLimitPropertyBag, linkedContingencies, csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion);
    }

    public void addAngleCnecs() {
        if (nativeAssessedElement.inBaseCase()) {
            addAngleCnec(crac.getPreventiveInstant().getId(), null);
        }
        for (Contingency contingency : linkedContingencies) {
            addAngleCnec(crac.getInstant(InstantKind.CURATIVE).getId(), contingency);
        }
    }

    private void addAngleCnec(String instantId, Contingency contingency) {
        AngleCnecAdder angleCnecAdder = initAngleCnec();
        addAngleLimit(angleCnecAdder);
        addCnecBaseInformation(angleCnecAdder, contingency, instantId);
        angleCnecAdder.add();
        markCnecAsImportedAndHandleRejectedContingencies(getCnecName(instantId, contingency));
    }

    private AngleCnecAdder initAngleCnec() {
        return crac.newAngleCnec().withReliabilityMargin(0);
    }

    private void addAngleLimit(AngleCnecAdder angleCnecAdder) {
        String isFlowToRefTerminalStr = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_IS_FLOW_TO_REF_TERMINAL);
        boolean isFlowToRefTerminalIsNull = isFlowToRefTerminalStr == null;
        boolean isFlowToRefTerminal = isFlowToRefTerminalIsNull || Boolean.parseBoolean(isFlowToRefTerminalStr);

        String terminal1Id = operationalLimitPropertyBag.getId("terminal1");
        String terminal2Id = operationalLimitPropertyBag.getId("terminal2");

        String networkElement1Id = checkAngleNetworkElementAndGetId(terminal1Id);
        String networkElement2Id = checkAngleNetworkElementAndGetId(terminal2Id);

        addAngleCnecElements(angleCnecAdder, networkElement1Id, networkElement2Id, isFlowToRefTerminal);
        addAngleLimitThreshold(angleCnecAdder, operationalLimitPropertyBag, isFlowToRefTerminalIsNull);
    }

    private String checkAngleNetworkElementAndGetId(String terminalId) {
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(terminalId);
        if (networkElement == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, writeAssessedElementIgnoredReasonMessage("the angle limit equipment " + terminalId + " is missing in network"));
        }
        if (!networkElement.getType().equals(IdentifiableType.BUS)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the network element " + networkElement.getId() + " is not a bus bar section"));
        }
        return networkElement.getId();
    }

    private void addAngleLimitThreshold(AngleCnecAdder angleCnecAdder, PropertyBag angleLimit, boolean isFlowToRefTerminalIsNull) {
        String normalValueStr = angleLimit.get(CsaProfileConstants.REQUEST_VOLTAGE_ANGLE_LIMIT_NORMAL_VALUE);
        Double normalValue = Double.valueOf(normalValueStr);
        if (normalValue < 0) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the angle limit's normal value is negative"));
        }
        String direction = angleLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION);
        if (CsaProfileConstants.OperationalLimitDirectionKind.HIGH.toString().equals(direction)) {
            handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(isFlowToRefTerminalIsNull, CsaProfileConstants.OperationalLimitDirectionKind.HIGH);
            angleCnecAdder.newThreshold()
                .withUnit(Unit.DEGREE)
                .withMax(normalValue).add();
        } else if (CsaProfileConstants.OperationalLimitDirectionKind.LOW.toString().equals(direction)) {
            handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(isFlowToRefTerminalIsNull, CsaProfileConstants.OperationalLimitDirectionKind.LOW);
            angleCnecAdder.newThreshold()
                .withUnit(Unit.DEGREE)
                .withMin(-normalValue).add();
        } else if (CsaProfileConstants.OperationalLimitDirectionKind.ABSOLUTE.toString().equals(direction)) {
            angleCnecAdder.newThreshold()
                .withUnit(Unit.DEGREE)
                .withMin(-normalValue)
                .withMax(normalValue).add();
        }
    }

    private void handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(boolean isFlowToRefTerminalIsNull, CsaProfileConstants.OperationalLimitDirectionKind direction) {
        if (isFlowToRefTerminalIsNull) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("of an ambiguous angle limit direction definition from an undefined VoltageAngleLimit.isFlowToRefTerminal and an OperationalLimit.OperationalLimitType: " + direction));
        }
    }

    private void addAngleCnecElements(AngleCnecAdder angleCnecAdder, String networkElement1Id, String networkElement2Id, boolean isFlowToRefTerminal) {
        if (networkElement1Id.equals(networkElement2Id)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("AngleCNEC's importing and exporting equipments are the same: " + networkElement1Id));
        }
        String importingElement = isFlowToRefTerminal ? networkElement1Id : networkElement2Id;
        String exportingElement = isFlowToRefTerminal ? networkElement2Id : networkElement1Id;
        angleCnecAdder.withImportingNetworkElement(importingElement).withExportingNetworkElement(exportingElement);
    }
}
