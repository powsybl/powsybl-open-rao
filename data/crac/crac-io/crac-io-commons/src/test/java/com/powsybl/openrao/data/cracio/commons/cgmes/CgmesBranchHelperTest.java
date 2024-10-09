/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.cgmes;

import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards{@literal <philippe.edwards at rte-france.com>}
 */
class CgmesBranchHelperTest {

    private static Network network;
    private CgmesBranchHelper cgmesBranchHelper;

    @BeforeAll
    public static void setUp() {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        importParams.put("iidm.import.cgmes.cgm-with-subnetworks", false);
        network = Network.read(Paths.get(new File(CgmesBranchHelperTest.class.getResource("/MicroGrid.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }

    @Test
    void testValidNonTieLine() {
        cgmesBranchHelper = new CgmesBranchHelper("_ffbabc27-1ccd-4fdc-b037-e341706c8d29", network);
        assertTrue(cgmesBranchHelper.isValid());
        assertNotNull(cgmesBranchHelper.getBranch());
        assertFalse(cgmesBranchHelper.isHalfLine());
        assertEquals("_ffbabc27-1ccd-4fdc-b037-e341706c8d29", cgmesBranchHelper.getIdInNetwork());
    }

    @Test
    void testTieLineSideOne() {
        cgmesBranchHelper = new CgmesBranchHelper("_b18cd1aa-7808-49b9-a7cf-605eaf07b006", network);
        assertTrue(cgmesBranchHelper.isValid());
        assertNotNull(cgmesBranchHelper.getBranch());
        assertTrue(cgmesBranchHelper.isHalfLine());
        assertSame(TwoSides.ONE, cgmesBranchHelper.getTieLineSide());
        assertEquals("_b18cd1aa-7808-49b9-a7cf-605eaf07b006 + _e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", cgmesBranchHelper.getIdInNetwork());
    }

    @Test
    void testTieLineSideTwo() {
        cgmesBranchHelper = new CgmesBranchHelper("_e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", network);
        assertTrue(cgmesBranchHelper.isValid());
        assertNotNull(cgmesBranchHelper.getBranch());
        assertTrue(cgmesBranchHelper.isHalfLine());
        assertSame(TwoSides.TWO, cgmesBranchHelper.getTieLineSide());
        assertEquals("_b18cd1aa-7808-49b9-a7cf-605eaf07b006 + _e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", cgmesBranchHelper.getIdInNetwork());
    }

    @Test
    void testAbsentLine() {
        cgmesBranchHelper = new CgmesBranchHelper("_ffbabc27-1ccd-4fdc-b037-e341706c8d20", network);
        assertFalse(cgmesBranchHelper.isValid());
        assertEquals("Branch with id _ffbabc27-1ccd-4fdc-b037-e341706c8d20 was not found in network.", cgmesBranchHelper.getInvalidReason());
    }
}
