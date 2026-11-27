/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
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
class ControlAreaTest {
    @Test
    void checkThatControlAreaIsCorrectlyCreated() {
        final ControlArea controlArea = new ControlArea("code", "eic", true, true, true);
        Assertions.assertThat(controlArea.code()).isEqualTo("code");
        Assertions.assertThat(controlArea.eic()).isEqualTo("eic");
        Assertions.assertThat(controlArea.isMcParticipant()).isTrue();
        Assertions.assertThat(controlArea.isTechCounterparty()).isTrue();
        Assertions.assertThat(controlArea.isSdac()).isTrue();
    }

    @Test
    void checkThatControlAreaCreationThrowsWhenCodeIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new ControlArea(null, "eic", false, false, false))
            .withMessage("ControlArea creation does not allow null attribute 'code'");
    }

    @Test
    void checkThatControlAreaCreationThrowsWhenEicIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new ControlArea("code", null, false, false, false))
            .withMessage("ControlArea creation does not allow null attribute 'eic'");
    }
}
