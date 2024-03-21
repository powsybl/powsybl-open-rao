package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.List;
import java.util.Set;

public class VoltageCnecCreator extends AbstractCnecCreator {

    public VoltageCnecCreator(Crac crac, Network network, String assessedElementId, String nativeAssessedElementName, String assessedElementOperator, boolean inBaseCase, PropertyBag voltageLimitPropertyBag, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, String rejectedLinksAssessedElementContingency, String regionEic, String aeSecuredForRegion, String aeScannedForRegion) {
        super(crac, network, assessedElementId, nativeAssessedElementName, assessedElementOperator, inBaseCase, voltageLimitPropertyBag, linkedContingencies, csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, regionEic, aeSecuredForRegion, aeScannedForRegion);
    }

    public void addVoltageCnecs() {
        if (inBaseCase) {
            addVoltageCnec(crac.getPreventiveInstant().getId(), null);
        }
        for (Contingency contingency : linkedContingencies) {
            addVoltageCnec(crac.getInstant(InstantKind.CURATIVE).getId(), contingency);
        }
    }

    private void addVoltageCnec(String instantId, Contingency contingency) {
        VoltageCnecAdder voltageCnecAdder = initVoltageCnec();
        if (addVoltageLimit(voltageCnecAdder)) {
            addCnecBaseInformation(voltageCnecAdder, contingency, instantId);
            voltageCnecAdder.add();
            markCnecAsImportedAndHandleRejectedContingencies(instantId, contingency);
        }
    }

    private VoltageCnecAdder initVoltageCnec() {
        return crac.newVoltageCnec()
            .withMonitored(true)
            .withOptimized(false)
            .withReliabilityMargin(0);
    }

    private boolean addVoltageLimit(VoltageCnecAdder voltageCnecAdder) {
        String equipmentId = operationalLimitPropertyBag.getId(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_EQUIPMENT);
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(equipmentId);
        if (networkElement == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, writeAssessedElementIgnoredReasonMessage("the voltage limit equipment " + equipmentId + " is missing in network")));
            return false;
        }

        if (!networkElement.getType().equals(IdentifiableType.BUS)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the network element " + networkElement.getId() + " is not a bus bar section")));
            return false;
        }

        String networkElementId = networkElement.getId();
        voltageCnecAdder.withNetworkElement(networkElementId);

        if (!checkDuration()) {
            return false;
        }

        return this.addVoltageLimitThreshold(assessedElementId, voltageCnecAdder, operationalLimitPropertyBag);
    }

    private boolean addVoltageLimitThreshold(String assessedElementId, VoltageCnecAdder voltageCnecAdder, PropertyBag voltageLimit) {
        String valueStr = voltageLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_VALUE);
        Double value = Double.valueOf(valueStr);
        String limitType = voltageLimit.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_TYPE);
        if (CsaProfileConstants.LimitTypeKind.HIGH_VOLTAGE.toString().equals(limitType)) {
            voltageCnecAdder.newThreshold()
                .withUnit(Unit.KILOVOLT)
                .withMax(value).add();
        } else if (CsaProfileConstants.LimitTypeKind.LOW_VOLTAGE.toString().equals(limitType)) {
            voltageCnecAdder.newThreshold()
                .withUnit(Unit.KILOVOLT)
                .withMin(value).add();
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("a voltage limit can only be of kind highVoltage or lowVoltage")));
            return false;
        }
        return true;
    }

    private boolean checkDuration() {
        String isInfiniteDurationStr = operationalLimitPropertyBag.get(CsaProfileConstants.REQUEST_VOLTAGE_LIMIT_IS_INFINITE_DURATION);
        boolean isInfiniteDuration = Boolean.parseBoolean(isInfiniteDurationStr);
        if (!isInfiniteDuration) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, writeAssessedElementIgnoredReasonMessage("only permanent voltage limits (with infinite duration) are currently handled")));
            return false;
        }
        return true;
    }
}
