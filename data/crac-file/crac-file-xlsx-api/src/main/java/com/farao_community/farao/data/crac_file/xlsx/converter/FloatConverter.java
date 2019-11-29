/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.converter;

import com.farao_community.farao.commons.FaraoException;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Float Converter class
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@Slf4j
public final class FloatConverter implements Converter<Float> {
    @Override
    public Float convert(String value, int row) {
        if (value == null) {
            return Float.valueOf(0);
        }
        try {
            String result = value.trim();
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            DecimalFormat format = new DecimalFormat("0.#");
            format.setDecimalFormatSymbols(symbols);
            return format.parse(result).floatValue();
        } catch (Exception e) {
            log.error(String.format("Failed to parse '%s' as float at row='%s' ", value, row));
            throw new FaraoException(String.format("Failed to parse '%s' as float at row='%s' ", value, row), e);
        }
    }
}
