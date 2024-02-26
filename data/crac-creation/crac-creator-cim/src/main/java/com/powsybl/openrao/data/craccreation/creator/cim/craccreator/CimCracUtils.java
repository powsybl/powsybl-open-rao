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
import com.powsybl.openrao.data.craccreation.creator.cim.xsd.*;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CimCracUtils {

    private static final DateTimeFormatter CRAC_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX");

    private static final Comparator<Point> REVERSE_POINT_COMPARATOR = (new PointPositionComparator()).reversed();

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
            curveType = CimConstants.VARIABLE_SIZED_BLOCK_CURVE;
        }
        if (!curveType.equals(CimConstants.VARIABLE_SIZED_BLOCK_CURVE)
                && !curveType.equals(CimConstants.SEQUENTIAL_FIXED_SIZE_BLOCKS_CURVE_TYPE)) {
            throw new OpenRaoException("CurveType not supported: " + curveType);
        }
        return curveType;
    }

    public static boolean isTimestampInPeriod(Instant timestamp, TimeSeries timeSerie, SeriesPeriod period, int position, Optional<Integer> endPosition) {
        final String curveType = CimCracUtils.getCurveTypeFromTimeSeries(timeSerie);
        final Duration periodResolution = Duration.parse(period.getResolution().toString());
        final Instant periodStart = CimCracUtils.parseDateTime(period.getTimeInterval().getStart());
        final Instant periodEnd = CimCracUtils.parseDateTime(period.getTimeInterval().getEnd());

        Instant pointStart = periodStart.plus(periodResolution.multipliedBy(position - 1L));
        // default "A03"
        Instant pointEnd = periodEnd;
        if (curveType.equals(CimConstants.SEQUENTIAL_FIXED_SIZE_BLOCKS_CURVE_TYPE)) {
            pointEnd = periodStart.plus(periodResolution.multipliedBy(position));
        } else {
            if (endPosition.isPresent()) {
                pointEnd = periodStart.plus(periodResolution.multipliedBy(endPosition.get() - 1L));
            }
        }

        return pointStart.compareTo(timestamp) <= 0 && pointEnd.isAfter(timestamp);
    }

    public static Instant parseDateTime(String dateTimeStringFromCrac) {
        return OffsetDateTime.parse(dateTimeStringFromCrac, CRAC_DATE_TIME_FORMAT).toInstant();
    }

    public static Comparator<Point> getReversePointComparator() {
        return REVERSE_POINT_COMPARATOR;
    }
}
