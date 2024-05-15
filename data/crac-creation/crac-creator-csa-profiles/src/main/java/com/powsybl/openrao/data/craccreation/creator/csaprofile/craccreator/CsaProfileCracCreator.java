/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreator;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.CsaProfileCrac;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec.CsaProfileCnecCreator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.contingency.CsaProfileContingencyCreator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction.CsaProfileRemedialActionsCreator;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.craccreation.util.RaUsageLimitsAdder;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class CsaProfileCracCreator implements CracCreator<CsaProfileCrac, CsaProfileCracCreationContext> {

    private Crac crac;
    private Network network;
    CsaProfileCracCreationContext creationContext;
    private CsaProfileCrac nativeCrac;

    @Override
    public String getNativeCracFormat() {
        return "CsaProfileCrac";
    }

    public CsaProfileCracCreationContext createCrac(CsaProfileCrac nativeCrac, Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreationParameters) {
        CsaCracCreationParameters csaParameters = cracCreationParameters.getExtension(CsaCracCreationParameters.class);
        this.crac = cracCreationParameters.getCracFactory().create(nativeCrac.toString());
        this.network = network;
        this.creationContext = new CsaProfileCracCreationContext(crac, offsetDateTime, network.getNameOrId());
        this.nativeCrac = nativeCrac;
        addCsaInstants();
        RaUsageLimitsAdder.addRaUsageLimits(crac, cracCreationParameters);

        this.nativeCrac.setForTimestamp(offsetDateTime);

        createContingencies();
        createCnecs(cracCreationParameters.getDefaultMonitoredSides(), csaParameters.getCapacityCalculationRegionEicCode());
        createRemedialActions(csaParameters.getSpsMaxTimeToImplementThresholdInSeconds());

        creationContext.buildCreationReport();
        return creationContext.creationSuccess(crac);
    }

    private void addCsaInstants() {
        crac.newInstant(PREVENTIVE_INSTANT, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT, InstantKind.AUTO)
            .newInstant(CURATIVE_1_INSTANT, InstantKind.CURATIVE)
            .newInstant(CURATIVE_2_INSTANT, InstantKind.CURATIVE)
            .newInstant(CURATIVE_3_INSTANT, InstantKind.CURATIVE);
    }

    private void createRemedialActions(int spsMaxTimeToImplementThreshold) {
        new CsaProfileRemedialActionsCreator(crac, network, nativeCrac, creationContext, spsMaxTimeToImplementThreshold, creationContext.getCnecCreationContexts());
    }

    private void createContingencies() {
        new CsaProfileContingencyCreator(crac, network, nativeCrac, creationContext);
    }

    private void createCnecs(Set<Side> defaultMonitoredSides, String regionEic) {
        new CsaProfileCnecCreator(crac, network, nativeCrac, creationContext, defaultMonitoredSides, regionEic);
    }
}
