/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.NetworkActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;

import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneUtil {

    private CneUtil() { }

    // Creation of time interval
    public static ESMPDateTimeInterval createEsmpDateTimeInterval(DateTime networkDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));

        ESMPDateTimeInterval timeInterval = new ESMPDateTimeInterval();

        timeInterval.setStart(dateFormat.format(networkDate.toDate()));
        timeInterval.setEnd(dateFormat.format(networkDate.plusHours(1).toDate()));
        return timeInterval;
    }

    // Creation of current date
    public static XMLGregorianCalendar createXMLGregorianCalendarNow() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        try {
            XMLGregorianCalendar xmlcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(dateFormat.format(new Date()));
            xmlcal.setTimezone(0);

            return xmlcal;
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException();
        }
    }

    // Creation of ID with code scheme
    public static AreaIDString createAreaIDString(String codingScheme, String value) {
        AreaIDString domainMRID = new AreaIDString();
        domainMRID.setCodingScheme(codingScheme);
        domainMRID.setValue(value);
        return domainMRID;
    }

    // Creation of ID with code scheme
    public static PartyIDString createPartyIDString(String codingScheme, String value) {
        PartyIDString marketParticipantMRID = new PartyIDString();
        marketParticipantMRID.setCodingScheme(codingScheme);
        marketParticipantMRID.setValue(value);
        return marketParticipantMRID;
    }

    // Creation of ID with code scheme
    public static ResourceIDString createResourceIDString(String codingScheme, String value) {
        ResourceIDString resourceMRID = new ResourceIDString();
        resourceMRID.setCodingScheme(codingScheme);
        resourceMRID.setValue(value);
        return resourceMRID;
    }

    // Generates a random code with 35 characters
    public static String generateRandomMRID() {
        Random random = new SecureRandom();
        return String.format("%s-%s-%s-%s", Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()));
    }

    public static String createRangeActionId(String id, double setpoint) {
        return String.format("%s@%s@", id, setpoint);
    }

    // Creates any measurement
    public static Analog createMeasurement(String measurementType, Unit unit, double flow) {
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
        measurement.setAnalogValuesValue(Math.round(Math.abs(flow)));

        return measurement;
    }

    /**
     * Returns the list of Preventive Remedial Actions activated
     */
    private static List<RemedialAction<?>> getListOfPra(Crac crac, String preOptimVariantId, String postOptimVariantId) {
        List<RemedialAction<?>> pras = new ArrayList<>();
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            RangeActionResultExtension rangeActionResultExtension = rangeAction.getExtension(RangeActionResultExtension.class);

            if (rangeActionResultExtension.getVariant(postOptimVariantId).isActivated(preventiveState) &&
                rangeActionResultExtension.getVariant(postOptimVariantId).getSetPoint(preventiveState) != rangeActionResultExtension.getVariant(preOptimVariantId).getSetPoint(preventiveState)) {
                pras.add(rangeAction);
            }
        }
        for (NetworkAction networkAction : crac.getNetworkActions()) {
            NetworkActionResultExtension networkActionResultExtension = networkAction.getExtension(NetworkActionResultExtension.class);
            if (networkActionResultExtension.getVariant(postOptimVariantId).isActivated(preventiveState)) {
                pras.add(networkAction);
            }
        }
        return pras;
    }

    /**
     * Returns the number of Preventive Remedial Actions activated
     */
    public static int getNumberOfPra(Crac crac, String preOptimVariantId, String postOptimVariantId) {
        return getListOfPra(crac, preOptimVariantId, postOptimVariantId).size();
    }
}
