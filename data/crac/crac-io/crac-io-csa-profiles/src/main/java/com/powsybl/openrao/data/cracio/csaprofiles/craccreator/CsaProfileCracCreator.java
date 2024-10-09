/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracio.csaprofiles.CsaProfileCrac;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.cnec.CsaProfileCnecCreator;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaInstant;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.contingency.CsaProfileContingencyCreator;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction.CsaProfileRemedialActionsCreator;
import com.powsybl.openrao.data.cracio.csaprofiles.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.cracio.commons.RaUsageLimitsAdder;

import java.time.OffsetDateTime;
import java.util.Arrays;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
class CsaProfileCracCreator {

    private Crac crac;
    private Network network;
    CsaProfileCracCreationContext creationContext;
    private CsaProfileCrac nativeCrac;

    CsaProfileCracCreationContext createCrac(CsaProfileCrac nativeCrac, Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreationParameters) {
        CsaCracCreationParameters csaParameters = cracCreationParameters.getExtension(CsaCracCreationParameters.class);
        this.crac = cracCreationParameters.getCracFactory().create(nativeCrac.toString());
        this.network = network;
        this.creationContext = new CsaProfileCracCreationContext(crac, offsetDateTime, network.getNameOrId());
        this.nativeCrac = nativeCrac;
        addCsaInstants();
        RaUsageLimitsAdder.addRaUsageLimits(crac, cracCreationParameters);

        this.nativeCrac.setForTimestamp(offsetDateTime);

        createContingencies();
        createCnecs(cracCreationParameters);
        createRemedialActions(csaParameters.getSpsMaxTimeToImplementThresholdInSeconds());

        creationContext.buildCreationReport();
        return creationContext.creationSuccess(crac);
    }

    private void addCsaInstants() {
        Arrays.stream(CsaInstant.values()).forEach(instant -> crac.newInstant(instant.getInstantName(), instant.getInstantKind()));
    }

    private void createRemedialActions(int spsMaxTimeToImplementThreshold) {
        new CsaProfileRemedialActionsCreator(crac, network, nativeCrac, creationContext, spsMaxTimeToImplementThreshold, creationContext.getCnecCreationContexts());
    }

    private void createContingencies() {
        new CsaProfileContingencyCreator(crac, network, nativeCrac, creationContext);
    }

    private void createCnecs(CracCreationParameters cracCreationParameters) {
        new CsaProfileCnecCreator(crac, network, nativeCrac, creationContext, cracCreationParameters);
    }
}
