/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.iidm.network.Country;

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
                throw new FaraoException(String.format("[%s] in loopflow countries could not be recognized as a country", countryString));
            }
        }
        return countryList;
    }

    public static List<String> convertMapStringStringToList(Map<String, String> map) {
        List<String> list = new ArrayList<>();
        map.entrySet().forEach(entry -> list.add(entry.getKey() + " : " + entry.getValue()));
        return list;
    }

    public static Map<String, String> convertListToMapStringString(List<String> list) {
        Map<String, String> map = new HashMap<>();
        list.forEach(listEntry -> {
            String[] splitListEntry = listEntry.split(" : ");
            map.put(splitListEntry[0], splitListEntry[1]);
        });
        return map;
    }

    public static List<String> convertMapStringIntegerToList(Map<String, Integer> map) {
        List<String> list = new ArrayList<>();
        map.entrySet().forEach(entry -> list.add(entry.getKey() + " : " + entry.getValue().toString()));
        return list;
    }

    public static Map<String, Integer> convertListToMapStringInteger(List<String> list) {
        Map<String, Integer> map = new HashMap<>();
        list.stream().forEach(listEntry -> {
            String[] splitListEntry = listEntry.split(" : ");
            map.put(splitListEntry[0], Integer.parseInt(splitListEntry[1]));
        });
        return map;
    }

    public static List<String> convertListOfListToList(List<List<String>> listOfList) {
        List<String> list = new ArrayList<>();
        listOfList.forEach(entry -> list.add(String.join(" + ", entry)));
        return list;
    }

    // TODO
    public static List<List<String>> convertListToListOfList(List<String> list) {
        List<List<String>> listOfList = new ArrayList<>();
        list.forEach(listEntry -> {
            String[] splitListEntry = listEntry.split(" + ");
            List<String> newList = new ArrayList<>();
            newList.addAll(Arrays.asList(splitListEntry));
            listOfList.add(newList);
        });
        return listOfList;
    }

}
