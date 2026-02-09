/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.parameters.AbstractAlignedRaCracCreationParameters;

import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkCracCreationParameters extends AbstractAlignedRaCracCreationParameters {

    private SortedMap<InstantKind, List<String>> instants = new TreeMap<>(Map.of(
        InstantKind.PREVENTIVE, List.of("preventive"),
        InstantKind.OUTAGE, List.of("outage"),
        InstantKind.CURATIVE, List.of("curative")
    ));
    private CriticalElements criticalElements = new CriticalElements();
    private Contingencies contingencies = new Contingencies();
    private PstRangeActions pstRangeActions = new PstRangeActions();
    private RedispatchingRangeActions redispatchingRangeActions = new RedispatchingRangeActions();
    private CountertradingRangeActions countertradingRangeActions = new CountertradingRangeActions();
    private BalancingRangeAction balancingRangeAction = new BalancingRangeAction();

    @Override
    public String getName() {
        return "NetworkCracCreationParameters";
    }

    public SortedMap<InstantKind, List<String>> getInstants() {
        return instants;
    }

    public void setInstants(SortedMap<InstantKind, List<String>> instants) {
        // TODO verify 1 element for preventive, outage, etc. also that they are in correct order.
        this.instants = instants;
    }

    public Contingencies getContingencies() {
        return contingencies;
    }

    public CriticalElements getCriticalElements() {
        return criticalElements;
    }

    public PstRangeActions getPstRangeActions() {
        return pstRangeActions;
    }

    public RedispatchingRangeActions getRedispatchingRangeActions() {
        return redispatchingRangeActions;
    }

    public CountertradingRangeActions getCountertradingRangeActions() {
        return countertradingRangeActions;
    }

    public BalancingRangeAction getBalancingRangeAction() {
        return balancingRangeAction;
    }
}
