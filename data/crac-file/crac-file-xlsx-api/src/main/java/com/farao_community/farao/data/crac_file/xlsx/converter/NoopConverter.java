/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.converter;

/**
 * Empty Converter class
 *
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public final class NoopConverter implements Converter<String> {
    @Override
    public String convert(String value, int row) {
        return value;
    }
}
