/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.contingency.ContingencyElementFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.RangeActionGroup;
import com.powsybl.openrao.data.crac.io.network.parameters.Contingencies;
import com.powsybl.openrao.data.crac.io.network.parameters.NetworkCracCreationParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Creates a CRAC from a network.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class NetworkCracCreator {

    private NetworkCracCreator() {
        // should not be used
    }

    public static NetworkCracCreationContext createCrac(Network network, CracCreationParameters cracCreationParameters) {
        if (cracCreationParameters.getExtension(NetworkCracCreationParameters.class) == null) {
            throw new OpenRaoException("Cannot create a CRAC from a network file unless a NetworkCracCreationParameters extension is defined in CracCreationParameters.");
        }
        NetworkCracCreationParameters specificParameters = cracCreationParameters.getExtension(NetworkCracCreationParameters.class);

        String cracId = "CRAC_FROM_NETWORK_" + network.getNameOrId();
        Crac crac = cracCreationParameters.getCracFactory().create(cracId, cracId, network.getCaseDate().toOffsetDateTime());
        NetworkCracCreationContext creationContext = new NetworkCracCreationContext(crac, network.getNameOrId());
        addInstants(crac, specificParameters.getInstants());
        addContingencies(crac, network, specificParameters.getContingencies());

        new CnecCreator(creationContext, network, cracCreationParameters).addCnecs();
        new PstRangeActionsCreator(crac, network, specificParameters.getPstRangeActions(), computeRaGroupsMap(specificParameters)).addPstRangeActions();
        new RedispatchingCreator(creationContext, network, specificParameters.getRedispatchingRangeActions()).addRedispatchRangeActions();
        new CountertradingRangeActionsCreator(creationContext, network, specificParameters.getCountertradingRangeActions()).addCountertradingActions();
        new BalancingRangeActionCreator(creationContext, network, specificParameters.getBalancingRangeAction()).addBalancingRangeAction();

        creationContext.setCreationSuccessful(true);
        return creationContext;
    }

    private static Map<String, String> computeRaGroupsMap(NetworkCracCreationParameters specificParameters) {
        Map<String, String> raGroupPerNetworkElement = new HashMap<>();
        int i = 0;
        for (RangeActionGroup group : specificParameters.getRangeActionGroups()) {
            String groupId = "RA_GROUP_" + i++;
            group.getRangeActionsIds().forEach(ne -> raGroupPerNetworkElement.put(ne, groupId));
        }
        return raGroupPerNetworkElement;
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
                    .withContingencyElement(branch.getId(), ContingencyElementFactory.create(branch).getType())
                    .add());
    }
}
