/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface RaoMetadata extends Extension<RaoResult> {
    Optional<OffsetDateTime> getComputationStart();

    void setComputationStart(OffsetDateTime computationStart);

    Optional<OffsetDateTime> getComputationEnd();

    void setComputationEnd(OffsetDateTime computationEnd);

    default Optional<Duration> getComputationDuration() {
        Optional<OffsetDateTime> start = getComputationStart();
        Optional<OffsetDateTime> end = getComputationEnd();
        if (start.isPresent() && end.isPresent()) {
            return Optional.of(Duration.between(start.orElseThrow(), end.orElseThrow()));
        }
        return Optional.empty();
    }

    String getExecutionDetails();

    void setExecutionDetails(String executionDetails);
}
