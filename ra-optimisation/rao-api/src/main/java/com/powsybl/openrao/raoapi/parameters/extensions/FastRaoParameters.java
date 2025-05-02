/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */

public class FastRaoParameters extends AbstractExtension<RaoParameters> {
    @Override
    public String getName() {
        return FAST_RAO_PARAMETERS;
    }

    private static final int DEFAULT_NUMBER_OF_CNECS_TO_ADD = 20;
    private static final boolean DEFAULT_ADD_UNSECURE_CNECS = false;
    private static final double DEFAULT_MARGIN_LIMIT = 5;

    private int getNumberOfCnecsToAdd = DEFAULT_NUMBER_OF_CNECS_TO_ADD;
    private boolean addUnsecureCnecs = DEFAULT_ADD_UNSECURE_CNECS;
    private double marginLimit = DEFAULT_MARGIN_LIMIT;

    public int getNumberOfCnecsToAdd() {
        return getNumberOfCnecsToAdd;
    }

    public void setNumberOfCnecsToAdd(int getNumberOfCnecsToAdd) {
        this.getNumberOfCnecsToAdd = getNumberOfCnecsToAdd;
    }

    public boolean getAddUnsecureCnecs() {
        return addUnsecureCnecs;
    }

    public void setAddUnsecureCnecs(boolean addUnsecureCnecs) {
        this.addUnsecureCnecs = addUnsecureCnecs;
    }

    public double getMarginLimit() {
        return marginLimit;
    }

    public void setMarginLimit(double marginLimit) {
        this.marginLimit = marginLimit;
    }

    public static FastRaoParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        FastRaoParameters parameters = new FastRaoParameters();
        platformConfig.getOptionalModuleConfig(FAST_RAO_PARAMETERS)
            .ifPresent(config -> {
                parameters.setNumberOfCnecsToAdd(config.getIntProperty(NUMBER_OF_CNECS_TO_ADD, DEFAULT_NUMBER_OF_CNECS_TO_ADD));
                parameters.setAddUnsecureCnecs(config.getBooleanProperty(ADD_UNSECURE_CNECS, DEFAULT_ADD_UNSECURE_CNECS));
                parameters.setMarginLimit(config.getDoubleProperty(MARGIN_LIMIT, DEFAULT_MARGIN_LIMIT));

            });
        return parameters;
    }

}
