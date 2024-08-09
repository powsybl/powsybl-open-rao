/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.fbconstraint;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class ComplexVariantCreationContext extends StandardElementaryCreationContext {
    ComplexVariantCreationContext(String complexVariantId, ImportStatus importStatus, String createdRaId, String importStatusDetail) {
        this.nativeObjectId = complexVariantId;
        this.importStatus = importStatus;
        this.createdObjectId = createdRaId;
        this.importStatusDetail = importStatusDetail;
        this.isAltered = false;
    }
}
