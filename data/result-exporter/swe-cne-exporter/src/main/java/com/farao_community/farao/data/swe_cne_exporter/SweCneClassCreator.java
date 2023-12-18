/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.swe_cne_exporter;

import com.powsybl.open_rao.data.swe_cne_exporter.xsd.Point;
import com.powsybl.open_rao.data.swe_cne_exporter.xsd.SeriesPeriod;
import com.powsybl.open_rao.data.swe_cne_exporter.xsd.TimeSeries;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.time.OffsetDateTime;

import static com.powsybl.open_rao.data.swe_cne_exporter.SweCneUtil.createEsmpDateTimeInterval;

/**
 * Creates the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class SweCneClassCreator {

    private SweCneClassCreator() {
    }

    public static Point newPoint(int position) {
        Point point = new Point();
        point.setPosition(position);

        return point;
    }

    public static SeriesPeriod newPeriod(OffsetDateTime offsetDateTime, String duration, Point point) throws DatatypeConfigurationException {
        SeriesPeriod period = new SeriesPeriod();
        period.setTimeInterval(createEsmpDateTimeInterval(offsetDateTime));
        period.setResolution(DatatypeFactory.newInstance().newDuration(duration));
        period.getPoint().add(point);
        return period;
    }

    public static TimeSeries newTimeSeries(String businessType, String curveType, SeriesPeriod period) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setMRID("CNE_RAO_CASTOR-TimeSeries-1");
        timeSeries.setBusinessType(businessType);
        timeSeries.setCurveType(curveType);
        timeSeries.getPeriod().add(period);
        return timeSeries;
    }
}
