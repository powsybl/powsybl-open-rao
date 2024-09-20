/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.cnec;

import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.LimitTypeKind;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.VoltageLimit;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElement;
import com.powsybl.openrao.data.cracio.commons.OpenRaoImportException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class VoltageCnecCreator extends AbstractCnecCreator {

    private final VoltageLimit nativeVoltageLimit;

    public VoltageCnecCreator(Crac crac, Network network, AssessedElement nativeAssessedElement, VoltageLimit nativeVoltageLimit, List<Contingency> linkedContingencies, Set<ElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, String rejectedLinksAssessedElementContingency, boolean aeSecuredForRegion, boolean aeScannedForRegion, Map<String, String> borderPerTso, Map<String, String> borderPerEic) {
        super(crac, network, nativeAssessedElement, linkedContingencies, csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion, borderPerTso, borderPerEic);
        this.nativeVoltageLimit = nativeVoltageLimit;
    }

    public void addVoltageCnecs() {
        if (nativeAssessedElement.inBaseCase()) {
            addVoltageCnec(crac.getPreventiveInstant().getId(), null);
        }
        for (Contingency contingency : linkedContingencies) {
            addVoltageCnec(crac.getLastInstant().getId(), contingency);
        }
    }

    private void addVoltageCnec(String instantId, Contingency contingency) {
        VoltageCnecAdder voltageCnecAdder = initVoltageCnec();
        addVoltageLimit(voltageCnecAdder);
        addCnecBaseInformation(voltageCnecAdder, contingency, instantId);
        voltageCnecAdder.add();
        markCnecAsImportedAndHandleRejectedContingencies(getCnecName(instantId, contingency));

    }

    private VoltageCnecAdder initVoltageCnec() {
        return crac.newVoltageCnec()
            .withMonitored(true)
            .withOptimized(false)
            .withReliabilityMargin(0);
    }

    private void addVoltageLimit(VoltageCnecAdder voltageCnecAdder) {
        Identifiable<?> networkElement = this.getNetworkElementInNetwork(nativeVoltageLimit.equipment());
        if (networkElement == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, writeAssessedElementIgnoredReasonMessage("the voltage limit equipment " + nativeVoltageLimit.equipment() + " is missing in network"));
        }

        if (!networkElement.getType().equals(IdentifiableType.BUS) && !networkElement.getType().equals(IdentifiableType.BUSBAR_SECTION)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("the network element " + networkElement.getId() + " is not a bus bar section"));
        }

        String networkElementId = networkElement.getId();
        voltageCnecAdder.withNetworkElement(networkElementId);

        if (!nativeVoltageLimit.isInfiniteDuration()) {
            throw new OpenRaoImportException(ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, writeAssessedElementIgnoredReasonMessage("only permanent voltage limits (with infinite duration) are currently handled"));
        }

        addVoltageLimitThreshold(voltageCnecAdder);
    }

    private void addVoltageLimitThreshold(VoltageCnecAdder voltageCnecAdder) {
        if (LimitTypeKind.HIGH_VOLTAGE.toString().equals(nativeVoltageLimit.limitType())) {
            voltageCnecAdder.newThreshold()
                .withUnit(Unit.KILOVOLT)
                .withMax(nativeVoltageLimit.value()).add();
        } else if (LimitTypeKind.LOW_VOLTAGE.toString().equals(nativeVoltageLimit.limitType())) {
            voltageCnecAdder.newThreshold()
                .withUnit(Unit.KILOVOLT)
                .withMin(nativeVoltageLimit.value()).add();
        } else {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, writeAssessedElementIgnoredReasonMessage("a voltage limit can only be of kind highVoltage or lowVoltage"));
        }
    }
}
