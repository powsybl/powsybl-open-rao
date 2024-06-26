/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.io.Importer;
import com.powsybl.openrao.data.craciojson.deserializers.CracDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(Importer.class)
public class JsonImport implements Importer {
    @Override
    public String getFormat() {
        return "JSON";
    }

    @Override
    public boolean exists(InputStream inputStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(Crac.class, new CracDeserializer());
            objectMapper.registerModule(module);
            objectMapper.readValue(inputStream, Crac.class);
            return true;
        } catch (OpenRaoException | IOException e) {
            return false;
        }
    }

    @Override
    public Crac importData(InputStream inputStream, CracFactory cracFactory, Network network) {
        if (network == null) {
            throw new OpenRaoException("Network object is null but it is needed to map contingency's elements");
        }
        try {
            ObjectMapper objectMapper = createObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(Crac.class, new CracDeserializer(cracFactory, network));
            objectMapper.registerModule(module);
            return objectMapper.readValue(inputStream, Crac.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
