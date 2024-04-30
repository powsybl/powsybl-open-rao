package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.NCObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class NcAggregator<T extends NCObject> {
    private final Function<T, String> attribute;

    public NcAggregator(Function<T, String> attribute) {
        this.attribute = attribute;
    }

    public Map<String, Set<T>> aggregate(Set<T> ncObjects) {
        Map<String, Set<T>> ncObjectsPerAttribute = new HashMap<>();
        for (T ncObject : ncObjects) {
            String attributeValue = attribute.apply(ncObject);
            ncObjectsPerAttribute.computeIfAbsent(attributeValue, k -> new HashSet<>()).add(ncObject);
        }
        return ncObjectsPerAttribute;
    }
}
