/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.serializers;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultJsonSerializerModule extends SimpleModule {

    public RaoResultJsonSerializerModule(Crac crac, Set<Unit> flowUnits) {
        super();
        this.addSerializer(RaoResult.class, new RaoResultSerializer(crac, flowUnits));
    }
}
