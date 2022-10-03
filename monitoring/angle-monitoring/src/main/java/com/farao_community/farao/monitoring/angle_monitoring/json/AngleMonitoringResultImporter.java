/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring.json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoringResultImporter {
    public AngleMonitoringResult importAngleMonitoringResult(InputStream inputStream, Crac crac) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(AngleMonitoringResult.class, new AngleMonitoringResultDeserializer(crac));
            objectMapper.registerModule(module);
            return objectMapper.readValue(inputStream, AngleMonitoringResult.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
