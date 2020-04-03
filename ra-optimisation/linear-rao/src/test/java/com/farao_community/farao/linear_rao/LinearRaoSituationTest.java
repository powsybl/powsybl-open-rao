/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class LinearRaoSituationTest {

    @Test
    public void initialSituationTest() {
        Crac crac = new SimpleCrac("crac");
        LinearRaoInitialSituation linearRaoInitialSituation = new LinearRaoInitialSituation(crac);

        assertNotNull(crac.getExtension(CracResultExtension.class);

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(1, resultVariantManager.getVariants().size());

        linearRaoInitialSituation.deleteResultVariant();
        // We don't want to delete the initial variant
        assertEquals(1, resultVariantManager.getVariants().size());

    }
}
