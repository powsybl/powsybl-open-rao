/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TsoEICode;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileCracUtils {

    private CsaProfileCracUtils() {

    }

    public static Map<String, Set<PropertyBag>> getMappedPropertyBagsSet(PropertyBags propertyBags, String property) {
        Map<String, Set<PropertyBag>> mappedPropertyBags = new HashMap<>();
        for (PropertyBag propertyBag : propertyBags) {
            String propValue = propertyBag.getId(property);
            Set<PropertyBag> propPropertyBags = mappedPropertyBags.computeIfAbsent(propValue, k -> new HashSet<>());
            propPropertyBags.add(propertyBag);
        }
        return mappedPropertyBags;
    }

    public static String getUniqueName(String prefixUrl, String suffix) {
        return TsoEICode.fromEICode(prefixUrl.substring(prefixUrl.lastIndexOf('/') + 1)).getDisplayName().concat("_").concat(suffix);
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
        try {
            OffsetDateTime startDateTime = OffsetDateTime.parse(startTime);
            OffsetDateTime endDateTime = OffsetDateTime.parse(endTime);
            return !dateTime.isBefore(startDateTime) && !dateTime.isAfter(endDateTime);
        } catch (Exception e) {
            return false;
        }
    }

    public static int convertDurationToSeconds(String duration) {
        Map<Character, Integer> durationFactors = new HashMap<>();
        durationFactors.put('D', 86400);
        durationFactors.put('H', 3600);
        durationFactors.put('M', 60);
        durationFactors.put('S', 1);

        Pattern pattern = Pattern.compile("P(?:\\d+D)?(?:T(?:\\d+H)?(?:\\d+M)?(?:\\d+S)?)?");
        Matcher matcher = pattern.matcher(duration);

        if (!matcher.matches()) {
            throw new OpenRaoException("Error occurred while converting time to implement to seconds, unknown pattern: " + duration);
        }

        int seconds = 0;

        for (Map.Entry<Character, Integer> entry : durationFactors.entrySet()) {
            Pattern unitPattern = Pattern.compile("(\\d+)" + entry.getKey());
            Matcher unitMatcher = unitPattern.matcher(duration);

            if (unitMatcher.find()) {
                int value = Integer.parseInt(unitMatcher.group(1));
                seconds += value * entry.getValue();
            }
        }
        return seconds;
    }

    public static void checkPropertyReference(PropertyBag propertyBag, String remedialActionId, String propertyBagKind, String expectedPropertyReference) {
        String actualPropertyReference = propertyBag.get(CsaProfileConstants.GRID_ALTERATION_PROPERTY_REFERENCE);
        if (!actualPropertyReference.equals(expectedPropertyReference)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action '%s' will not be imported because '%s' must have a property reference with '%s' value, but it was: '%s'", remedialActionId, propertyBagKind, expectedPropertyReference, actualPropertyReference));
        }
    }

    public static boolean checkProfileValidityInterval(PropertyBag propertyBag, OffsetDateTime importTimestamp) {
        String startTime = propertyBag.get(CsaProfileConstants.REQUEST_HEADER_START_DATE);
        String endTime = propertyBag.get(CsaProfileConstants.REQUEST_HEADER_END_DATE);
        return isValidInterval(importTimestamp, startTime, endTime);
    }

    public static boolean checkProfileKeyword(PropertyBag propertyBag, CsaProfileConstants.CsaProfileKeywords csaProfileKeyword) {
        String keyword = propertyBag.get(CsaProfileConstants.REQUEST_HEADER_KEYWORD);
        return csaProfileKeyword.toString().equals(keyword);
    }

    public static Set<String> addFileToSet(Map<String, Set<String>> map, String contextName, String keyword) {
        Set<String> returnSet = map.getOrDefault(keyword, new HashSet<>());
        returnSet.add(contextName);
        return returnSet;
    }

    public static PropertyBags overrideData(PropertyBags propertyBags, Map<String, String> dataMap, CsaProfileConstants.OverridingObjectsFields overridingObjectsFields) {
        for (PropertyBag propertyBag : propertyBags) {
            String id = propertyBag.getId(overridingObjectsFields.getObjectName());
            String data = dataMap.get(id);
            if (data != null) {
                propertyBag.put(overridingObjectsFields.getInitialFieldName(), data);
            }
        }
        return propertyBags;
    }

}
