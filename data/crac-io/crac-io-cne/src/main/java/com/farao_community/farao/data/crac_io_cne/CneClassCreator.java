/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.Collections;

import static com.farao_community.farao.data.crac_io_cne.CneUtil.*;

/**
 * Creates the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneClassCreator {

    private CneClassCreator() { }

    /*****************
     POINT
     *****************/
    public static Point newPoint(int position) {
        Point point = new Point();
        point.setPosition(position);

        return point;
    }

    /*****************
     PERIOD
     *****************/
    public static SeriesPeriod newPeriod(DateTime networkDate, String duration, Point point) throws DatatypeConfigurationException {
        SeriesPeriod period = new SeriesPeriod();
        period.setTimeInterval(createEsmpDateTimeInterval(networkDate));
        period.setResolution(DatatypeFactory.newInstance().newDuration(duration));
        period.point = Collections.singletonList(point);

        return period;
    }

    /*****************
     TIME SERIES
     *****************/
    public static TimeSeries newTimeSeries(String businessType, String curveType, SeriesPeriod period) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setMRID(cutString(generateRandomMRID(), 60));
        timeSeries.setBusinessType(businessType);
        timeSeries.setCurveType(curveType);
        timeSeries.period = Collections.singletonList(period);

        return timeSeries;
    }
}
