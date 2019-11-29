/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.converter;

import lombok.extern.slf4j.Slf4j;

/**
 * Float Converter class
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@Slf4j
public final class IntConverter implements Converter<Integer> {
    @Override
    public Integer convert(String value, int row) {
        if (value == null) {
            return Integer.valueOf(0);
        }
        return Integer.valueOf(value.trim());
    }
}
