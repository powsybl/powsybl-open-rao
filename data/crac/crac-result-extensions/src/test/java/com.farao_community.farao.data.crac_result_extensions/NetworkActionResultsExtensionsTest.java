/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.SimpleState;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionResultsExtensionsTest {

    @Test
    public void testOk() {
        NetworkActionResultsExtension networkActionResultsExtension = new NetworkActionResultsExtension();

        Set<State> stateSet = Collections.singleton(new SimpleState(Optional.empty(), new Instant("N", 0)));

        // test variant addition
        networkActionResultsExtension.addVariant("variant-before-opt", new NetworkActionResult(stateSet));
        networkActionResultsExtension.addVariant("variant-after-opt", new NetworkActionResult(stateSet));

        assertNotNull(networkActionResultsExtension.getVariant("variant-before-opt"));
        assertNotNull(networkActionResultsExtension.getVariant("variant-after-opt"));
        assertNull(networkActionResultsExtension.getVariant("variant-not-created"));

        // test variant deletion
        networkActionResultsExtension.deleteVariant("variant-before-opt");
        assertNull(networkActionResultsExtension.getVariant("variant-before-opt"));
    }

    @Test
    public void getName() {
        NetworkActionResultsExtension networkActionResultsExtension = new NetworkActionResultsExtension();
        assertEquals("NetworkActionResultsExtension", networkActionResultsExtension.getName());
    }
}
