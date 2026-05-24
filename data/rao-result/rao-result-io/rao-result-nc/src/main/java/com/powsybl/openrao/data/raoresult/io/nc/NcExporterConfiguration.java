/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.nc;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class NcExporterConfiguration {
    private Duration validityDuration = Duration.of(1, ChronoUnit.HOURS);
    private ConfidentialityLevel confidentialityLevel = ConfidentialityLevel.PUBLIC;

    public NcExporterConfiguration() {
    }

    public Duration getValidityDuration() {
        return validityDuration;
    }

    public void setValidityDuration(Duration validityDuration) {
        this.validityDuration = validityDuration;
    }

    public ConfidentialityLevel getConfidentialityLevel() {
        return confidentialityLevel;
    }

    public void setConfidentialityLevel(ConfidentialityLevel confidentialityLevel) {
        this.confidentialityLevel = confidentialityLevel;
    }
}
