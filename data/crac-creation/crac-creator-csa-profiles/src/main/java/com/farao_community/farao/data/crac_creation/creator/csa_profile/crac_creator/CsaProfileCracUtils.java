/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileCracUtils {

    private CsaProfileCracUtils() {

    }

    public static PropertyBags getLinkedPropertyBags(PropertyBags sources, PropertyBag dest, String sourceProperty, String destProperty) {
        PropertyBags linkedBags = new PropertyBags();
        for (PropertyBag source : sources) {
            String sourceValue = source.getId(sourceProperty);
            if (sourceValue != null && sourceValue.equals(dest.getId(destProperty))) {
                linkedBags.add(source);
            }
        }
        return linkedBags;
    }
}
