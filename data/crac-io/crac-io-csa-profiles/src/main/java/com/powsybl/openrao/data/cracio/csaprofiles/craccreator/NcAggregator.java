/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import com.powsybl.openrao.data.cracio.csaprofiles.nc.NCObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class NcAggregator<T extends NCObject> {
    private final Function<T, String> groupingAttribute;

    public NcAggregator(Function<T, String> groupingAttribute) {
        this.groupingAttribute = groupingAttribute;
    }

    public Map<String, Set<T>> aggregate(Set<T> ncObjects) {
        Map<String, Set<T>> ncObjectsPerAttribute = new HashMap<>();
        for (T ncObject : ncObjects) {
            String attributeValue = groupingAttribute.apply(ncObject);
            ncObjectsPerAttribute.computeIfAbsent(attributeValue, k -> new HashSet<>()).add(ncObject);
        }
        return ncObjectsPerAttribute;
    }
}
