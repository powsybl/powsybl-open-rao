/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.contingency;

import com.powsybl.openrao.data.cracapi.ContingencyAdder;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
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
        this.contingencyEquipmentsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(contingencyEquipmentsPropertyBags, CsaProfileConstants.REQUEST_CONTINGENCY);
        this.cracCreationContext = cracCreationContext;
        this.createAndAddContingencies();
    }

    private void createAndAddContingencies() {
        this.csaProfileContingencyCreationContexts = new HashSet<>();

        for (PropertyBag contingencyPropertyBag : contingenciesPropertyBags) {
            this.addContingency(contingencyPropertyBag);
        }
        this.cracCreationContext.setContingencyCreationContexts(csaProfileContingencyCreationContexts);
    }

    private void addContingency(PropertyBag contingencyPropertyBag) {

        String contingencyId = contingencyPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY);
        Set<PropertyBag> contingencyEquipments = this.dataCheck(contingencyPropertyBag, contingencyId);
        if (contingencyEquipments.isEmpty()) {
            return;
        }

        String nativeContingencyName = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_NAME);
        String equipmentOperator = contingencyPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR);
        Optional<String> contingencyNameOpt = CsaProfileCracUtils.createElementName(nativeContingencyName, equipmentOperator);
        String contingencyName = contingencyNameOpt.orElse(contingencyId);

        ContingencyAdder contingencyAdder = crac.newContingency()
            .withId(contingencyId)
            .withName(contingencyName);
        boolean isIncorrectContingentStatus = false;
        boolean isMissingNetworkElement = false;
        boolean atLeastOneCorrectContingentStatus = false;
        boolean atLeastOneNetworkElement = false;
        List<String> incorrectContingentStatusElements = new ArrayList<>();
        List<String> missingNetworkElements = new ArrayList<>();
        for (PropertyBag contingencyEquipmentPropertyBag : contingencyEquipments) {
            String equipmentId = contingencyEquipmentPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_ID);
            String contingentStatus = contingencyEquipmentPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_CONTINGENT_STATUS);

            if (!CsaProfileConstants.IMPORTED_CONTINGENT_STATUS.equals(contingentStatus)) {
                isIncorrectContingentStatus = true;
                incorrectContingentStatusElements.add(equipmentId);
            } else {
                atLeastOneCorrectContingentStatus = true;
                String networkElementId = this.getNetworkElementIdInNetwork(equipmentId);
                if (networkElementId == null) {
                    isMissingNetworkElement = true;
                    missingNetworkElements.add(equipmentId);
                } else {
                    atLeastOneNetworkElement = true;
                    contingencyAdder.withNetworkElement(networkElementId);
                }
            }
        }

        incorrectContingentStatusElements.sort(String::compareTo);
        missingNetworkElements.sort(String::compareTo);

        if (!atLeastOneCorrectContingentStatus) {
            csaProfileContingencyCreationContexts.add(CsaProfileElementaryCreationContext.notImported(contingencyId, ImportStatus.INCONSISTENCY_IN_DATA, formatNotImportedMessage(contingencyId, "all contingency equipments have an incorrect contingent status: " + String.join(", ", incorrectContingentStatusElements))));
            return;
        }

        if (!atLeastOneNetworkElement) {
            csaProfileContingencyCreationContexts.add(CsaProfileElementaryCreationContext.notImported(contingencyId, ImportStatus.INCONSISTENCY_IN_DATA, formatNotImportedMessage(contingencyId, "all contingency equipments are missing in the network: " + String.join(", ", missingNetworkElements))));
            return;
        }

        contingencyAdder.add();
        List<String> importStatusDetail = new ArrayList<>();

        if (isIncorrectContingentStatus) {
            importStatusDetail.add("incorrect contingent status for equipment(s): " + String.join(", ", incorrectContingentStatusElements));
        }

        if (isMissingNetworkElement) {
            importStatusDetail.add("missing contingent equipment(s) in network: " + String.join(", ", missingNetworkElements));
        }

        csaProfileContingencyCreationContexts.add(CsaProfileElementaryCreationContext.imported(contingencyId, contingencyId, contingencyName, String.join("; ", importStatusDetail), isIncorrectContingentStatus || isMissingNetworkElement));
    }

    private Set<PropertyBag> dataCheck(PropertyBag contingencyPropertyBag, String contingencyId) {
        Boolean mustStudy = Boolean.parseBoolean(contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_NORMAL_MUST_STUDY));
        if (!Boolean.TRUE.equals(mustStudy)) {
            csaProfileContingencyCreationContexts.add(CsaProfileElementaryCreationContext.notImported(contingencyId, ImportStatus.NOT_FOR_RAO, formatNotImportedMessage(contingencyId, "its field mustStudy is set to false")));
            return new HashSet<>();
        }

        Set<PropertyBag> contingencyEquipments = contingencyEquipmentsPropertyBags.get(contingencyPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY));
        if (contingencyEquipments == null) {
            csaProfileContingencyCreationContexts.add(CsaProfileElementaryCreationContext.notImported(contingencyId, ImportStatus.INCOMPLETE_DATA, formatNotImportedMessage(contingencyId, "no contingency equipment is linked to the contingency")));
            return new HashSet<>();
        }

        return contingencyEquipments;
    }

    private String getNetworkElementIdInNetwork(String networkElementIdInCrac) {
        String networkElementId = null;
        Identifiable<?> networkElement = network.getIdentifiable(networkElementIdInCrac);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(networkElementIdInCrac, network);
            if (cgmesBranchHelper.isValid()) {
                networkElementId = cgmesBranchHelper.getIdInNetwork();
                networkElement = cgmesBranchHelper.getBranch();
            }
        } else {
            networkElementId = networkElement.getId();
        }

        if (networkElement instanceof DanglingLine danglingLine) {
            Optional<TieLine> optionalTieLine = danglingLine.getTieLine();
            if (optionalTieLine.isPresent()) {
                networkElementId = optionalTieLine.get().getId();
            }
        }
        return networkElementId;
    }

    private static String formatNotImportedMessage(String contingencyId, String reason) {
        return "Contingency %s will not be imported because %s".formatted(contingencyId, reason);
    }
}
