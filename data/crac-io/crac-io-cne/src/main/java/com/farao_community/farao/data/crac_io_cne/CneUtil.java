/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;

import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.ABS_MARG_TATL_MEASUREMENT_TYPE;

/**
 * Auxiliary methods
 *
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
        domainMRID.setValue(cutString(value, 18));
        return domainMRID;
    }

    // Creation of ID with code scheme
    public static PartyIDString createPartyIDString(String codingScheme, String value) {
        PartyIDString marketParticipantMRID = new PartyIDString();
        marketParticipantMRID.setCodingScheme(codingScheme);
        marketParticipantMRID.setValue(cutString(value, 16));
        return marketParticipantMRID;
    }

    // Creation of ID with code scheme
    public static ResourceIDString createResourceIDString(String codingScheme, String value) {
        ResourceIDString resourceMRID = new ResourceIDString();
        resourceMRID.setCodingScheme(codingScheme);
        resourceMRID.setValue(cutString(value, 60));
        return resourceMRID;
    }

    // Generates a random code with 35 characters
    public static String generateRandomMRID() {
        Random random = new SecureRandom();
        return String.format("%s-%s-%s-%s", Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()));
    }

    public static String createRangeActionId(String id, int setpoint) {
        return String.format("%s@%s@", id, setpoint);
    }

    /**
     * Returns the list of Preventive Remedial Actions activated
     */
    private static List<RemedialAction<?>> getListOfPra(Crac crac, String preOptimVariantId, String postOptimVariantId) {
        List<RemedialAction<?>> pras = new ArrayList<>();
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            RangeActionResultExtension rangeActionResultExtension = rangeAction.getExtension(RangeActionResultExtension.class);
            if (rangeActionResultExtension.getVariant(preOptimVariantId) != null
                && rangeActionResultExtension.getVariant(postOptimVariantId) != null
                && rangeActionResultExtension.getVariant(preOptimVariantId) instanceof PstRangeResult
                && rangeActionResultExtension.getVariant(postOptimVariantId) instanceof PstRangeResult
                && isActivated(preventiveState, rangeActionResultExtension.getVariant(preOptimVariantId), rangeActionResultExtension.getVariant(postOptimVariantId))) {
                pras.add(rangeAction);
            }
        }
        for (NetworkAction networkAction : crac.getNetworkActions()) {
            NetworkActionResultExtension networkActionResultExtension = networkAction.getExtension(NetworkActionResultExtension.class);
            if (networkActionResultExtension.getVariant(preOptimVariantId) != null
                && networkActionResultExtension.getVariant(postOptimVariantId) != null
                && isActivated(preventiveState, networkActionResultExtension.getVariant(preOptimVariantId), networkActionResultExtension.getVariant(postOptimVariantId))) {
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

    public static boolean isActivated(String stateId, RangeActionResult preOptimRangeActionResult, RangeActionResult postOptimRangeActionResult) {
        if (!Double.isNaN(preOptimRangeActionResult.getSetPoint(stateId)) && !Double.isNaN(postOptimRangeActionResult.getSetPoint(stateId))) {
            return postOptimRangeActionResult.getSetPoint(stateId) != preOptimRangeActionResult.getSetPoint(stateId);
        }
        return false;
    }

    public static boolean isActivated(String stateId, NetworkActionResult preOptimNetworkActionResult, NetworkActionResult postOptimNetworkActionResult) {
        return postOptimNetworkActionResult.isActivated(stateId) != preOptimNetworkActionResult.isActivated(stateId);
    }

    public static String cutString(String string, int maxChar) {
        return string.substring(0, Math.min(string.length(), maxChar));
    }

    public static float limitFloatInterval(double value) {
        return (float) Math.min(Math.round(Math.abs(value)), 100000);
    }

    public static String findNodeInNetwork(String id, Network network, Branch.Side side) {
        try {
            return network.getBranch(id).getTerminal(side).getBusView().getBus().getId();
        } catch (NullPointerException e) {
            return network.getBranch(id).getTerminal(side).getBusView().getConnectableBus().getId();
        }
    }

    public static String computeAbsMarginMeasType(String measurementType) {
        String absMarginMeasType = "";
        if (measurementType.equals(PATL_MEASUREMENT_TYPE)) {
            absMarginMeasType = ABS_MARG_PATL_MEASUREMENT_TYPE;
        } else if (measurementType.equals(TATL_MEASUREMENT_TYPE)) {
            absMarginMeasType = ABS_MARG_TATL_MEASUREMENT_TYPE;
        }
        return absMarginMeasType;
    }
}
