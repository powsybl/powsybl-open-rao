/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.ResourceBundle;

import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonResultVariantManagerTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void cracRoundTripTest() {
        // Crac
        SimpleCrac simpleCrac = new SimpleCrac("cracId", "cracName");

        // add extension
        simpleCrac.addExtension(ResultVariantManager.class, new ResultVariantManager());
        simpleCrac.getExtension(ResultVariantManager.class).createVariant("variant-id-1");
        simpleCrac.getExtension(ResultVariantManager.class).createVariant("variant-id-2");
        simpleCrac.getExtension(ResultVariantManager.class).createVariant("variant-id-3");

        // export crac
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CracExporters.exportCrac(simpleCrac, "Json", outputStream);

        // import Crac
        Crac crac;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            crac = CracImporters.importCrac("unknown.json", inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // assert that the crac has a ResultVariantManager extension with the expected content
        assertNotNull(crac.getExtension(ResultVariantManager.class));
        assertEquals(3, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains("variant-id-1"));
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains("variant-id-2"));
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains("variant-id-3"));
    }

    @Test
    public void cracImportTest() {
       /* Crac crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));

        assertNotNull(crac.getCnec("Tieline BE FR - Défaut - N-1 NL1-NL3").getExtension(CnecResult.class));
        assertEquals(-450.0, crac.getCnec("Tieline BE FR - Défaut - N-1 NL1-NL3").getExtension(CnecResult.class).getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(750.0, crac.getCnec("Tieline BE FR - Défaut - N-1 NL1-NL3").getExtension(CnecResult.class).getFlowInA(), DOUBLE_TOLERANCE);
    */
    }

    @Test
    public void cracImportWithUnknownFieldInExtension() {
        /*try {
            Crac crac = CracImporters.importCrac("small-crac-errored.json", getClass().getResourceAsStream("/small-crac-errored.json"));
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }*/
    }
}
