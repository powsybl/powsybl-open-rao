/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency;

import com.farao_community.farao.data.crac_api.ContingencyAdder;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileContingencyCreator {

    private final Crac crac;

    private final Network network;

    private final PropertyBags contingenciesPropertyBags;

    private final PropertyBags contingencyEquipmentsPropertyBags;

    private Set<CsaProfileContingencyCreationContext> csaProfileContingencyCreationContexts;
    private CsaProfileCracCreationContext cracCreationContext;

    public CsaProfileContingencyCreator(Crac crac, Network network, PropertyBags contingenciesPropertyBags, PropertyBags contingencyEquipmentsPropertyBags, CsaProfileCracCreationContext cracCreationContext) {
        this.crac = crac;
        this.network = network;
        this.contingenciesPropertyBags = contingenciesPropertyBags;
        this.contingencyEquipmentsPropertyBags = contingencyEquipmentsPropertyBags;
        this.cracCreationContext = cracCreationContext;
    }

    public void createAndAddContingencies() {
        this.csaProfileContingencyCreationContexts = new HashSet<>();

        for (PropertyBag contingencyPropertyBag : contingenciesPropertyBags) {
            this.addContingency(contingencyPropertyBag);
        }
        this.cracCreationContext.setContingencyCreationContexts(csaProfileContingencyCreationContexts);
    }

    private void addContingency(PropertyBag contingencyPropertyBag) {
        String keyword = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_KEYWORD);
        String startTime = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_START_DATE);
        String endTime = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_END_DATE);
        String contingencyId = contingencyPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY);
        String nativeContingencyName = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_NAME);
        String equipmentOperator = contingencyPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR);
        String contingencyName = CsaProfileCracUtils.getUniqueName(equipmentOperator, nativeContingencyName);
        Boolean mustStudy = Boolean.parseBoolean(contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_MUST_STUDY));

        if (!CsaProfileConstants.CONTINGENCY_FILE_KEYWORD.equals(keyword)) {
            csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, ImportStatus.INCONSISTENCY_IN_DATA, "Model.keyword must be CO, but it is " + keyword));
            return;
        }

        if (!this.isValidInterval(startTime, endTime)) {
            csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "Required timestamp does not fall between Model.startDate and Model.endDate"));
            return;
        }

        if (!mustStudy) {
            csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, ImportStatus.NOT_FOR_RAO, "contingency.mustStudy is false"));
            return;
        }

        PropertyBags contingencyEquipments = CsaProfileCracUtils.getLinkedPropertyBags(contingencyEquipmentsPropertyBags, contingencyPropertyBag, CsaProfileConstants.REQUEST_CONTINGENCY, CsaProfileConstants.REQUEST_CONTINGENCY);
        if (contingencyEquipments.isEmpty()) {
            csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, ImportStatus.INCOMPLETE_DATA, "no contingency equipment linked to the contingency"));
            return;
        }

        ContingencyAdder contingencyAdder = crac.newContingency()
                .withId(contingencyId)
                .withName(contingencyName);
        boolean isIncorrectContingentStatus = false;
        boolean isMissingNetworkElement = false;
        boolean atLeastOneCorrectContingentStatus = false;
        boolean atLeastOneNetworkElement = false;
        String incorrectContingentStatusElements = "";
        String missingNetworkElements = "";
        for (PropertyBag contingencyEquipmentPropertyBag : contingencyEquipments) {
            String equipmentId = contingencyEquipmentPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_ID);
            String contingentStatus = contingencyEquipmentPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_CONTINGENT_STATUS);

            if (!CsaProfileConstants.IMPORTED_CONTINGENT_STATUS.equals(contingentStatus)) {
                isIncorrectContingentStatus = true;
                incorrectContingentStatusElements = incorrectContingentStatusElements.concat(equipmentId + " ");
            } else {
                atLeastOneCorrectContingentStatus = true;
                String networkElementId = this.getNetworkElementIdInNetwork(equipmentId);
                if (networkElementId == null) {
                    isMissingNetworkElement = true;
                    missingNetworkElements = missingNetworkElements.concat(equipmentId + " ");
                } else {
                    atLeastOneNetworkElement = true;
                    contingencyAdder.withNetworkElement(networkElementId);
                }
            }
        }

        if (!atLeastOneCorrectContingentStatus) {
            csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, ImportStatus.INCONSISTENCY_IN_DATA, "all contingency equipments have incorrect contingent status : " + incorrectContingentStatusElements));
            return;
        }

        if (!atLeastOneNetworkElement) {
            csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, ImportStatus.INCONSISTENCY_IN_DATA, "all contingency equipments are missing in network : " + missingNetworkElements));
            return;
        }

        contingencyAdder
                .add();

        if (isIncorrectContingentStatus) {
            csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.imported(contingencyId, contingencyId, contingencyName, "incorrect contingent status for equipment(s) : " + incorrectContingentStatusElements, true));
            return;
        }

        if (isMissingNetworkElement) {
            csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.imported(contingencyId, contingencyId, contingencyName, "missing contingent equipment(s) in network : " + incorrectContingentStatusElements, true));
            return;
        }

        csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.imported(contingencyId, contingencyId, contingencyName, "", false));
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

        if (networkElement instanceof DanglingLine) {
            Optional<TieLine> optionalTieLine = ((DanglingLine) networkElement).getTieLine();
            if (optionalTieLine.isPresent()) {
                networkElementId = optionalTieLine.get().getId();
            }
        }
        return networkElementId;
    }

    private boolean isValidInterval(String startTime, String endTime) {
        OffsetDateTime cracCreationTimeStamp = cracCreationContext.getTimeStamp();
        OffsetDateTime startDateTime = OffsetDateTime.parse(startTime);
        OffsetDateTime endDateTime = OffsetDateTime.parse(endTime);
        return !cracCreationTimeStamp.isBefore(startDateTime) && !cracCreationTimeStamp.isAfter(endDateTime);
    }
}
