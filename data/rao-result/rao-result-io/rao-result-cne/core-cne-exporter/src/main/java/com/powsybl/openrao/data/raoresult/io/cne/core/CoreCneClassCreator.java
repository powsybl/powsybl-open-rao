/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.core;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TsoEICode;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.*;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil.cutString;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil.limitFloatInterval;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneUtil.*;

/**
 * Creates the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CoreCneClassCreator {

    private CoreCneClassCreator() {
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

    public static ConstraintSeries newConstraintSeries(String id, String businessType) {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(id);
        constraintSeries.setBusinessType(businessType);
        return constraintSeries;
    }

    public static ConstraintSeries newConstraintSeries(String id, String businessType, String optimStatus) {
        ConstraintSeries constraintSeries = newConstraintSeries(id, businessType);
        constraintSeries.setOptimizationMarketObjectStatusStatus(optimStatus);

        return constraintSeries;
    }

    public static ConstraintSeries newConstraintSeries(String id, String businessType, String operator, String optimStatus) {
        ConstraintSeries constraintSeries = newConstraintSeries(id, businessType, optimStatus);
        if (!Objects.isNull(operator)) {
            constraintSeries.getPartyMarketParticipant().add(newPartyMarketParticipant(TsoEICode.fromShortId(operator).getEICode()));
        }
        return constraintSeries;
    }

    public static PartyMarketParticipant newPartyMarketParticipant(String eiCode) {
        PartyMarketParticipant partyMarketParticipant = new PartyMarketParticipant();
        partyMarketParticipant.setMRID(createPartyIDString("A01", eiCode));
        return partyMarketParticipant;
    }

    public static ContingencySeries newContingencySeries(String id, String name) {
        ContingencySeries contingencySeries = new ContingencySeries();
        contingencySeries.setMRID(id);
        contingencySeries.setName(name);

        return contingencySeries;
    }

    public static MonitoredSeries newMonitoredSeries(String id, String name) {
        MonitoredSeries monitoredSeries = new MonitoredSeries();
        monitoredSeries.setMRID(id);
        monitoredSeries.setName(name);

        return monitoredSeries;
    }

    public static MonitoredSeries newMonitoredSeries(String id, String name, MonitoredRegisteredResource monitoredRegisteredResource) {
        MonitoredSeries monitoredSeries = newMonitoredSeries(id, name);
        monitoredSeries.getRegisteredResource().add(monitoredRegisteredResource);

        return monitoredSeries;
    }

    public static MonitoredRegisteredResource newMonitoredRegisteredResource(String id, String name) {
        MonitoredRegisteredResource monitoredRegisteredResource = new MonitoredRegisteredResource();
        monitoredRegisteredResource.setMRID(createResourceIDString(A02_CODING_SCHEME, id));
        monitoredRegisteredResource.setName(name);

        return monitoredRegisteredResource;
    }

    public static MonitoredRegisteredResource newMonitoredRegisteredResource(String id, String name, List<Analog> measurementsList) {
        MonitoredRegisteredResource monitoredRegisteredResource = newMonitoredRegisteredResource(id, name);
        monitoredRegisteredResource.getMeasurements().addAll(measurementsList);

        return monitoredRegisteredResource;
    }

    public static Analog newFlowMeasurement(String measurementType, Unit unit, double flow) {
        Analog measurement = new Analog();
        measurement.setMeasurementType(measurementType);

        if (unit.equals(Unit.MEGAWATT)) {
            measurement.setUnitSymbol(MAW_UNIT_SYMBOL);
        } else if (unit.equals(Unit.AMPERE)) {
            measurement.setUnitSymbol(AMP_UNIT_SYMBOL);
        } else {
            throw new OpenRaoException(String.format("Unhandled unit %s", unit));
        }

        if (flow < 0) {
            measurement.setPositiveFlowIn(OPPOSITE_POSITIVE_FLOW_IN);
        } else {
            measurement.setPositiveFlowIn(DIRECT_POSITIVE_FLOW_IN);
        }
        measurement.setAnalogValuesValue(limitFloatInterval(flow));

        return measurement;
    }

    public static Analog newPtdfMeasurement(String measurementType, double value) {
        Analog measurement = new Analog();
        measurement.setMeasurementType(measurementType);
        measurement.setUnitSymbol(DIMENSIONLESS_SYMBOL);
        measurement.setAnalogValuesValue((float) (Math.round(value * 1e5) / 1e5));
        return measurement;
    }

    public static RemedialActionSeries newRemedialActionSeries(String id, String name, String marketObjectStatus) {
        RemedialActionSeries remedialActionSeries = new RemedialActionSeries();
        remedialActionSeries.setMRID(cutString(id, 60));
        remedialActionSeries.setName(name);
        if (marketObjectStatus != null) {
            remedialActionSeries.setApplicationModeMarketObjectStatusStatus(marketObjectStatus);
        }

        return remedialActionSeries;
    }

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
