/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.commons.TsoEICode;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileCracUtils {

    private CsaProfileCracUtils() {

    }

    public static Map<String, ArrayList<PropertyBag>> getMappedPropertyBags(PropertyBags propertyBags, String property) {
        Map<String, ArrayList<PropertyBag>> mappedPropertyBags = new HashMap<>();
        for (PropertyBag propertyBag : propertyBags) {
            String propValue = propertyBag.getId(property);
            ArrayList<PropertyBag> propPropertyBags = mappedPropertyBags.get(propValue);
            if (propPropertyBags == null) {
                propPropertyBags = new ArrayList<>();
                mappedPropertyBags.put(propValue, propPropertyBags);
            }
            propPropertyBags.add(propertyBag);
        }
        return mappedPropertyBags;
    }

    public static String getUniqueName(String idWithEicCode, String elementId) {
        return TsoEICode.fromEICode(idWithEicCode.substring(idWithEicCode.lastIndexOf('/') + 1)).getDisplayName().concat("_").concat(elementId);
    }

    public static boolean isValidInterval(OffsetDateTime dateTime, String startTime, String endTime) {
        OffsetDateTime startDateTime = OffsetDateTime.parse(startTime);
        OffsetDateTime endDateTime = OffsetDateTime.parse(endTime);
        return !dateTime.isBefore(startDateTime) && !dateTime.isAfter(endDateTime);
    }
}
