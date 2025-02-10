/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.csaprofiles.craccreator;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.contingency.CsaProfileContingencyCreator;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.csaprofiles.CsaProfileCrac;
import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.cnec.CsaProfileCnecCreator;
import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.remedialaction.CsaProfileRemedialActionsCreator;
import com.powsybl.openrao.data.crac.io.csaprofiles.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.crac.io.commons.RaUsageLimitsAdder;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.constants.CsaProfileConstants.*;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
class CsaProfileCracCreator {

    private Crac crac;
    private Network network;
    CsaProfileCracCreationContext creationContext;
    private CsaProfileCrac nativeCrac;

    CsaProfileCracCreationContext createCrac(CsaProfileCrac nativeCrac, Network network, CracCreationParameters cracCreationParameters) {
        CsaCracCreationParameters csaParameters = cracCreationParameters.getExtension(CsaCracCreationParameters.class);
        OffsetDateTime offsetDateTime = csaParameters.getTimestamp();
        this.crac = cracCreationParameters.getCracFactory().create(nativeCrac.toString());
        this.network = network;
        this.creationContext = new CsaProfileCracCreationContext(crac, offsetDateTime, network.getNameOrId());
        this.nativeCrac = nativeCrac;
        addCsaInstants(csaParameters);
        RaUsageLimitsAdder.addRaUsageLimits(crac, cracCreationParameters);

        this.nativeCrac.setForTimestamp(offsetDateTime);

        createContingencies();
        createCnecs(cracCreationParameters);
        createRemedialActions(csaParameters.getAutoInstantApplicationTime());

        creationContext.buildCreationReport();
        return creationContext.creationSuccess(crac);
    }

    private void addCsaInstants(CsaCracCreationParameters csaCracCreationParameters) {
        crac.newInstant(PREVENTIVE_INSTANT, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT, InstantKind.AUTO);
        List<String> sortedCurativeInstants = csaCracCreationParameters.getCurativeInstants().entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue)).map(Map.Entry::getKey).toList();
        sortedCurativeInstants.forEach(instantName -> crac.newInstant(instantName, InstantKind.CURATIVE));
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
