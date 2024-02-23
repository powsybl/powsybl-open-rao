/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.cim.craccreator;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.cnec.MonitoredSeriesCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.contingency.CimContingencyCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cim.xsd.ContingencySeries;
import com.powsybl.openrao.data.craccreation.creator.cim.xsd.MonitoredSeries;
import com.powsybl.openrao.data.craccreation.creator.cim.xsd.TimeSeries;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CimCracUtils {

    private static final String DEFAULT_CURVE_TYPE = "A03";
    private static final DateTimeFormatter CRAC_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX");

    private CimCracUtils() { }

    public static Contingency getContingencyFromCrac(ContingencySeries cimContingency, CimCracCreationContext cracCreationContext) {
        CimContingencyCreationContext ccc = cracCreationContext.getContingencyCreationContextById(cimContingency.getMRID());
        if (ccc == null || !ccc.isImported()) {
            return null;
        }
        return cracCreationContext.getCrac().getContingency(ccc.getCreatedContingencyId());
    }

    public static Set<FlowCnec> getFlowCnecsFromCrac(MonitoredSeries monitoredSeries, CimCracCreationContext cracCreationContext) {
        MonitoredSeriesCreationContext mscc = cracCreationContext.getMonitoredSeriesCreationContext(monitoredSeries.getMRID());
        if (mscc == null) {
            return new HashSet<>();
        }
        return mscc.getCreatedCnecIds().stream().map(cnecId -> cracCreationContext.getCrac().getFlowCnec(cnecId)).collect(Collectors.toSet());
    }

    public static String getCurveTypeFromTimeSeries(TimeSeries cimTimeSerie) {
        String curveType = cimTimeSerie.getCurveType();
        if (StringUtils.isBlank(curveType)) {
            curveType = DEFAULT_CURVE_TYPE;
        }
        if (!curveType.equals("A03") && !curveType.equals("A01")) {
            throw new OpenRaoException("CurveType not supported: " + curveType);
        }
        return curveType;
    }

    public static boolean isTimestampInPeriod(Instant timestamp, Instant periodStart, Instant periodEnd, String curveType, Duration periodResolution, int position) {

        Instant pointStart = periodStart.plus(periodResolution.multipliedBy(position - 1L));
        // default "A03"
        Instant pointEnd = periodEnd;
        if (curveType.equals("A01")) {
            pointEnd = periodStart.plus(periodResolution.multipliedBy(position));
        }

        return pointStart.compareTo(timestamp) <= 0 && pointEnd.isAfter(timestamp);
    }

    public static Instant parseDateTime(String dateTimeStringFromCrac) {
        return OffsetDateTime.parse(dateTimeStringFromCrac, CRAC_DATE_TIME_FORMAT).toInstant();
    }
}
