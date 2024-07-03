/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.api.parameters;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.commons.report.ReportNode;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreationParametersJsonModule extends SimpleModule {

    public CracCreationParametersJsonModule(ReportNode reportNode) {
        addDeserializer(CracCreationParameters.class, new CracCreationParametersDeserializer(reportNode));
        addSerializer(CracCreationParameters.class, new CracCreationParametersSerializer());
    }
}
