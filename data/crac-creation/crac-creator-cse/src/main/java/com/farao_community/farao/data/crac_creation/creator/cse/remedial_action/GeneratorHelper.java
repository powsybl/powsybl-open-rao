/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.remedial_action;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteBusHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class GeneratorHelper {
    private ImportStatus importStatus;
    private boolean isAltered = false;
    private String generatorId = null;
    private String detail = null;

    // Find a generator using a bus ID
    public GeneratorHelper(String busIdInCrac, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        UcteBusHelper busHelper = new UcteBusHelper(busIdInCrac, ucteNetworkAnalyzer);
        if (!busHelper.isValid()) {
            importStatus = ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK;
            detail = busHelper.getInvalidReason();
            return;
        }
        Identifiable<?> busId = ucteNetworkAnalyzer.getNetwork().getIdentifiable(busHelper.getIdInNetwork());
        if (busId instanceof Bus) {
            Bus bus = (Bus) busId;
            // 1 generator connected to bus
            if (bus.getGeneratorStream().count() == 1) {
                importStatus = ImportStatus.IMPORTED;
                generatorId = bus.getGenerators().iterator().next().getId();
                return;
            }
            // > 1  generator connected to bus : return 1st generator + warning
            if (bus.getGeneratorStream().count() > 1) {
                importStatus = ImportStatus.IMPORTED;
                isAltered = true;
                detail = "More than 1 generator associated to {}. Load first generator.";
                generatorId = bus.getGenerators().iterator().next().getId();
                return;
            }
            // 0  generator connected to bus :
            if (bus.getGeneratorStream().count() == 0) {
                importStatus = ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK;
                detail = String.format("No generator associated to %s, filter RA", busId);
                return;
            }
        }
        if (busId instanceof Generator) {
            importStatus = ImportStatus.IMPORTED;
            generatorId = busId.getId();
        }
        importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
        detail = String.format("BusId %s is neither a Bus nor a Generator.", busId);
    }

    public boolean isValid() {
        return importStatus.equals(ImportStatus.IMPORTED);
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public boolean isAltered() {
        return isAltered;
    }

    public String getGeneratorId() {
        return generatorId;
    }

    public String getDetail() {
        return detail;
    }
}
