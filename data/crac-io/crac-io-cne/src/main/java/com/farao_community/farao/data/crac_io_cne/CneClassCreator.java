/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Unit;
import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.DIRECT_POSITIVE_FLOW_IN;
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
        timeSeries.setMRID(generateRandomMRID());
        timeSeries.setBusinessType(businessType);
        timeSeries.setCurveType(curveType);
        timeSeries.period = Collections.singletonList(period);

        return timeSeries;
    }

    /*****************
     REASON
     *****************/
    public static Reason newReason(String code, String text) {
        Reason reason = new Reason();
        reason.setCode(code);
        reason.setText(text);

        return reason;
    }

    /*****************
     CONSTRAINT SERIES
     *****************/
    public static ConstraintSeries newConstraintSeries(String businessType) {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(generateRandomMRID());
        constraintSeries.setBusinessType(businessType);

        return constraintSeries;
    }

    public static ConstraintSeries newConstraintSeries(String businessType, ContingencySeries contingencySeries) {
        ConstraintSeries constraintSeries = newConstraintSeries(businessType);
        constraintSeries.contingencySeries = Collections.singletonList(contingencySeries);

        return constraintSeries;
    }

    /*****************
     CONTINGENCY SERIES
     *****************/
    public static ContingencySeries newContingencySeries(String id, String name) {
        ContingencySeries contingencySeries = new ContingencySeries();
        contingencySeries.setMRID(id);
        contingencySeries.setName(name);

        return contingencySeries;
    }

    /*****************
     MONITORED SERIES
     *****************/
    public static MonitoredSeries newMonitoredSeries(String id, String name) {
        MonitoredSeries monitoredSeries = new MonitoredSeries();
        monitoredSeries.setMRID(id);
        monitoredSeries.setName(name);

        return monitoredSeries;
    }

    public static MonitoredSeries newMonitoredSeries(String id, String name, MonitoredRegisteredResource monitoredRegisteredResource) {
        MonitoredSeries monitoredSeries = newMonitoredSeries(id, name);
        monitoredSeries.registeredResource = Collections.singletonList(monitoredRegisteredResource);

        return monitoredSeries;
    }

    /*****************
     MONITORED REGISTERED RESOURCE
     *****************/
    public static MonitoredRegisteredResource newMonitoredRegisteredResource(String id, String name, String inAggregateNodeMRID, String outAggregateNodeMRID) {
        MonitoredRegisteredResource monitoredRegisteredResource = new MonitoredRegisteredResource();
        monitoredRegisteredResource.setMRID(createResourceIDString(A02_CODING_SCHEME, id));
        monitoredRegisteredResource.setName(name);
        monitoredRegisteredResource.setInAggregateNodeMRID(createResourceIDString(A02_CODING_SCHEME, inAggregateNodeMRID));
        monitoredRegisteredResource.setOutAggregateNodeMRID(createResourceIDString(A02_CODING_SCHEME, outAggregateNodeMRID));

        return monitoredRegisteredResource;
    }

    public static MonitoredRegisteredResource newMonitoredRegisteredResource(String id, String name, String inAggregateNodeMRID, String outAggregateNodeMRID, List<Analog> measurementsList) {
        MonitoredRegisteredResource monitoredRegisteredResource = newMonitoredRegisteredResource(id, name, inAggregateNodeMRID, outAggregateNodeMRID);
        monitoredRegisteredResource.measurements = measurementsList;

        return monitoredRegisteredResource;
    }

    /*****************
     MEASUREMENT
     *****************/
    public static Analog newMeasurement(String measurementType, Unit unit, double flow) {
        Analog measurement = new Analog();
        measurement.setMeasurementType(measurementType);

        if (unit.equals(Unit.MEGAWATT)) {
            measurement.setUnitSymbol(MAW_UNIT_SYMBOL);
        } else if (unit.equals(Unit.AMPERE)) {
            measurement.setUnitSymbol(AMP_UNIT_SYMBOL);
        } else {
            throw new FaraoException(String.format("Unhandled unit %s", unit.toString()));
        }

        if (flow < 0) {
            measurement.setPositiveFlowIn(OPPOSITE_POSITIVE_FLOW_IN);
        } else {
            measurement.setPositiveFlowIn(DIRECT_POSITIVE_FLOW_IN);
        }
        measurement.setAnalogValuesValue((float) Math.round(Math.abs(flow)));

        return measurement;
    }

    /*****************
     REMEDIAL ACTION SERIES
     *****************/
    public static RemedialActionSeries newRemedialActionSeries(String id, String name, String marketObjectStatus) {
        RemedialActionSeries remedialActionSeries = new RemedialActionSeries();
        remedialActionSeries.setMRID(id);
        remedialActionSeries.setName(name);
        remedialActionSeries.setApplicationModeMarketObjectStatusStatus(marketObjectStatus);

        return remedialActionSeries;
    }

    /*****************
     REMEDIAL ACTION REGISTERED RESOURCE
     *****************/
    public static RemedialActionRegisteredResource newRemedialActionRegisteredResource(String id, String name, String psrType, int setpoint, String unitSymbol, String marketObjectStatus) {
        RemedialActionRegisteredResource remedialActionRegisteredResource = new RemedialActionRegisteredResource();
        remedialActionRegisteredResource.setMRID(createResourceIDString(A01_CODING_SCHEME, id));
        remedialActionRegisteredResource.setName(name);
        remedialActionRegisteredResource.setPSRTypePsrType(psrType);
        remedialActionRegisteredResource.setResourceCapacityDefaultCapacity(BigDecimal.valueOf(setpoint));
        remedialActionRegisteredResource.setResourceCapacityUnitSymbol(unitSymbol);
        remedialActionRegisteredResource.setMarketObjectStatusStatus(marketObjectStatus);
        return remedialActionRegisteredResource;
    }

}
