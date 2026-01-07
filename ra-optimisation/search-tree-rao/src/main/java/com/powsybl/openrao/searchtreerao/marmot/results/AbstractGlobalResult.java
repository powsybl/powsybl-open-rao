/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractGlobalResult<T> implements GlobalResult<T> {
    protected final TemporalData<T> resultPerTimestamp;

    protected AbstractGlobalResult(TemporalData<? extends T> resultPerTimestamp) {
        this.resultPerTimestamp = new TemporalDataImpl<>(resultPerTimestamp.getDataPerTimestamp());
    }

    @Override
    public List<OffsetDateTime> getTimestamps() {
        return resultPerTimestamp.getTimestamps();
    }

    @Override
    public T getIndividualResult(OffsetDateTime timestamp) {
        return resultPerTimestamp.getData(timestamp).orElseThrow(() -> new OpenRaoException("No individual result exists for the provided timestamp."));
    }
}
