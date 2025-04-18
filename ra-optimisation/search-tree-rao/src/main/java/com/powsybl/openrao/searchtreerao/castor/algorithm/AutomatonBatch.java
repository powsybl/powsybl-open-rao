/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AutomatonBatch implements Comparable<AutomatonBatch> {
    private final int timeAfterOutage;
    private final Set<NetworkAction> topologicalAutomatons;
    private final Set<RangeAction<?>> rangeAutomatons;

    public AutomatonBatch(int timeAfterOutage) {
        this.timeAfterOutage = timeAfterOutage;
        this.topologicalAutomatons = new HashSet<>();
        this.rangeAutomatons = new HashSet<>();
    }

    public int getTimeAfterOutage() {
        return timeAfterOutage;
    }

    public Set<NetworkAction> getTopologicalAutomatons() {
        return topologicalAutomatons;
    }

    public Set<RangeAction<?>> getRangeAutomatons() {
        return rangeAutomatons;
    }

    public void add(RemedialAction<?> automaton) {
        // TODO: default speed is 0, see if we want to keep this
        if (automaton.getSpeed().orElse(0) != timeAfterOutage) {
            throw new OpenRaoException("The speed of automaton %s is inconsistent with the automaton batch speed (%s).".formatted(automaton.getId(), timeAfterOutage));
        }
        if (automaton instanceof NetworkAction topologicalAutomaton) {
            topologicalAutomatons.add(topologicalAutomaton);
        } else if (automaton instanceof RangeAction<?> rangeAutomaton) {
            rangeAutomatons.add(rangeAutomaton);
        }
    }

    public OptimizationResult simulate(Network network) {
        return null;
    }

    @Override
    public int compareTo(AutomatonBatch automatonBatch) {
        return Integer.compare(timeAfterOutage, automatonBatch.getTimeAfterOutage());
    }
}
