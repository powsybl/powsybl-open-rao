/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public enum UcteNodeMatchingRule {

    FIRST_7_CHARACTER_EQUAL(7),
    ALL_8_CHARACTER_EQUAL(8);

    private final int value;

    UcteNodeMatchingRule(int value) {
        this.value = value;
    }

    public int getNbCharacter() {
        return value;
    }
}
