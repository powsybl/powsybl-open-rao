/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.swe_cne_exporter.xsd.*;
import org.threeten.extra.Interval;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.farao_community.farao.data.cne_exporter_commons.CneUtil.cutString;

/**
 * Generates MonitoredSeries for SWE CNE format
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SweMonitoredSeriesCreator {
    private final CneHelper cneHelper;

    public SweMonitoredSeriesCreator(CneHelper cneHelper) {
        this.cneHelper = cneHelper;
    }

    public List<MonitoredSeries> generateMonitoredSeries(Contingency contingency) {
        return new ArrayList<>();
    }
}
