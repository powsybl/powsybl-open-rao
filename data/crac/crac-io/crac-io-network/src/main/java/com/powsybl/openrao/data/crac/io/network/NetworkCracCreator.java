/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;


import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.network.parameters.*;

import java.util.*;

/**
 * Creates a CRAC from a network.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkCracCreator {
    private NetworkCracCreationContext creationContext;
    private CracCreationParameters cracCreationParameters;
    private NetworkCracCreationParameters specificParameters;
    private Network network;
    private Crac crac;
    private final Map<Branch<?>, Contingency> contingencyPerBranch = new HashMap<>();

    NetworkCracCreationContext createCrac(Network network, CracCreationParameters cracCreationParameters) {
        String cracId = "CRAC_FROM_NETWORK_" + network.getNameOrId();
        this.network = network;
        crac = cracCreationParameters.getCracFactory().create(cracId, cracId, network.getCaseDate().toOffsetDateTime());
        creationContext = new NetworkCracCreationContext(crac, network.getNameOrId());
        if (cracCreationParameters.getExtension(NetworkCracCreationParameters.class) == null) {
            throw new OpenRaoException("Cannot create a CRAC from a network file unless a NetworkCracCreationParameters extension is defined in CracCreationParameters.");
        }
        this.cracCreationParameters = cracCreationParameters;
        specificParameters = cracCreationParameters.getExtension(NetworkCracCreationParameters.class);
        addInstants();
        addContingencies();
        new CnecCreator(crac, network, cracCreationParameters, creationContext, contingencyPerBranch).addCnecsAndMnecs();
        new PstCreator(crac, network, specificParameters.getPstRangeActions()).addPstRangeActions();
        new RedispatchingCreator(crac, network, specificParameters.getRedispatchingRangeActions()).addRedispatchRangeActions();
        creationContext.setCreationSuccessful(true);
        return creationContext;
    }

    private void addInstants() {
        specificParameters.getInstants().forEach((instantKind, ids) ->
            ids.forEach(id -> crac.newInstant(id, instantKind)));
    }

    private void addContingencies() {
        Contingencies params = specificParameters.getContingencies();
        network.getBranchStream().filter(b ->
                Utils.branchIsInCountries(b, params.getCountries().orElse(null))
                    && Utils.branchIsInVRange(b, params.getMinV(), params.getMaxV()))
            .forEach(
                branch -> {
                    Contingency contingency = crac.newContingency()
                        .withId("CO_" + branch.getNameOrId())
                        .withContingencyElement(branch.getId(), ContingencyElementType.BRANCH)
                        .add();
                    contingencyPerBranch.put(branch, contingency);
                }
            );
    }


}
