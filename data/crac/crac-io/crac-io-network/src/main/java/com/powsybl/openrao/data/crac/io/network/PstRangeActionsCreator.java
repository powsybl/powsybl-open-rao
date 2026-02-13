/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.PstHelper;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmPstHelper;
import com.powsybl.openrao.data.crac.io.network.parameters.PstRangeActions;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class PstRangeActionsCreator {
    private final Crac crac;
    private final Network network;
    private final PstRangeActions parameters;
    private final Map<String, String> raGroupPerNetworkElement;

    public PstRangeActionsCreator(Crac crac, Network network, PstRangeActions parameters, Map<String, String> raGroupPerNetworkElement) {
        this.crac = crac;
        this.network = network;
        this.parameters = parameters;
        this.raGroupPerNetworkElement = raGroupPerNetworkElement;
    }

    void addPstRangeActions() {
        Set<Instant> instants = crac.getSortedInstants().stream().filter(instant -> !instant.isOutage())
            .filter(parameters::arePstsAvailableForInstant).collect(Collectors.toSet());
        network.getTwoWindingsTransformerStream()
            .filter(twt -> twt.getPhaseTapChanger() != null)
            .filter(twt -> Utils.branchIsInCountries(twt, parameters.getCountries().orElse(null)))
            .forEach(twt -> instants.forEach(instant -> addPstRangeActionForInstant(twt, instant)));
    }

    private void addPstRangeActionForInstant(TwoWindingsTransformer twt, Instant instant) {
        PstHelper pstHelper = new IidmPstHelper(twt.getId(), network);
        PstRangeActionAdder pstAdder = crac.newPstRangeAction()
            .withId("PST_RA_" + twt.getId() + "_" + instant.getId())
            .withNetworkElement(twt.getId())
            .withInitialTap(pstHelper.getInitialTap())
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(pstHelper.getLowTapPosition()).withMaxTap(pstHelper.getHighTapPosition()).add()
            .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap());
        if (raGroupPerNetworkElement.containsKey(twt.getId())) {
            pstAdder.withGroupId(raGroupPerNetworkElement.get(twt.getId()));
        }
        // TODO fail if one PST listed in a group is not added as a RA? (to prevent having unrealistic cases)

        boolean availableForAllStates = crac.getStates(instant).stream().allMatch(state -> parameters.isAvailable(twt, state));
        if (availableForAllStates) {
            pstAdder.newOnInstantUsageRule().withInstant(instant.getId()).add();
        } else {
            crac.getStates().stream().filter(state -> parameters.isAvailable(twt, state))
                .forEach(
                    state -> pstAdder.newOnContingencyStateUsageRule()
                        .withInstant(instant.getId())
                        .withContingency(state.getContingency().orElseThrow().getId())
                        .add());
        }
        parameters.getTapRange(instant).ifPresent(
            range -> pstAdder.newTapRange().withRangeType(range.rangeType())
                .withMinTap(range.min()).withMaxTap(range.max())
                .add());

        pstAdder.add();
    }

}
