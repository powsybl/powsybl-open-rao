/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.data.crac_file.xlsx.service;

import com.farao_community.farao.data.crac_file.xlsx.model.ElementDescriptionMode;
import com.farao_community.farao.data.crac_file.xlsx.model.TimesSeries;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public final class CracTools {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.XlsxCracApi");
    private static final int MAX_NODEID_LENGTH = 8;

    private CracTools() {
        throw new AssertionError("Utility class should not have constructor");
    }

    /**
     * concat of order code and element name.
     *
     * @param type
     * @param utcNodeFrom
     * @param utcNodeTo
     * @param orderCodeElementName
     */
    public static String getOrderCodeElementName(ElementDescriptionMode type, String utcNodeFrom, String utcNodeTo, String orderCodeElementName) {
        String id = "";
        String spaceNodeFrom = " ";
        String spaceNodeTo = " ";
        int nodeFromSpace = MAX_NODEID_LENGTH - utcNodeFrom.length();
        int nodeToSpace = MAX_NODEID_LENGTH - utcNodeTo.length();
        for (int i = 0; i < nodeFromSpace; i++) {
            spaceNodeFrom = spaceNodeFrom + " ";
        }
        for (int i = 0; i < nodeToSpace; i++) {
            spaceNodeTo = spaceNodeTo + " ";
        }
        if (null != type) {
            if (type.equals(ElementDescriptionMode.ORDER_CODE)) {
                id = utcNodeFrom + spaceNodeFrom + utcNodeTo + spaceNodeTo + orderCodeElementName;
            } else if (type.equals(ElementDescriptionMode.ELEMENT_NAME)) {
                id = orderCodeElementName;
            } else {
                id = RESOURCE_BUNDLE.getString("elementNameTypeNotDefined");
            }
        }
        return id;
    }

    public static String getId(String id, TimesSeries hour, LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return id + formatter.format(date) + "_" + hour.name().replaceFirst("TIME_", "");
    }

    public static String getDescription(String fileName, TimesSeries hour, LocalDate date) {
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime ldt = LocalDateTime.now();
        return String.format("%s %s %s.%n%s '%s.xlsx' %s %s %s %s",
                RESOURCE_BUNDLE.getString("descriptionPart1"),
                date,
                hour.toString().replaceFirst("TIME_", ""),
                RESOURCE_BUNDLE.getString("descriptionPart2"),
                fileName,
                RESOURCE_BUNDLE.getString("descriptionPart3"),
                ldt.format(formatterDate),
                RESOURCE_BUNDLE.getString("descriptionpart4"),
                ldt.format(formatterTime)
        );
    }
}
