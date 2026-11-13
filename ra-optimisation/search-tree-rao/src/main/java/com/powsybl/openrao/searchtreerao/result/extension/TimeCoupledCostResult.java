/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.extension;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class TimeCoupledCostResult extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "time-coupled-cost-results";

    private final TemporalData<CostResult> costResults;

    private TimeCoupledCostResult(TemporalData<CostResult> costResults) {
        this.costResults = new TemporalDataImpl<>();
        costResults.getDataPerTimestamp().forEach(this.costResults::put);
    }

    public List<OffsetDateTime> getTimestamps() {
        return costResults.getTimestamps();
    }

    public CostResult getCostResult(OffsetDateTime timestamp) {
        return costResults.getData(timestamp).orElseThrow(
            () -> new OpenRaoException("No cost result found for timestamp " + DateTimeFormatter.ISO_DATE_TIME.format(timestamp))
        );
    }

    /**
     * It gives the global cost of the situation at a given {@link Instant} according to the objective
     * function defined in the RAO.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param timestamp        The timestamp of the optimized instant to be studied.
     * @return The global cost of the situation state.
     */
    public double getCost(Instant optimizedInstant, OffsetDateTime timestamp) {
        return costResults.getData(timestamp).orElseThrow().getCost(optimizedInstant);
    }

    /**
     * It gives the functional cost of the situation at a given {@link Instant} according to the objective
     * function defined in the RAO. It represents the main part of the objective function.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param timestamp        The timestamp of the optimized instant to be studied.
     * @return The functional cost of the situation state.
     */
    public double getFunctionalCost(Instant optimizedInstant, OffsetDateTime timestamp) {
        return costResults.getData(timestamp).orElseThrow().getFunctionalCost(optimizedInstant);
    }

    /**
     * It gives the sum of virtual costs of the situation at a given {@link Instant} according to the
     * objective function defined in the RAO. It represents the secondary parts of the objective
     * function.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param timestamp        The timestamp of the optimized instant to be studied.
     * @return The global virtual cost of the situation state.
     */
    public double getVirtualCost(Instant optimizedInstant, OffsetDateTime timestamp) {
        return costResults.getData(timestamp).orElseThrow().getVirtualCost(optimizedInstant);
    }

    /**
     * It gives the names of the different virtual costs implied in the objective function defined in
     * the RAO.
     *
     * @return The set of virtual cost names.
     */
    public Set<String> getVirtualCostNames() {
        Set<String> allVirtualCosts = new HashSet<>();
        costResults.getDataPerTimestamp()
            .values()
            .forEach(costResult -> allVirtualCosts.addAll(costResult.getVirtualCostNames()));
        return allVirtualCosts;
    }

    /**
     * It gives the names of the different virtual costs implied in the objective function defined in
     * the RAO.
     *
     * @param timestamp The timestamp for which to retrieve the virtual cost names.
     * @return The set of virtual cost names.
     */
    public Set<String> getVirtualCostNames(OffsetDateTime timestamp) {
        return costResults.getData(timestamp).orElseThrow().getVirtualCostNames();
    }

    /**
     * It gives the specified virtual cost of the situation at a given {@link Instant}. It represents the
     * secondary parts of the objective. If the specified name is not part of the virtual costs defined in the
     * objective function, this method will return {@code Double.NaN} values.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param virtualCostName  The name of the virtual cost.
     * @param timestamp        The timestamp of the optimized instant to be studied.
     * @return The specific virtual cost of the situation state.
     */
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName, OffsetDateTime timestamp) {
        return costResults.getData(timestamp).orElseThrow().getVirtualCost(optimizedInstant, virtualCostName);
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    public static TimeCoupledCostResultBuilder builder() {
        return new TimeCoupledCostResultBuilder();
    }

    public static final class TimeCoupledCostResultBuilder {
        TemporalData<CostResult> costResults;

        private TimeCoupledCostResultBuilder() {
            this.costResults = new TemporalDataImpl<>();
        }

        public TimeCoupledCostResultBuilder addCostResult(OffsetDateTime timestamp, CostResult costResult) {
            costResults.put(timestamp, costResult);
            return this;
        }

        public TimeCoupledCostResult build() {
            return new TimeCoupledCostResult(costResults);
        }
    }
}
