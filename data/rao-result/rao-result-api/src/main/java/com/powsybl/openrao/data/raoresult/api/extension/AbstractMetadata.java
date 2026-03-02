/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractMetadata extends AbstractExtension<RaoResult> implements RaoMetadata {
    protected OffsetDateTime computationStart;
    protected OffsetDateTime computationEnd;
    protected String executionDetails;

    protected AbstractMetadata() {
    }

    public Optional<OffsetDateTime> getComputationStart() {
        return Optional.ofNullable(computationStart);
    }

    public void setComputationStart(OffsetDateTime computationStart) {
        this.computationStart = computationStart;
    }

    public Optional<OffsetDateTime> getComputationEnd() {
        return Optional.ofNullable(computationEnd);
    }

    public void setComputationEnd(OffsetDateTime computationEnd) {
        this.computationEnd = computationEnd;
    }

    public String getExecutionDetails() {
        return executionDetails;
    }

    public void setExecutionDetails(String executionDetails) {
        this.executionDetails = executionDetails;
    }
}
