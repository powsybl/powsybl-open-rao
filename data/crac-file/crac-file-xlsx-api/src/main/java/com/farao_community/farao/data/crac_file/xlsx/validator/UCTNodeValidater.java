/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.validator;

import com.farao_community.farao.commons.FaraoException;
import io.vavr.Predicates;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

/**
 * UCTNode validater class
 */
@Slf4j
public final class UCTNodeValidater implements Validater<String> {
    @Override
    public void validate(String value, int row) {
        Predicate<String> length = v -> v.length() != 8;
        if (Predicates.anyOf(length).test(value)) {
            log.error(String.format("Failed to parse %s the UCT code can be described with 7 and a '*' or 8 characters at row=%s ", value, row));
            throw new FaraoException(String.format("Failed to parse %s the UCT code can be described with 7 and a '*' or 8 characters at row=%s  ", value, row));
        }
    }
}
