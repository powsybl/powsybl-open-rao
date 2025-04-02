/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */

//TODO change name of class to SearchTreeIcsImporterParameters ?
public class IcsImporterParameters {
    static final double DEFAULT_COST_UP = 10;
    static final double DEFAULT_COST_DOWN = 10;
    private double costUp = DEFAULT_COST_UP;
    private double costDown = DEFAULT_COST_DOWN;

    public double getCostUp() {
        return costUp;
    }

    public void setCostUp(double costUp) {
        this.costUp = costUp;
    }

    public double getCostDown() {
        return costDown;
    }

    public void setCostDown(double costDown) {
        this.costDown = costDown;
    }

    public static Optional<IcsImporterParameters> load(PlatformConfig platformConfig) {

        Objects.requireNonNull(platformConfig);

        return platformConfig.getOptionalModuleConfig(ICS_IMPORTER_PARAMETERS)
            .map(config -> {
                IcsImporterParameters parameters = new IcsImporterParameters();
                parameters.setCostUp(config.getDoubleProperty(COST_UP, IcsImporterParameters.DEFAULT_COST_UP));
                parameters.setCostDown(config.getDoubleProperty(COST_DOWN, IcsImporterParameters.DEFAULT_COST_DOWN));
                return parameters;
            });

    }
}
