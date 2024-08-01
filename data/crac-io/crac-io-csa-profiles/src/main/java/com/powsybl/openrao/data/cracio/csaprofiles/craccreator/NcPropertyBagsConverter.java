/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import com.powsybl.openrao.data.cracio.csaprofiles.nc.NCObject;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class NcPropertyBagsConverter<T extends NCObject> {
    private final Function<PropertyBag, T> propertyBagConverter;

    public NcPropertyBagsConverter(Function<PropertyBag, T> propertyBagConverter) {
        this.propertyBagConverter = propertyBagConverter;
    }

    public Set<T> convert(PropertyBags propertyBags) {
        return propertyBags.stream().map(propertyBagConverter).collect(Collectors.toSet());
    }
}
