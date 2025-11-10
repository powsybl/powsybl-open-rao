/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda.scenariorepository;

import com.powsybl.action.GeneratorActionBuilder;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;

import java.time.OffsetDateTime;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class GeneratorTargetPNetworkVariation extends AbstractNetworkVariation implements NetworkVariation {

    public GeneratorTargetPNetworkVariation(String id, String networkElementId, TemporalData<Double> values) {
        super(id, networkElementId, values);
    }

    @Override
    public TemporalData<Double> computeShifts(TemporalData<Network> networks) {
        return inspect(networks, false);
    }

    @Override
    public TemporalData<Double> apply(TemporalData<Network> networks) {
        return inspect(networks, true);
    }

    @Override
    public void apply(Network network, OffsetDateTime timestamp) {
        new GeneratorActionBuilder()
            .withId("id")
            .withGeneratorId(networkElementId)
            .withActivePowerRelativeValue(false)
            .withActivePowerValue(values.getData(timestamp).orElseThrow())
            .build()
            .toModification()
            .apply(network, true, ReportNode.NO_OP);
    }

    private TemporalData<Double> inspect(TemporalData<Network> networks, boolean apply) {
        if (!networks.getTimestamps().equals(values.getTimestamps())) {
            throw new OpenRaoException("Networks and values have different timestamps");
        }
        TemporalData<Double> shifts = new TemporalDataImpl<>();
        for (OffsetDateTime ts : networks.getTimestamps()) {
            Network network = networks.getData(ts).orElseThrow();
            Generator generator = network.getGenerator(networkElementId);
            if (generator == null) {
                throw new OpenRaoException(String.format("Network at timestamp %s does not contain generator %s.", ts, networkElementId));
            }
            shifts.put(ts, values.getData(ts).orElseThrow() - generator.getTargetP());
            if (apply) {
                apply(network, ts);
            }
        }
        return shifts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return this.id.equals(((GeneratorTargetPNetworkVariation) o).id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
