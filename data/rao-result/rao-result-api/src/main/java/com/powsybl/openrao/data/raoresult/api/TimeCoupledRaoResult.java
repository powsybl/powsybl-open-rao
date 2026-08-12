/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.raoresult.api.extension.CastorCostResult;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipOutputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface TimeCoupledRaoResult extends RaoResult {
    List<OffsetDateTime> getTimestamps();

    default double getGlobalCost(Instant instant) {
        return getGlobalFunctionalCost(instant) + getGlobalVirtualCost(instant);
    }

    default double getGlobalFunctionalCost(Instant instant) {
        CastorCostResult castorCostResult = getExtension(CastorCostResult.class);
        return castorCostResult == null ? Double.NaN : castorCostResult.getFunctionalCost(instant);
    }

    default double getGlobalVirtualCost(Instant instant) {
        CastorCostResult castorCostResult = getExtension(CastorCostResult.class);
        return castorCostResult == null ? Double.NaN : castorCostResult.getVirtualCost(instant);
    }

    default double getGlobalVirtualCost(Instant instant, String virtualCostName) {
        CastorCostResult castorCostResult = getExtension(CastorCostResult.class);
        return castorCostResult == null ? Double.NaN : castorCostResult.getVirtualCost(instant, virtualCostName);
    }

    /**
     * It gives the global cost of the situation at a given {@link Instant} according to the objective
     * function defined in the RAO.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param timestamp        The timestamp to be studied
     * @return The global cost of the situation state.
     */
    default double getCost(Instant optimizedInstant, OffsetDateTime timestamp) {
        return getFunctionalCost(optimizedInstant, timestamp) + getVirtualCost(optimizedInstant, timestamp);
    }

    /**
     * It gives the functional cost of the situation at a given {@link Instant} according to the objective
     * function defined in the RAO. It represents the main part of the objective function.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param timestamp        The timestamp to be studied
     * @return The functional cost of the situation state.
     */
    default double getFunctionalCost(Instant optimizedInstant, OffsetDateTime timestamp) {
        return getIndividualRaoResult(timestamp).getFunctionalCost(optimizedInstant);
    }

    /**
     * It gives the sum of virtual costs of the situation at a given {@link Instant} according to the
     * objective function defined in the RAO. It represents the secondary parts of the objective
     * function.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param timestamp        The timestamp to be studied
     * @return The global virtual cost of the situation state.
     */
    default double getVirtualCost(Instant optimizedInstant, OffsetDateTime timestamp) {
        return getIndividualRaoResult(timestamp).getVirtualCost(optimizedInstant);
    }

    /**
     * It gives the specified virtual cost of the situation at a given {@link Instant}. It represents the
     * secondary parts of the objective. If the specified name is not part of the virtual costs defined in the
     * objective function, this method could return {@code Double.NaN} values.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param virtualCostName  The name of the virtual cost.
     * @param timestamp        The timestamp to be studied
     * @return The specific virtual cost of the situation state.
     */
    default double getVirtualCost(Instant optimizedInstant, String virtualCostName, OffsetDateTime timestamp) {
        return getIndividualRaoResult(timestamp).getVirtualCost(optimizedInstant, virtualCostName);
    }

    RaoResult getIndividualRaoResult(OffsetDateTime timestamp);

    void write(ZipOutputStream zipOutputStream, TemporalData<Crac> cracs, Properties properties) throws IOException;
}
