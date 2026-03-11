/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class BorderDirectionTest {
    @Test
    void checkThatBorderDirectionIsCorrectlyCreated() {
        final BorderDirection borderDirection = new BorderDirection("Paris", "Berlin", true);
        Assertions.assertThat(borderDirection.from()).isEqualTo("Paris");
        Assertions.assertThat(borderDirection.to()).isEqualTo("Berlin");
        Assertions.assertThat(borderDirection.isAhc()).isTrue();
    }

    @Test
    void checkThatBorderDirectionCreationThrowsWhenFromIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new BorderDirection(null, "To", false))
            .withMessage("BorderDirection creation does not allow null attribute 'from'");
    }

    @Test
    void checkThatBorderDirectionCreationThrowsWhenToIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new BorderDirection("From", null, false))
            .withMessage("BorderDirection creation does not allow null attribute 'to'");
    }
}
