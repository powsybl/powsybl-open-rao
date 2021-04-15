/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_io_json.serializers.ContingencySerializer;
import com.farao_community.farao.data.crac_io_json.serializers.CracSerializer;
import com.farao_community.farao.data.crac_io_json.serializers.NetworkElementSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImplJsonModule extends SimpleModule {

    public CracImplJsonModule() {
        super();
        this.addSerializer(Crac.class, new CracSerializer());
        this.addSerializer(Contingency.class, new ContingencySerializer());
        this.addSerializer(NetworkElement.class, new NetworkElementSerializer());
    }
}
