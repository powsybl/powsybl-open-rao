/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.NCObject;
import com.powsybl.triplestore.api.PropertyBag;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 *
 * Utility class that provides a generic template to query native objects
 * in NC profiles and to cast the results to native NC objects.
 */
public final class NativeParser {

    private NativeParser() {
    }

    private static Object parseStringValue(String value, Class<?> targetType) {
        if (value == null || String.class.equals(targetType)) {
            return value;
        } else if (Boolean.class.equals(targetType)) {
            if ("true".equalsIgnoreCase(value)) {
                return true;
            } else if ("false".equalsIgnoreCase(value)) {
                return false;
            } else {
                return null;
            }
        } else if (Double.class.equals(targetType)) {
            return Double.parseDouble(value);
        } else if (Integer.class.equals(targetType)) {
            return Integer.parseInt(value);
        } else {
            throw new OpenRaoException("Unsupported type %s".formatted(targetType.getName()));
        }
    }

    private static String getMrid(PropertyBag propertyBag, Class<?> nativeClass) {
        String propertyBagName = Character.toLowerCase(nativeClass.getSimpleName().charAt(0)) + nativeClass.getSimpleName().substring(1);
        return propertyBag.getId(propertyBagName);
    }

    public static <T extends NCObject> T fromPropertyBag(PropertyBag propertyBag, Class<T> nativeClass, Map<String, Object> defaultValues) throws IllegalArgumentException, OpenRaoException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // Ensure the class is a record
        if (!nativeClass.isRecord()) {
            throw new IllegalArgumentException("Provided class is not a record");
        }

        // Get the record's components (fields)
        RecordComponent[] components = nativeClass.getRecordComponents();

        // Prepare an array to hold the values for the record's constructor
        Object[] constructorArgs = new Object[components.length];

        // Loop through the components and extract values from the property bag
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            if ("mrid".equals(component.getName())) {
                constructorArgs[i] = getMrid(propertyBag, nativeClass);
            } else {
                Object parsedValue = parseStringValue(propertyBag.getId(component.getName()), component.getType());
                if (parsedValue == null) {
                    parsedValue = defaultValues.get(component.getName());
                }
                constructorArgs[i] = parsedValue;
            }
        }

        // Find the canonical constructor of the record class
        Constructor<T> constructor = nativeClass.getDeclaredConstructor(
            // Get the types of the components (this matches the constructor signature)
            Arrays.stream(components)
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new)
        );

        // Create and return a new instance of the record
        return constructor.newInstance(constructorArgs);
    }
}
