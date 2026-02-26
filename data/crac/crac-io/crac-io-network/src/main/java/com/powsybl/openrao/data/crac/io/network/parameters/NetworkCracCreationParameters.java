/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.parameters.AbstractAlignedRaCracCreationParameters;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkCracCreationParameters extends AbstractAlignedRaCracCreationParameters {
    static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private final SortedMap<InstantKind, List<String>> instants;
    private final Contingencies contingencies = new Contingencies();
    private final CriticalElements criticalElements;
    private final PstRangeActions pstRangeActions = new PstRangeActions();
    private final RedispatchingRangeActions redispatchingRangeActions = new RedispatchingRangeActions();
    private final CountertradingRangeActions countertradingRangeActions = new CountertradingRangeActions();
    private final BalancingRangeAction balancingRangeAction = new BalancingRangeAction();

    /**
     * Create the NetworkCracCreationParameters.
     * One "preventive", one "outage" and one "curative" instants will be created.
     * Set the auto & curative instants to create (you can replace by "null").
     */
    public NetworkCracCreationParameters(@Nullable List<String> autoInstants, @Nullable List<String> curativeInstants) {
        TreeMap<InstantKind, List<String>> map = new TreeMap<>(Map.of(
            InstantKind.PREVENTIVE, List.of(PREVENTIVE_INSTANT_ID),
            InstantKind.OUTAGE, List.of(OUTAGE_INSTANT_ID)));
        List<String> allInstants = new ArrayList<>(List.of(PREVENTIVE_INSTANT_ID, OUTAGE_INSTANT_ID));
        if (autoInstants != null) {
            map.put(InstantKind.AUTO, autoInstants);
            allInstants.addAll(autoInstants);
        }
        if (curativeInstants != null) {
            map.put(InstantKind.CURATIVE, curativeInstants);
            allInstants.addAll(curativeInstants);
        }
        checkForIllegalInstantNames(allInstants);
        this.instants = map;
        criticalElements = new CriticalElements(allInstants);
    }

    private void checkForIllegalInstantNames(List<String> instants) {
        if (instants.contains(null) || instants.contains("")) {
            throw new OpenRaoException("All instant names should be non null and not empty.");
        }
        if (instants.stream().anyMatch(instant -> Collections.frequency(instants, instant) > 1)) {
            throw new OpenRaoException(String.format(
                "Instant names must be unique. Names '%s' and '%s' are reserved for the preventive and outage instants.",
                PREVENTIVE_INSTANT_ID, OUTAGE_INSTANT_ID));
        }
    }

    @Override
    public String getName() {
        return "NetworkCracCreationParameters";
    }

    public SortedMap<InstantKind, List<String>> getInstants() {
        return instants;
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
