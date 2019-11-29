/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.converter;

/**
 * Converts a value from a String type to another
 *
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@FunctionalInterface
public interface Converter<T> {
    T convert(String value, int row);
}
