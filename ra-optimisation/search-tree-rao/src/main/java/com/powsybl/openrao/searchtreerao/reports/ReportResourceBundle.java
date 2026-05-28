/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.google.auto.service.AutoService;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@AutoService(com.powsybl.commons.report.ReportResourceBundle.class)
public class ReportResourceBundle implements com.powsybl.commons.report.ReportResourceBundle {

    public static final String BASE_NAME = "com.powsybl.openrao.searchtreerao.reports";

    @Override
    public String getBaseName() {
        return BASE_NAME;
    }
}
