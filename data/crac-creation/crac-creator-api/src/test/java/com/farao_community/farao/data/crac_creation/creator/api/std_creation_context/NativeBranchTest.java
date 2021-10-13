/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.api.std_creation_context;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NativeBranchTest {

    @Test
    public void testConstructor() {
        NativeBranch nativeBranch = new NativeBranch("from", "to", "suffix");
        assertEquals("from", nativeBranch.getFrom());
        assertEquals("to", nativeBranch.getTo());
        assertEquals("suffix", nativeBranch.getSuffix());
    }
}
