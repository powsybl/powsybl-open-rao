/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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

    private OffsetDateTime timestamp;

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
}
