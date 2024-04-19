/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.contingency;

import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.openrao.data.cracapi.ContingencyAdder;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.openrao.data.craccreation.util.cgmes.CgmesBranchHelper;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileContingencyCreator {

    private final Crac crac;

    private final Network network;

    private final PropertyBags contingenciesPropertyBags;

    private final Map<String, Set<PropertyBag>> contingencyEquipmentsPropertyBags;

    private Set<CsaProfileElementaryCreationContext> csaProfileContingencyCreationContexts;
    private final CsaProfileCracCreationContext cracCreationContext;

    public CsaProfileContingencyCreator(Crac crac, Network network, PropertyBags contingenciesPropertyBags, PropertyBags contingencyEquipmentsPropertyBags, CsaProfileCracCreationContext cracCreationContext) {
        this.crac = crac;
        this.network = network;
        this.contingenciesPropertyBags = contingenciesPropertyBags;
        this.contingencyEquipmentsPropertyBags = CsaProfileCracUtils.groupPropertyBagsBy(contingencyEquipmentsPropertyBags, CsaProfileConstants.REQUEST_CONTINGENCY);
        this.cracCreationContext = cracCreationContext;
        this.createAndAddContingencies();
    }

    private void createAndAddContingencies() {
        csaProfileContingencyCreationContexts = new HashSet<>();

        for (PropertyBag contingencyPropertyBag : contingenciesPropertyBags) {
            String contingencyId = contingencyPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY);
            try {
                addContingency(contingencyPropertyBag);
            } catch (OpenRaoImportException exception) {
                csaProfileContingencyCreationContexts.add(CsaProfileElementaryCreationContext.notImported(contingencyId, exception.getImportStatus(), exception.getMessage()));
            }
        }

        cracCreationContext.setContingencyCreationContexts(csaProfileContingencyCreationContexts);
    }

    private void addContingency(PropertyBag contingencyPropertyBag) {

        String contingencyId = contingencyPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY);
        String contingencyName = CsaProfileCracUtils.createElementName(contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_NAME), contingencyPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR)).orElse(contingencyId);
        List<String> alterations = new ArrayList<>();

        checkMustStudy(contingencyId, Boolean.parseBoolean(contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_NORMAL_MUST_STUDY)));
        Set<PropertyBag> contingencyEquipments = contingencyEquipmentsPropertyBags.get(contingencyId);

        ContingencyAdder contingencyAdder = crac.newContingency().withId(contingencyId).withName(contingencyName);
        addContingencyEquipments(contingencyId, contingencyEquipments, contingencyAdder, alterations);
        contingencyAdder.add();

        csaProfileContingencyCreationContexts.add(CsaProfileElementaryCreationContext.imported(contingencyId, contingencyId, contingencyName, String.join(" ", alterations), !alterations.isEmpty()));
    }

    private static void checkMustStudy(String contingencyId, boolean mustStudy) {
        if (!Boolean.TRUE.equals(mustStudy)) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, formatNotImportedMessage(contingencyId, "its field mustStudy is set to false"));
        }
    }

    private void addContingencyEquipments(String contingencyId, Set<PropertyBag> contingencyEquipments, ContingencyAdder contingencyAdder, List<String> alterations) {
        if (contingencyEquipments == null || contingencyEquipments.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, formatNotImportedMessage(contingencyId, "no contingency equipment is linked to that contingency"));
        }

        List<String> incorrectContingentStatusElements = new ArrayList<>();
        List<String> missingNetworkElements = new ArrayList<>();

        for (PropertyBag contingencyEquipmentPropertyBag : contingencyEquipments) {
            String equipmentId = contingencyEquipmentPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_ID);
            String contingentStatus = contingencyEquipmentPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_CONTINGENT_STATUS);

            if (!CsaProfileConstants.IMPORTED_CONTINGENT_STATUS.equals(contingentStatus)) {
                incorrectContingentStatusElements.add(equipmentId);
            } else {
                Identifiable<?> networkElement = getNetworkElementInNetwork(equipmentId);
                if (networkElement == null) {
                    missingNetworkElements.add(equipmentId);
                } else {
                    String networkElementId = networkElement.getId();
                    ContingencyElementType contingencyElementType = ContingencyElement.of(networkElement).getType();
                    contingencyAdder.withContingencyElement(networkElementId, contingencyElementType);
                }
            }
        }

        incorrectContingentStatusElements.sort(String::compareTo);
        missingNetworkElements.sort(String::compareTo);

        if (incorrectContingentStatusElements.size() == contingencyEquipments.size()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, formatNotImportedMessage(contingencyId, "all contingency equipments have an incorrect contingent status: %s".formatted(String.join(", ", incorrectContingentStatusElements))));
        }

        if (missingNetworkElements.size() == contingencyEquipments.size()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, formatNotImportedMessage(contingencyId, "all contingency equipments are missing in the network: %s".formatted(String.join(", ", missingNetworkElements))));
        }

        if (!incorrectContingentStatusElements.isEmpty()) {
            alterations.add("Incorrect contingent status for equipment(s): %s.".formatted(String.join(", ", incorrectContingentStatusElements)));
        }

        if (!missingNetworkElements.isEmpty()) {
            alterations.add("Missing contingent equipment(s) in network: %s.".formatted(String.join(", ", missingNetworkElements)));
        }
    }

    private Identifiable<?> getNetworkElementInNetwork(String networkElementIdInCrac) {
        Identifiable<?> networkElementToReturn = null;
        Identifiable<?> networkElement = network.getIdentifiable(networkElementIdInCrac);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(networkElementIdInCrac, network);
            if (cgmesBranchHelper.isValid()) {
                networkElementToReturn = cgmesBranchHelper.getBranch();
                networkElement = cgmesBranchHelper.getBranch();
            }
        } else {
            networkElementToReturn = networkElement;
        }

        if (networkElement instanceof DanglingLine danglingLine) {
            Optional<TieLine> optionalTieLine = danglingLine.getTieLine();
            if (optionalTieLine.isPresent()) {
                networkElementToReturn = optionalTieLine.get();
            }
        }
        return networkElementToReturn;
    }

    private static String formatNotImportedMessage(String contingencyId, String reason) {
        return "Contingency %s will not be imported because %s".formatted(contingencyId, reason);
    }
}
