/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.fbconstraint.parameters;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;

import java.time.OffsetDateTime;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class FbConstraintCracCreationParameters extends AbstractExtension<CracCreationParameters> {
    private static final double DEFAULT_ICS_COST_UP = 10.0;
    private static final double DEFAULT_ICS_COST_DOWN = 10.0;

    private OffsetDateTime timestamp;
    private double icsCostUp = DEFAULT_ICS_COST_UP;
    private double icsCostDown = DEFAULT_ICS_COST_DOWN;

    @Override
    public String getName() {
        return "FbConstraintCracCreatorParameters";
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setIcsCostUp(double icsCostUp) {
        this.icsCostUp = icsCostUp;
    }

    public double getIcsCostUp() {
        return icsCostUp;
    }

    public void setIcsCostDown(double icsCostDown) {
        this.icsCostDown = icsCostDown;
    }

    public double getIcsCostDown() {
        return icsCostDown;
    }
}
