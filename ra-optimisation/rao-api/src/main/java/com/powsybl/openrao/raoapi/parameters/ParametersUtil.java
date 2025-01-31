/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class ParametersUtil {

    private ParametersUtil() {
    }

    public static Set<Country> convertToCountrySet(List<String> countryStringList) {
        Set<Country> countryList = new HashSet<>();
        for (String countryString : countryStringList) {
            try {
                countryList.add(Country.valueOf(countryString));
            } catch (Exception e) {
                throw new OpenRaoException(String.format("[%s] could not be recognized as a country", countryString));
            }
        }
        return countryList;
    }

    protected static Map<String, String> convertListToStringStringMap(List<String> stringList) {
        Map<String, String> map = new HashMap<>();
        stringList.forEach(listEntry -> {
            String[] splitListEntry = listEntry.split(":");
            if (splitListEntry.length != 2) {
                throw new OpenRaoException(String.format("String pairs separated by \":\" must be defined, e.g {String1}:{String2} instead of %s", listEntry));
            }
            map.put(convertBracketIntoString(splitListEntry[0]), convertBracketIntoString(splitListEntry[1]));
        });
        return map;
    }

    protected static List<String> convertStringStringMapToList(Map<String, String> map) {
        List<String> list = new ArrayList<>();
        map.forEach((key, value) -> list.add("{" + key + "}:{" + value + "}"));
        return list;
    }

    protected static Map<String, Integer> convertListToStringIntMap(List<String> stringList) {
        Map<String, Integer> map = new HashMap<>();
        stringList.forEach(listEntry -> {
            String[] splitListEntry = listEntry.split(":");
            if (splitListEntry.length != 2) {
                throw new OpenRaoException(String.format("String-Integer pairs separated by \":\" must be defined, e.g {String1}:Integer instead of %s", listEntry));
            }
            map.put(convertBracketIntoString(splitListEntry[0]), Integer.parseInt(splitListEntry[1]));
        });
        return map;
    }

    protected static List<String> convertStringIntMapToList(Map<String, Integer> map) {
        List<String> list = new ArrayList<>();
        map.forEach((key, value) -> list.add("{" + key + "}:" + value.toString()));
        return list;
    }

    public static List<List<String>> convertListToListOfList(List<String> stringList) {
        List<List<String>> listOfList = new ArrayList<>();
        stringList.forEach(listEntry -> {
            String[] splitListEntry = listEntry.split("\\+");
            List<String> newList = new ArrayList<>();
            for (String splitString : splitListEntry) {
                newList.add(convertBracketIntoString(splitString));
            }
            listOfList.add(newList);
        });
        return listOfList;
    }

    public static List<String> convertListOfListToList(List<List<String>> listOfList) {
        List<String> finalList = new ArrayList<>();
        listOfList.forEach(subList -> {
            if (!subList.isEmpty()) {
                finalList.add("{" + String.join("}+{", subList) + "}");
            }
        });
        return finalList;
    }

    private static String convertBracketIntoString(String stringInBrackets) {
        // Check that there are only one opening and one closing brackets
        if (stringInBrackets.chars().filter(ch -> ch == '{').count() != 1 || stringInBrackets.chars().filter(ch -> ch == '}').count() != 1) {
            throw new OpenRaoException(String.format("%s contains too few or too many occurences of \"{ or \"}", stringInBrackets));
        }
        String insideString = StringUtils.substringBetween(stringInBrackets, "{", "}");
        if (insideString == null || insideString.length() == 0) {
            throw new OpenRaoException(String.format("%s is not contained into brackets", stringInBrackets));
        }
        return insideString;
    }

}
