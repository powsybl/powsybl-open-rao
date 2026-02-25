/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TsoEICode;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcKeyword;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.OverridingObjectsFields;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.PropertyReference;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class NcCracUtils {

    private NcCracUtils() {

    }

    public static String getUniqueName(String prefixUrl, String suffix) {
        return getTsoNameFromUrl(prefixUrl).concat("_").concat(suffix);
    }

    public static Optional<String> createElementName(String nativeElementName, String tsoNameUrl) {
        if (nativeElementName != null) {
            if (tsoNameUrl != null) {
                return Optional.of(getUniqueName(tsoNameUrl, nativeElementName));
            }
            return Optional.of(nativeElementName);
        } else {
            return Optional.empty();
        }
    }

    public static boolean isValidInterval(OffsetDateTime dateTime, String startTime, String endTime) {
        if (Objects.isNull(dateTime) || Objects.isNull(startTime) || Objects.isNull(endTime)) {
            return false;
        }
        try {
            OffsetDateTime startDateTime = OffsetDateTime.parse(startTime);
            OffsetDateTime endDateTime = OffsetDateTime.parse(endTime);
            return !dateTime.isBefore(startDateTime) && !dateTime.isAfter(endDateTime);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static int convertDurationToSeconds(String duration) {
        Pattern removeYearAndMonthPattern = Pattern.compile("P(?:0Y)?(?:0M)?(\\d+D)?(T(?:\\d+H)?(?:\\d+M)?(?:\\d+S)?)?");
        Matcher matcher = removeYearAndMonthPattern.matcher(duration);
        String durationWithoutYearAndMonth;
        if (matcher.matches()) {
            durationWithoutYearAndMonth = "P" + (matcher.group(1) == null ? "" : matcher.group(1)) + (matcher.group(2) == null ? "" : matcher.group(2));
        } else {
            durationWithoutYearAndMonth = duration;
        }

        try {
            return (int) java.time.Duration.parse(durationWithoutYearAndMonth).get(ChronoUnit.SECONDS);
        } catch (DateTimeException dateTimeException) {
            throw new OpenRaoException("Error occurred while converting time to implement to seconds for duration: " + durationWithoutYearAndMonth);
        }
    }

    public static void checkPropertyReference(String remedialActionId, String gridStateAlterationType, PropertyReference expectedPropertyReference, String actualPropertyReference) {
        if (!expectedPropertyReference.toString().equals(actualPropertyReference)) {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Remedial action %s will not be imported because %s must have a property reference with %s value, but it was: %s",
                              remedialActionId, gridStateAlterationType, expectedPropertyReference, actualPropertyReference)
            );
        }
    }

    public static boolean checkProfileValidityInterval(PropertyBag propertyBag, OffsetDateTime importTimestamp) {
        String startTime = propertyBag.get(NcConstants.REQUEST_HEADER_START_DATE);
        String endTime = propertyBag.get(NcConstants.REQUEST_HEADER_END_DATE);
        return isValidInterval(importTimestamp, startTime, endTime);
    }

    public static boolean checkProfileKeyword(PropertyBag propertyBag, NcKeyword ncKeyword) {
        String keyword = propertyBag.get(NcConstants.REQUEST_HEADER_KEYWORD);
        return ncKeyword.toString().equals(keyword);
    }

    public static Set<String> addFileToSet(Map<String, Set<String>> map, String contextName, String keyword) {
        Set<String> returnSet = map.getOrDefault(keyword, new HashSet<>());
        returnSet.add(contextName);
        return returnSet;
    }

    public static PropertyBags overrideData(PropertyBags propertyBags, Map<String, String> dataMap, OverridingObjectsFields overridingObjectsFields) {
        for (PropertyBag propertyBag : propertyBags) {
            String id = propertyBag.getId(overridingObjectsFields.getObjectName());
            String data = dataMap.get(id);
            if (data != null) {
                propertyBag.put(overridingObjectsFields.getInitialFieldName(), data);
            }
        }
        return propertyBags;
    }

    public static String getEicFromUrl(String url) {
        Pattern eicPattern = Pattern.compile("http://energy.referencedata.eu/EIC/([A-Z0-9_+\\-]{16})");
        Matcher matcher = eicPattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String getTsoNameFromUrl(String url) {
        return TsoEICode.fromEICode(getEicFromUrl(url)).getDisplayName();
    }
}
