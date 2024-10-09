/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.nc;

import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileConstants;
import com.powsybl.openrao.data.cracio.commons.cgmes.CgmesBranchHelper;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record ContingencyEquipment(String mrid, String contingency, String contingentStatus, String equipment) implements NCObject {

    public boolean isEquipmentOutOfService() {
        return CsaProfileConstants.OUT_OF_SERVICE_CONTINGENT_STATUS.equals(contingentStatus);
    }

    public Identifiable<?> getEquipmentInNetwork(Network network) {
        Identifiable<?> networkElementToReturn = null;
        Identifiable<?> networkElement = network.getIdentifiable(equipment);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(equipment, network);
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

    public static ContingencyEquipment fromPropertyBag(PropertyBag propertyBag) {
        return new ContingencyEquipment(
            propertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY_EQUIPMENT),
            propertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY),
            propertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_CONTINGENT_STATUS),
            propertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_ID)
        );
    }
}
