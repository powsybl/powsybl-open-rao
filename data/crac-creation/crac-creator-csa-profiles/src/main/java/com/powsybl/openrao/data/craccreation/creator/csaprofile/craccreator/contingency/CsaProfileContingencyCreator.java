/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.contingency;

import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.openrao.data.cracapi.ContingencyAdder;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.NcAggregator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.Contingency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ContingencyEquipment;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileContingencyCreator {

    private final Crac crac;

    private final Network network;

    private final Set<Contingency> nativeContingencies;

    private final Map<String, Set<ContingencyEquipment>> nativeContingencyEquipmentsPerNativeContingency;

    private Set<CsaProfileElementaryCreationContext> csaProfileContingencyCreationContexts;
    private final CsaProfileCracCreationContext cracCreationContext;

    public CsaProfileContingencyCreator(Crac crac, Network network, Set<Contingency> nativeContingencies, Set<ContingencyEquipment> nativeContingencyEquipments, CsaProfileCracCreationContext cracCreationContext) {
        this.crac = crac;
        this.network = network;
        this.nativeContingencies = nativeContingencies;
        this.nativeContingencyEquipmentsPerNativeContingency = new NcAggregator<>(ContingencyEquipment::contingency).aggregate(nativeContingencyEquipments);
        this.cracCreationContext = cracCreationContext;
        this.createAndAddContingencies();
    }

    private void createAndAddContingencies() {
        csaProfileContingencyCreationContexts = new HashSet<>();

        for (Contingency nativeContingency : nativeContingencies) {
            Set<ContingencyEquipment> nativeContingencyEquipments = nativeContingencyEquipmentsPerNativeContingency.getOrDefault(nativeContingency.mrid(), Set.of());
            try {
                addContingency(nativeContingency, nativeContingencyEquipments);
            } catch (OpenRaoImportException exception) {
                csaProfileContingencyCreationContexts.add(CsaProfileElementaryCreationContext.notImported(nativeContingency.mrid(), exception.getImportStatus(), exception.getMessage()));
            }
        }

        cracCreationContext.setContingencyCreationContexts(csaProfileContingencyCreationContexts);
    }

    private void addContingency(Contingency nativeContingency, Set<ContingencyEquipment> nativeContingencyEquipments) {
        List<String> alterations = new ArrayList<>();

        if (!nativeContingency.normalMustStudy()) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, formatNotImportedMessage(nativeContingency.mrid(), "its field mustStudy is set to false"));
        }

        ContingencyAdder contingencyAdder = crac.newContingency().withId(nativeContingency.mrid()).withName(nativeContingency.getUniqueName());
        addContingencyEquipments(nativeContingency.mrid(), nativeContingencyEquipments, contingencyAdder, alterations);
        contingencyAdder.add();

        csaProfileContingencyCreationContexts.add(CsaProfileElementaryCreationContext.imported(nativeContingency.mrid(), nativeContingency.mrid(), nativeContingency.getUniqueName(), String.join(" ", alterations), !alterations.isEmpty()));
    }

    private void addContingencyEquipments(String contingencyId, Set<ContingencyEquipment> nativeContingencyEquipments, ContingencyAdder contingencyAdder, List<String> alterations) {
        if (nativeContingencyEquipments == null || nativeContingencyEquipments.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, formatNotImportedMessage(contingencyId, "no contingency equipment is linked to that contingency"));
        }

        List<String> incorrectContingentStatusElements = new ArrayList<>();
        List<String> missingNetworkElements = new ArrayList<>();

        for (ContingencyEquipment nativeContingencyEquipment : nativeContingencyEquipments) {

            if (!nativeContingencyEquipment.isEquipmentOutOfService()) {
                incorrectContingentStatusElements.add(nativeContingencyEquipment.equipment());
            } else {
                Identifiable<?> networkElement = nativeContingencyEquipment.getEquipmentInNetwork(network);
                if (networkElement == null) {
                    missingNetworkElements.add(nativeContingencyEquipment.equipment());
                } else {
                    ContingencyElementType contingencyElementType = ContingencyElement.of(networkElement).getType();
                    contingencyAdder.withContingencyElement(networkElement.getId(), contingencyElementType);
                }
            }
        }

        incorrectContingentStatusElements.sort(String::compareTo);
        missingNetworkElements.sort(String::compareTo);

        if (incorrectContingentStatusElements.size() == nativeContingencyEquipments.size()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, formatNotImportedMessage(contingencyId, "all contingency equipments have an incorrect contingent status: %s".formatted(String.join(", ", incorrectContingentStatusElements))));
        }

        if (missingNetworkElements.size() == nativeContingencyEquipments.size()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, formatNotImportedMessage(contingencyId, "all contingency equipments are missing in the network: %s".formatted(String.join(", ", missingNetworkElements))));
        }

        if (!incorrectContingentStatusElements.isEmpty()) {
            alterations.add("Incorrect contingent status for equipment(s): %s.".formatted(String.join(", ", incorrectContingentStatusElements)));
        }

        if (!missingNetworkElements.isEmpty()) {
            alterations.add("Missing contingent equipment(s) in network: %s.".formatted(String.join(", ", missingNetworkElements)));
        }
    }

    private static String formatNotImportedMessage(String contingencyId, String reason) {
        return "Contingency %s will not be imported because %s".formatted(contingencyId, reason);
    }
}
