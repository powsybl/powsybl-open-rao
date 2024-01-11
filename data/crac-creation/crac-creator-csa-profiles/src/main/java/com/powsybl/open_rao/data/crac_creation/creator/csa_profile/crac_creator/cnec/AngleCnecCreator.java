package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Contingency;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.InstantKind;
import com.powsybl.open_rao.data.crac_api.cnec.AngleCnecAdder;
import com.powsybl.open_rao.data.crac_creation.creator.api.ImportStatus;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AngleCnecCreator extends AbstractCnecCreator {

    public AngleCnecCreator(Crac crac, Network network, String assessedElementId, String nativeAssessedElementName, String assessedElementOperator, boolean inBaseCase, PropertyBag angleLimitPropertyBag, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, String rejectedLinksAssessedElementContingency, boolean useGeographicalFilter) {
        super(crac, network, assessedElementId, nativeAssessedElementName, assessedElementOperator, inBaseCase, angleLimitPropertyBag, linkedContingencies, csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, useGeographicalFilter);
    }

    public void addAngleCnecs() {
        if (inBaseCase) {
            addAngleCnec(crac.getPreventiveInstant().getId(), null);
        }
        for (Contingency contingency : linkedContingencies) {
            addAngleCnec(crac.getInstant(InstantKind.CURATIVE).getId(), contingency);
        }
    }

    private void addAngleCnec(String instantId, Contingency contingency) {
        AngleCnecAdder angleCnecAdder = initAngleCnec();
        if (addAngleLimit(angleCnecAdder, contingency)) {
            addCnecBaseInformation(angleCnecAdder, contingency, instantId);
            angleCnecAdder.add();
            String cnecName = getCnecName(instantId, contingency);
            markCnecAsImportedAndHandleRejectedContingencies(cnecName);
        }
    }

    private AngleCnecAdder initAngleCnec() {
        return crac.newAngleCnec()
                .withMonitored(true)
                .withOptimized(false)
                .withReliabilityMargin(0);
    }

    private boolean addAngleLimit(AngleCnecAdder angleCnecAdder, Contingency contingency) {
        String isFlowToRefTerminalStr = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_IS_FLOW_TO_REF_TERMINAL);
        boolean isFlowToRefTerminalIsNull = isFlowToRefTerminalStr == null;
        boolean isFlowToRefTerminal = isFlowToRefTerminalIsNull || Boolean.parseBoolean(isFlowToRefTerminalStr);

        String terminal1Id = operationalLimitPropertyBag.getId("terminal1");
        String terminal2Id = operationalLimitPropertyBag.getId("terminal2");

        String networkElement1Id = checkAngleNetworkElementAndGetId(terminal1Id);
        if (networkElement1Id == null) {
            return false;
        }
        String networkElement2Id = checkAngleNetworkElementAndGetId(terminal2Id);
        if (networkElement2Id == null) {
            return false;
        }

        boolean areNetworkElementsOk = this.addAngleCnecElements(angleCnecAdder, networkElement1Id, networkElement2Id, isFlowToRefTerminal);
        if (!areNetworkElementsOk) {
            return false;
        }

        if (incompatibleLocationsBetweenCnecAndContingency(Set.of(terminal1Id, terminal2Id), contingency)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("AssessedElement and Contingency " + contingency.getId()) + " do not belong to a common country. AngleCNEC will not be imported."));
            return false;
        }

        return this.addAngleLimitThreshold(angleCnecAdder, operationalLimitPropertyBag, isFlowToRefTerminalIsNull);
    }

    private String checkAngleNetworkElementAndGetId(String terminalId) {
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(terminalId);
        if (networkElement == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, writeAssessedElementIgnoredReasonMessage("angle limit equipment is missing in network : " + terminalId)));
            return null;
        }
        if (!networkElement.getType().equals(IdentifiableType.BUS)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the network element " + networkElement.getId() + " is not a bus bar section")));
            return null;
        }
        return networkElement.getId();
    }

    private boolean addAngleLimitThreshold(AngleCnecAdder angleCnecAdder, PropertyBag angleLimit, boolean isFlowToRefTerminalIsNull) {
        String normalValueStr = angleLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_NORMAL_VALUE);
        Double normalValue = Double.valueOf(normalValueStr);
        if (normalValue < 0) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the angle limit's normal value is negative")));
            return false;
        }
        String direction = angleLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION);
        if (CsaProfileConstants.OperationalLimitDirectionKind.HIGH.toString().equals(direction)) {
            if (handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(isFlowToRefTerminalIsNull, CsaProfileConstants.OperationalLimitDirectionKind.HIGH)) {
                return false;
            }
            angleCnecAdder.newThreshold()
                    .withUnit(Unit.DEGREE)
                    .withMax(normalValue).add();
        } else if (CsaProfileConstants.OperationalLimitDirectionKind.LOW.toString().equals(direction)) {
            if (handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(isFlowToRefTerminalIsNull, CsaProfileConstants.OperationalLimitDirectionKind.LOW)) {
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

    private boolean handleMissingIsFlowToRefTerminalForNotAbsoluteDirection(boolean isFlowToRefTerminalIsNull, CsaProfileConstants.OperationalLimitDirectionKind direction) {
        if (isFlowToRefTerminalIsNull) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("of an ambiguous angle limit direction definition from an undefined VoltageAngleLimit.isFlowToRefTerminal and an OperationalLimit.OperationalLimitType : " + direction)));
            return true;
        }
        return false;
    }

    private boolean addAngleCnecElements(AngleCnecAdder angleCnecAdder, String networkElement1Id, String networkElement2Id, boolean isFlowToRefTerminal) {
        if (Objects.equals(networkElement1Id, networkElement2Id)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("AngleCNEC's importing and exporting equipments are the same : " + networkElement1Id)));
            return false;
        }
        String importingElement = isFlowToRefTerminal ? networkElement1Id : networkElement2Id;
        String exportingElement = isFlowToRefTerminal ? networkElement2Id : networkElement1Id;
        angleCnecAdder.withImportingNetworkElement(importingElement).withExportingNetworkElement(exportingElement);
        return true;
    }
}
