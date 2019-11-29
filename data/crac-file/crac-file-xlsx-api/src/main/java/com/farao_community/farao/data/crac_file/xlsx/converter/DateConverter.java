/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.converter;

import com.farao_community.farao.commons.FaraoException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Date Converter class
 *
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@Slf4j
public final class DateConverter implements Converter<LocalDate> {
    private static final String[] VALID_FORMAT_PATTERNS = {"dd/MM/yyyy", "M/dd/yy", "M/d/yy", "yyyy.MM.dd", "dd-MM-yyyy", "dd.MM.yyyy", "d/MM/yyyy", "d-MM-yyyy", "d.MM.yyyy", "d/MM/yy", "d-MM-yy", "d.MM.yy"
    };

    @Override
    public LocalDate convert(String value, int row) {
        Optional<LocalDate> test =  Arrays.stream(VALID_FORMAT_PATTERNS).map(DateTimeFormatter::ofPattern)
                .map(formatter -> {
                    try {
                        return LocalDate.parse(value.trim(), formatter);
                    } catch (Exception e) {
                        // Incorrect pattern : return null
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();
        if (test.isPresent()) {
            return test.get();
        } else {
            log.error(String.format("Failed to parse '%s' as Date at row='%s' ", value, row));
            throw new FaraoException(String.format("Failed to parse '%s' as Date at row='%s' ", value, row));
        }

    }
}
