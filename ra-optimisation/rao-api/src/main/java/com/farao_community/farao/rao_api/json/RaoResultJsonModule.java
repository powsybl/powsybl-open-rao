/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.rao_api.RaoResult;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RaoResultJsonModule  extends SimpleModule {

    public RaoResultJsonModule() {
        addDeserializer(RaoResult.class, new RaoResultDeserializer());
        addSerializer(RaoResult.class, new RaoResultSerializer());
    }
}