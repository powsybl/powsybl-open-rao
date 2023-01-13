/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.cne_exporter_commons.TsoEICode;
import com.farao_community.farao.data.swe_cne_exporter.xsd.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.data.cne_exporter_commons.CneConstants.*;
import static com.farao_community.farao.data.cne_exporter_commons.CneUtil.cutString;
import static com.farao_community.farao.data.cne_exporter_commons.CneUtil.limitFloatInterval;
import static com.farao_community.farao.data.swe_cne_exporter.SweCneUtil.*;

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
