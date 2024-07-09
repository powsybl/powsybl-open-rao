/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.fbconstraint;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;

import java.time.OffsetDateTime;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class FbConstraintCracCreationParameters extends AbstractExtension<CracCreationParameters> {
    private OffsetDateTime offsetDateTime = null;

    @Override
    public String getName() {
        return "FbConstraintCracCreationParameters";
    }

    public OffsetDateTime getOffsetDateTime() {
        return offsetDateTime;
    }

    public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
        this.offsetDateTime = offsetDateTime;
    }
}
