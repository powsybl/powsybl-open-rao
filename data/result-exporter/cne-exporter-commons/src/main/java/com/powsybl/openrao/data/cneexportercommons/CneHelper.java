/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cneexportercommons;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.raoresultapi.RaoResult;

import java.util.Properties;

import static com.powsybl.openrao.data.cneexportercommons.CneConstants.PATL_MEASUREMENT_TYPE;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.TATL_MEASUREMENT_TYPE;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CneHelper {

    private Crac crac;
    private boolean relativePositiveMargins;
    private boolean withLoopflows;
    private final RaoResult raoResult;
    private final CneExporterParameters exporterParameters;
    private final double mnecAcceptableMarginDiminution;

    public CneHelper(Crac crac, RaoResult raoResult, Properties properties, CneExporterParameters exporterParameters) {
        this.crac = crac;
        this.raoResult = raoResult;
        this.exporterParameters = exporterParameters;

        relativePositiveMargins = Boolean.parseBoolean((String) properties.getOrDefault("relative-positive-margins", "false"));
        withLoopflows = Boolean.parseBoolean((String) properties.getOrDefault("with-loop-flows", "false"));
        mnecAcceptableMarginDiminution = Double.parseDouble((String) properties.getOrDefault("mnec-acceptable-margin-diminution", "0"));
    }

    public RaoResult getRaoResult() {
        return raoResult;
    }

    public boolean isWithLoopflows() {
        return withLoopflows;
    }

    public double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    public Crac getCrac() {
        return crac;
    }

    public String instantToCodeConverter(Instant instant) {
        if (instant.isPreventive()) { // Before contingency
            return PATL_MEASUREMENT_TYPE;
        } else { // After contingency, before any post-contingency RA
            return TATL_MEASUREMENT_TYPE;
        }
    }

    public CneExporterParameters getExporterParameters() {
        return exporterParameters;
    }

    public boolean isRelativePositiveMargins() {
        return relativePositiveMargins;
    }
}
