/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cneexportercommons;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Auxiliary methods
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CneUtil {
    private static final double FLOAT_LIMIT = 999999;
    private static Set<String> usedUniqueIds;
    private static final String DOCUMENT_ID = "document-id";
    private static final String REVISION_NUMBER = "revision-number";
    private static final String DOMAIN_ID = "domain-id";
    private static final String PROCESS_TYPE = "process-type";
    private static final String SENDER_ID = "sender-id";
    private static final String SENDER_ROLE = "sender-role";
    private static final String RECEIVER_ID = "receiver-id";
    private static final String RECEIVER_ROLE = "receiver-role";
    private static final String TIME_INTERVAL = "time-interval";
    private static final String OBJECTIVE_FUNCTION_TYPE = "objective-function-type";
    private static final String WITH_LOOP_FLOWS = "with-loop-flows";
    private static final String MNEC_ACCEPTABLE_MARGIN_DIMINUTION = "mnec-acceptable-margin-diminution";

    private CneUtil() {
    }

    public static void initUniqueIds() {
        usedUniqueIds = new HashSet<>();
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
            throw new OpenRaoException("Could not write current date and time.");
        }
    }

    public static String cutString(String string, int maxChar) {
        return string.substring(0, Math.min(string.length(), maxChar));
    }

    public static String randomizeString(String string, int maxChar) {
        int nbRandomChars = 5;
        Random random = new SecureRandom();
        String randomString;
        do {
            String newString = cutString(string, maxChar - nbRandomChars - 1);
            randomString = newString + "_" + cutString(Integer.toHexString(random.nextInt()), nbRandomChars);
        } while (usedUniqueIds.contains(randomString));
        usedUniqueIds.add(randomString);
        return randomString;
    }

    public static float limitFloatInterval(double value) {
        return (float) Math.min(Math.round(Math.abs(value)), FLOAT_LIMIT);
    }

    public static String generateUUID() {
        String uuidString = UUID.randomUUID().toString().substring(1);
        while (usedUniqueIds.contains(uuidString)) {
            uuidString = UUID.randomUUID().toString().substring(1);
        }
        usedUniqueIds.add(uuidString);
        return uuidString;
    }

    public static RaoParameters getRaoParametersFromProperties(Properties properties) {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins();
        ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunctionType = getObjectiveFunctionTypeFromString(properties.getProperty(OBJECTIVE_FUNCTION_TYPE, "max-min-relative-margin-in-megawatt"));
        raoParameters.getObjectiveFunctionParameters().setType(objectiveFunctionType);
        boolean withLoopFlow = Boolean.parseBoolean(properties.getProperty(WITH_LOOP_FLOWS, "false"));
        if (withLoopFlow) {
            raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        }
        double mnecAcceptableMarginDiminution = Double.parseDouble(properties.getProperty(MNEC_ACCEPTABLE_MARGIN_DIMINUTION, "0"));
        if (mnecAcceptableMarginDiminution != 0) {
            MnecParametersExtension mnecParametersExtension = new MnecParametersExtension();
            mnecParametersExtension.setAcceptableMarginDecrease(mnecAcceptableMarginDiminution);
            raoParameters.addExtension(MnecParametersExtension.class, mnecParametersExtension);
        }
        return raoParameters;
    }

    private static ObjectiveFunctionParameters.ObjectiveFunctionType getObjectiveFunctionTypeFromString(String objectiveFunctionType) {
        if ("max-min-relative-margin-in-ampere".equals(objectiveFunctionType)) {
            return ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE;
        } else if ("max-min-relative-margin-in-megawatt".equals(objectiveFunctionType)) {
            return ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT;
        } else if ("max-min-margin-in-ampere".equals(objectiveFunctionType)) {
            return ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE;
        } else if ("max-min-margin-in-megawatt".equals(objectiveFunctionType)) {
            return ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT;
        } else {
            throw new OpenRaoException("Unknown ObjectiveFunctionType %s".formatted(objectiveFunctionType));
        }
    }

    public static CneExporterParameters getParametersFromProperties(Properties properties) {
        return new CneExporterParameters(
            getPropertyOrThrowException(properties, DOCUMENT_ID),
            Integer.parseInt(getPropertyOrThrowException(properties, REVISION_NUMBER)),
            getPropertyOrThrowException(properties, DOMAIN_ID),
            getProcessTypeFromString(getPropertyOrThrowException(properties, PROCESS_TYPE)),
            getPropertyOrThrowException(properties, SENDER_ID),
            getRoleTypeFromString(getPropertyOrThrowException(properties, SENDER_ROLE)),
            getPropertyOrThrowException(properties, RECEIVER_ID),
            getRoleTypeFromString(getPropertyOrThrowException(properties, RECEIVER_ROLE)),
            getPropertyOrThrowException(properties, TIME_INTERVAL)
        );
    }

    private static String getPropertyOrThrowException(Properties properties, String propertyName) {
        if (properties.containsKey(propertyName)) {
            return properties.getProperty(propertyName);
        }
        throw new OpenRaoException("Could not parse CNE exporter parameters because mandatory property %s is missing.".formatted(propertyName));
    }

    private static CneExporterParameters.ProcessType getProcessTypeFromString(String processType) {
        switch (processType) {
            case "A48" -> {
                return CneExporterParameters.ProcessType.DAY_AHEAD_CC;
            }
            case "Z01" -> {
                return CneExporterParameters.ProcessType.Z01;
            }
            default -> throw new OpenRaoException("Unknown ProcessType %s".formatted(processType));
        }
    }

    private static CneExporterParameters.RoleType getRoleTypeFromString(String roleType) {
        switch (roleType) {
            case "A36" -> {
                return CneExporterParameters.RoleType.CAPACITY_COORDINATOR;
            }
            case "A44" -> {
                return CneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR;
            }
            case "A04" -> {
                return CneExporterParameters.RoleType.SYSTEM_OPERATOR;
            }
            default -> throw new OpenRaoException("Unknown RoleType %s".formatted(roleType));
        }
    }
}
