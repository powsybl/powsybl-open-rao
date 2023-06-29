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
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.HashSet;
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
            String keyword = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_KEYWORD);
            String contingencyId = contingencyPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY);
            String requestContingencyName = contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_NAME);
            String requestContingencyId = contingencyPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR);
            String contingencyName = requestContingencyId.substring(requestContingencyId.lastIndexOf('/') + 1).concat("_").concat(requestContingencyName);
            Boolean mustStudy = Boolean.parseBoolean(contingencyPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_MUST_STUDY));

            ContingencyAdder contingencyAdder = crac.newContingency()
                    .withId(contingencyId)
                    .withName(contingencyName);

            if (!mustStudy) {
                csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, contingencyName, ImportStatus.INCONSISTENCY_IN_DATA, "contingency.mustStudy is false"));
            } else {
                PropertyBags contingencyEquipments = CsaProfileCracUtils.getLinkedPropertyBags(contingencyEquipmentsPropertyBags, contingencyPropertyBag, CsaProfileConstants.REQUEST_CONTINGENCY, CsaProfileConstants.REQUEST_CONTINGENCY);
                if (contingencyEquipments.isEmpty()) {
                    csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, contingencyName, ImportStatus.INCONSISTENCY_IN_DATA, "no contingency equipment linked to the contingency"));
                } else {
                    boolean isIncorrectContingentStatus = false;
                    boolean isMissingNetworkElement = false;
                    String incorrectContingentStatusElements = "";
                    String missingNetworkElements = "";
                    for (PropertyBag contingencyEquipmentPropertyBag : contingencyEquipments) {
                        String equipmentId = contingencyEquipmentPropertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_ID);
                        String contingentStatus = contingencyEquipmentPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_CONTINGENT_STATUS);

                        if (!CsaProfileConstants.IMPORTED_CONTINGENT_STATUS.equals(contingentStatus)) {
                            isIncorrectContingentStatus = true;
                            incorrectContingentStatusElements = incorrectContingentStatusElements.concat(equipmentId + " ");
                        } else {
                            String networkElementId = this.getNetworkElementIdInNetwork(equipmentId);
                            if (networkElementId == null) {
                                isMissingNetworkElement = true;
                                missingNetworkElements = missingNetworkElements.concat(equipmentId + " ");
                            } else {
                                contingencyAdder.withNetworkElement(networkElementId);
                            }
                        }
                    }

                    if (isIncorrectContingentStatus) {
                        csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, contingencyName, ImportStatus.INCONSISTENCY_IN_DATA, "incorrect contingent status for equipments : " + incorrectContingentStatusElements));
                    } else if (isMissingNetworkElement) {
                        csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.notImported(contingencyId, contingencyName, ImportStatus.INCONSISTENCY_IN_DATA, "missing contingency equipments in network : " + missingNetworkElements));
                    } else {
                        contingencyAdder
                                .add();
                        csaProfileContingencyCreationContexts.add(CsaProfileContingencyCreationContext.imported(contingencyId, contingencyName, "", false));
                    }
                }
            }
        }
        this.cracCreationContext.setContingencyCreationContexts(csaProfileContingencyCreationContexts);
    }

    private String getNetworkElementIdInNetwork(String networkElementIdInCrac) {
        String networkElementId = null;
        Identifiable<?> networkElement = network.getIdentifiable(networkElementIdInCrac);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(networkElementIdInCrac, network);
            if (cgmesBranchHelper.isValid()) {
                networkElementId = cgmesBranchHelper.getIdInNetwork();
            }
        } else {
            networkElementId = networkElement.getId();
        }
        return networkElementId;
    }
}
