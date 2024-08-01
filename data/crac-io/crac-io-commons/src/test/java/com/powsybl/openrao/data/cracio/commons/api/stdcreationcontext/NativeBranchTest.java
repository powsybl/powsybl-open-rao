/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NativeBranchTest {

    @Test
    void testConstructor() {
        com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.NativeBranch nativeBranch = new NativeBranch("from", "to", "suffix");
        assertEquals("from", nativeBranch.getFrom());
        assertEquals("to", nativeBranch.getTo());
        assertEquals("suffix", nativeBranch.getSuffix());
    }
}
