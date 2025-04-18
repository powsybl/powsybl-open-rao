/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.io.nc.NcCrac;
import com.powsybl.openrao.data.crac.io.nc.craccreator.contingency.NcContingencyCreator;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.nc.craccreator.cnec.NcCnecCreator;
import com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction.NcRemedialActionsCreator;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;
import com.powsybl.openrao.data.crac.io.commons.RaUsageLimitsAdder;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants.*;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
class NcCracCreator {

    private Crac crac;
    private Network network;
    NcCracCreationContext creationContext;
    private NcCrac nativeCrac;

    NcCracCreationContext createCrac(NcCrac nativeCrac, Network network, CracCreationParameters cracCreationParameters) {
        this.crac = cracCreationParameters.getCracFactory().create(nativeCrac.toString());
        this.network = network;
        NcCracCreationParameters ncParameters = cracCreationParameters.getExtension(NcCracCreationParameters.class);
        OffsetDateTime offsetDateTime = null;
        if (ncParameters != null) {
            offsetDateTime = ncParameters.getTimestamp();
        }
        this.creationContext = new NcCracCreationContext(crac, offsetDateTime, network.getNameOrId());
        this.nativeCrac = nativeCrac;

        if (offsetDateTime == null) {
            creationContext.getCreationReport().error("Timestamp is null for NC crac creator.");
            creationContext.creationFailure();
            return creationContext;
        }

        addCsaInstants(ncParameters);
        RaUsageLimitsAdder.addRaUsageLimits(crac, cracCreationParameters);

        this.nativeCrac.setForTimestamp(offsetDateTime);

        createContingencies();
        createCnecs(cracCreationParameters);
        createRemedialActions();

        creationContext.buildCreationReport();
        return creationContext.creationSuccess(crac);
    }

    private void addCsaInstants(NcCracCreationParameters ncCracCreationParameters) {
        crac.newInstant(PREVENTIVE_INSTANT, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT, InstantKind.AUTO);
        List<String> sortedCurativeInstants = ncCracCreationParameters.getCurativeInstants().entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue)).map(Map.Entry::getKey).toList();
        sortedCurativeInstants.forEach(instantName -> crac.newInstant(instantName, InstantKind.CURATIVE));
    }

    private void createRemedialActions() {
        new NcRemedialActionsCreator(crac, network, nativeCrac, creationContext, creationContext.getCnecCreationContexts());
    }

    private void createContingencies() {
        new NcContingencyCreator(crac, network, nativeCrac, creationContext);
    }

    private void createCnecs(CracCreationParameters cracCreationParameters) {
        new NcCnecCreator(crac, network, nativeCrac, creationContext, cracCreationParameters);
    }
}
