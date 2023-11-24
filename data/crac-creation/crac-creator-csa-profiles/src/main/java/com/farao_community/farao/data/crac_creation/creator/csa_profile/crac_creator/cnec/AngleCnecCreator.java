package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnecAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AngleCnecCreator extends CnecCreator {

    public AngleCnecCreator(Crac crac, Network network, String assessedElementId, String nativeAssessedElementName, String assessedElementOperator, boolean inBaseCase, PropertyBag angleLimitPropertyBag, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext) {
        super(crac, network, assessedElementId, nativeAssessedElementName, assessedElementOperator, inBaseCase, angleLimitPropertyBag, linkedContingencies, csaProfileCnecCreationContexts, cracCreationContext);
    }

    public void addAngleCnecs() {
        if (inBaseCase) {
            addAngleCnec(Instant.PREVENTIVE, null);
        }
        for (Contingency contingency : linkedContingencies) {
            addAngleCnec(Instant.CURATIVE, contingency);
        }
    }

    private void addAngleCnec(Instant instant, Contingency contingency) {
        AngleCnecAdder angleCnecAdder = initAngleCnec();
        if (addAngleLimit(angleCnecAdder)) {
            addCnecData(angleCnecAdder, contingency, instant);
            angleCnecAdder.add();
        }
    }

    private AngleCnecAdder initAngleCnec() {
        return crac.newAngleCnec()
                .withMonitored(true)
                .withOptimized(false)
                .withReliabilityMargin(0);
    }

    private boolean addAngleLimit(AngleCnecAdder angleCnecAdder) {
        boolean isErProfileDataCheckOk = this.erProfileDataCheck(assessedElementId, operationalLimitPropertyBag);

        if (!isErProfileDataCheckOk) {
            return false;
        }

        String isFlowToRefTerminalStr = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_IS_FLOW_TO_REF_TERMINAL);
        boolean isFlowToRefTerminalIsNull = isFlowToRefTerminalStr == null;
        boolean isFlowToRefTerminal = isFlowToRefTerminalIsNull || Boolean.parseBoolean(isFlowToRefTerminalStr);

        String terminal1Id = operationalLimitPropertyBag.getId("terminal1");
        String terminal2Id = operationalLimitPropertyBag.getId("terminal2");

        String networkElement1Id = checkAngleNetworkElementAndGetId(assessedElementId, terminal1Id);
        if (networkElement1Id == null) {
            return false;
        }
        String networkElement2Id = checkAngleNetworkElementAndGetId(assessedElementId, terminal2Id);
        if (networkElement2Id == null) {
            return false;
        }

        boolean areNetworkElementsOk = this.addAngleCnecElements(assessedElementId, angleCnecAdder, networkElement1Id, networkElement2Id, isFlowToRefTerminal);
        if (!areNetworkElementsOk) {
            return false;
        }

        return this.addAngleLimitThreshold(assessedElementId, angleCnecAdder, operationalLimitPropertyBag, isFlowToRefTerminalIsNull);
    }

    private String checkAngleNetworkElementAndGetId(String assessedElementId, String terminalId) {
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(terminalId);
        if (networkElement == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "angle limit equipment is missing in network : " + terminalId));
            return null;
        }
        if (!networkElement.getType().equals(IdentifiableType.BUS)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "network element " + networkElement.getId() + " is not a bus bar section"));
            return null;
        }
        return networkElement.getId();
    }

    private boolean addAngleLimitThreshold(String assessedElementId, AngleCnecAdder angleCnecAdder, PropertyBag angleLimit, boolean isFlowToRefTerminalIsNull) {
        String normalValueStr = angleLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_NORMAL_VALUE);
        Double normalValue = Double.valueOf(normalValueStr);
        if (normalValue < 0) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "angle limit's normal value is negative"));
            return false;
        }
        String direction = angleLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION);
        if (CsaProfileConstants.OperationalLimitDirectionKind.HIGH.toString().equals(direction)) {
            if (handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(assessedElementId, isFlowToRefTerminalIsNull, CsaProfileConstants.OperationalLimitDirectionKind.HIGH)) {
                return false;
            }
            angleCnecAdder.newThreshold()
                    .withUnit(Unit.DEGREE)
                    .withMax(normalValue).add();
        } else if (CsaProfileConstants.OperationalLimitDirectionKind.LOW.toString().equals(direction)) {
            if (handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(assessedElementId, isFlowToRefTerminalIsNull, CsaProfileConstants.OperationalLimitDirectionKind.LOW)) {
                return false;
            }
            angleCnecAdder.newThreshold()
                    .withUnit(Unit.DEGREE)
                    .withMin(-normalValue).add();
        } else if (CsaProfileConstants.OperationalLimitDirectionKind.ABSOLUTE.toString().equals(direction)) {
            angleCnecAdder.newThreshold()
                    .withUnit(Unit.DEGREE)
                    .withMin(-normalValue)
                    .withMax(normalValue).add();
        }
        return true;
    }

    private boolean handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(String assessedElementId, boolean isFlowToRefTerminalIsNull, CsaProfileConstants.OperationalLimitDirectionKind direction) {
        if (isFlowToRefTerminalIsNull) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "ambiguous angle limit direction definition from an undefined VoltageAngleLimit.isFlowToRefTerminal and an OperationalLimit.OperationalLimitType : " + direction));
            return true;
        }
        return false;
    }

    private boolean addAngleCnecElements(String assessedElementId, AngleCnecAdder angleCnecAdder, String networkElement1Id, String networkElement2Id, boolean isFlowToRefTerminal) {
        if (Objects.equals(networkElement1Id, networkElement2Id)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "AngleCNEC's importing and exporting equipments are the same : " + networkElement1Id));
            return false;
        }
        String importingElement = isFlowToRefTerminal ? networkElement1Id : networkElement2Id;
        String exportingElement = isFlowToRefTerminal ? networkElement2Id : networkElement1Id;
        angleCnecAdder.withImportingNetworkElement(importingElement).withExportingNetworkElement(exportingElement);
        return true;
    }

    private boolean erProfileDataCheck(String assessedElementId, PropertyBag angleLimitsPropertyBag) {
        CsaProfileConstants.HeaderValidity headerValidity = CsaProfileCracUtils.checkProfileHeader(angleLimitsPropertyBag, CsaProfileConstants.CsaProfile.EQUIPMENT_RELIABILITY, cracCreationContext.getTimeStamp());
        if (headerValidity == CsaProfileConstants.HeaderValidity.INVALID_KEYWORD) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "Model.keyword must be " + CsaProfileConstants.CsaProfile.EQUIPMENT_RELIABILITY));
            return false;
        } else if (headerValidity == CsaProfileConstants.HeaderValidity.INVALID_INTERVAL) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "Required timestamp does not fall between Model.startDate and Model.endDate"));
            return false;
        } else {
            return true;
        }
    }
}
