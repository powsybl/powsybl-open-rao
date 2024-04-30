package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.NCObject;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NcPropertyBagsConverter<T extends NCObject> {
    private final Function<PropertyBag, T> propertyBagConverter;

    public NcPropertyBagsConverter(Function<PropertyBag, T> propertyBagConverter) {
        this.propertyBagConverter = propertyBagConverter;
    }

    public Set<T> convert(PropertyBags propertyBags) {
        return propertyBags.stream().map(propertyBagConverter).collect(Collectors.toSet());
    }
}
