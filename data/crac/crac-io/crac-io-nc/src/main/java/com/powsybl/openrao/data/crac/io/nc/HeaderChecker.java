/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcKeyword;
import com.powsybl.triplestore.api.PropertyBag;

import java.time.OffsetDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO: update SPARQL queries to make keyword, startDate and conformsTo mandatory
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class HeaderChecker {

    private static final String SUPPORTED_NC_VERSION = "2.2";
    private static final Pattern VERSION_PATTERN = Pattern.compile("http://entsoe\\.eu/ns/CIM/(?<name>[A-Za-z]+)-EU/(?<version>[1-9]\\d*\\.[0-9]+)");

    private HeaderChecker() {
    }

    public static boolean checkHeader(PropertyBag propertyBag, String fileName, OffsetDateTime timestamp) {
        // 1. Retrieve keyword
        NcKeyword keyword = NcKeyword.getNcKeyword(propertyBag.get("keyword"));
        if (keyword == null) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("[NC Importer] File {} ignored because its keyword does not match any valid NC keyword.", fileName);
            return false;
        }

        // 2. Retrieve version
        String version = propertyBag.get("conformsTo");
        // TODO: remove conditional check when attribute is made mandatory in query
        if (version != null) {
            Matcher matcher = VERSION_PATTERN.matcher(version);
            String conformityUrl = "http://entsoe.eu/ns/CIM/" + keyword.getFullName() + "-EU/" + SUPPORTED_NC_VERSION;
            if (!matcher.matches()) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("[NC Importer] File {} ignored because it does not conform to the expected {} v{} profile standard (expected {}, got {}).", fileName, keyword.toString(), SUPPORTED_NC_VERSION, conformityUrl, version);
                return false;
            }
            String profileName = matcher.group("name");
            if (!keyword.getFullName().equals(profileName)) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("[NC Importer] File {} ignored because its keyword {} is not consistent with the declared type (expected {}, got {}).", fileName, keyword.toString(), keyword.getFullName(), profileName);
                return false;
            }
            String profileVersion = matcher.group("version");
            if (!SUPPORTED_NC_VERSION.equals(profileVersion)) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("[NC Importer] File {} ignored because its version {} is not supported by OpenRAO (only supported version is {}).", fileName, profileVersion, SUPPORTED_NC_VERSION);
                return false;
            }
        }

        // 3. Check timewise validity
        String startDate = propertyBag.get("startDate");
        // TODO: remove conditional check when attribute is made mandatory in query
        if (startDate == null) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("[NC Importer] File {} ignored because no validity start date was provided.", fileName);
            return false;
        }
        OffsetDateTime startTimestamp = OffsetDateTime.parse(startDate);
        if (startTimestamp.isAfter(timestamp)) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("[NC Importer] File {} ignored because its validity start date {} is posterior to the import timestamp.", fileName, startTimestamp);
            return false;
        }
        String endDate = propertyBag.get("endDate");
        if (endDate != null) {
            OffsetDateTime endTimestamp = OffsetDateTime.parse(endDate);
            if (endTimestamp.isBefore(timestamp)) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("[NC Importer] File {} ignored because its validity end date {} is anterior to the import timestamp.", fileName, endTimestamp);
                return false;
            }
        }

        return true;
    }
}
