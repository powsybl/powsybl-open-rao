/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;


import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.network.parameters.*;

import java.util.*;

/**
 * Creates a CRAC from a network.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkCracCreator {

    NetworkCracCreationContext createCrac(Network network, CracCreationParameters cracCreationParameters) {
        String cracId = "CRAC_FROM_NETWORK_" + network.getNameOrId();
        Crac crac = cracCreationParameters.getCracFactory().create(cracId, cracId, network.getCaseDate().toOffsetDateTime());
        NetworkCracCreationContext creationContext = new NetworkCracCreationContext(crac, network.getNameOrId());
        if (cracCreationParameters.getExtension(NetworkCracCreationParameters.class) == null) {
            throw new OpenRaoException("Cannot create a CRAC from a network file unless a NetworkCracCreationParameters extension is defined in CracCreationParameters.");
        }
        NetworkCracCreationParameters specificParameters = cracCreationParameters.getExtension(NetworkCracCreationParameters.class);

        addInstants(crac, specificParameters.getInstants());
        addContingencies(crac, network, specificParameters.getContingencies());
        new CnecCreator(crac, network, cracCreationParameters, creationContext).addCnecsAndMnecs();
        new PstRangeActionsCreator(crac, network, specificParameters.getPstRangeActions()).addPstRangeActions();
        new RedispatchingCreator(crac, network, specificParameters.getRedispatchingRangeActions()).addRedispatchRangeActions();
        new CountertradingRangeActionsCreator(crac, network, specificParameters.getCountertradingRangeActions(), creationContext).addCountertradingActions();
        new BalancingRangeActionsCreator(crac, network, specificParameters.getBalancingRangeActions()).addBalancingRangeActions();

        creationContext.setCreationSuccessful(true);
        return creationContext;
    }

    private static void addInstants(Crac crac, SortedMap<InstantKind, List<String>> instants) {
        instants.forEach((instantKind, ids) ->
            ids.forEach(id -> crac.newInstant(id, instantKind)));
    }

    private static void addContingencies(Crac crac, Network network, Contingencies parameters) {
        network.getBranchStream().filter(b ->
                Utils.branchIsInCountries(b, parameters.getCountries().orElse(null))
                    && Utils.branchIsInVRange(b, parameters.getMinV(), parameters.getMaxV()))
            .forEach(
                branch -> crac.newContingency()
                    .withId("CO_" + branch.getNameOrId())
                    .withContingencyElement(branch.getId(), ContingencyElementType.BRANCH)
                    .add()
            );
    }
}
