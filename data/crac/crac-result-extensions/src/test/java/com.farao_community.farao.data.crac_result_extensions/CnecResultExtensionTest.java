/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CnecResultExtensionTest {

    @Test
    public void testCnecResultExtension() {

        CnecResultExtension cnecResultExtension = new CnecResultExtension();

        // test variant addition
        cnecResultExtension.addVariant("variant-before-opt", new CnecResult());
        cnecResultExtension.addVariant("variant-after-opt", new CnecResult());

        assertNotNull(cnecResultExtension.getVariant("variant-before-opt"));
        assertNotNull(cnecResultExtension.getVariant("variant-after-opt"));
        assertNull(cnecResultExtension.getVariant("variant-not-created"));

        // test variant deletion
        cnecResultExtension.deleteVariant("variant-before-opt");
        assertNull(cnecResultExtension.getVariant("variant-before-opt"));

        // add extension to a Cnec
        Set<Threshold> thresholds = new HashSet<>();
        Cnec cnec = new SimpleCnec("cnecId", new NetworkElement("networkElementId"), thresholds, Mockito.mock(State.class));

        cnec.addExtension(CnecResultExtension.class, cnecResultExtension);
        CnecResultExtension ext = cnec.getExtension(CnecResultExtension.class);
        assertNotNull(ext);
    }

    @Test
    public void getName() {
        CnecResultExtension cnecResultExtension = new CnecResultExtension();
        assertEquals("CnecResultExtension", cnecResultExtension.getName());
    }
}
