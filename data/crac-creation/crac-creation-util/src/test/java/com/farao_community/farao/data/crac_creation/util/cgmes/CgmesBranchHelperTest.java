/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.util.cgmes;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards{@literal <philippe.edwards at rte-france.com>}
 */
public class CgmesBranchHelperTest {

    private static Network network;
    private CgmesBranchHelper cgmesBranchHelper;

    @BeforeClass
    public static void setUp() {
        network = Importers.loadNetwork(new File(CgmesBranchHelperTest.class.getResource("/MicroGrid.zip").getFile()).toString());
    }

    @Test
    public void testValidNonTieLine() {
        cgmesBranchHelper = new CgmesBranchHelper("_ffbabc27-1ccd-4fdc-b037-e341706c8d29", network);
        assertTrue(cgmesBranchHelper.isValid());
        assertNotNull(cgmesBranchHelper.getBranch());
        assertFalse(cgmesBranchHelper.isTieLine());
        assertEquals("_ffbabc27-1ccd-4fdc-b037-e341706c8d29", cgmesBranchHelper.getIdInNetwork());
    }

    @Test
    public void testTieLineSideOne() {
        cgmesBranchHelper = new CgmesBranchHelper("_b18cd1aa-7808-49b9-a7cf-605eaf07b006", network);
        assertTrue(cgmesBranchHelper.isValid());
        assertNotNull(cgmesBranchHelper.getBranch());
        assertTrue(cgmesBranchHelper.isTieLine());
        assertSame(Branch.Side.ONE, cgmesBranchHelper.getTieLineSide());
        assertEquals("_b18cd1aa-7808-49b9-a7cf-605eaf07b006 + _e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", cgmesBranchHelper.getIdInNetwork());
    }

    @Test
    public void testTieLineSideTwo() {
        cgmesBranchHelper = new CgmesBranchHelper("_e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", network);
        assertTrue(cgmesBranchHelper.isValid());
        assertNotNull(cgmesBranchHelper.getBranch());
        assertTrue(cgmesBranchHelper.isTieLine());
        assertSame(Branch.Side.TWO, cgmesBranchHelper.getTieLineSide());
        assertEquals("_b18cd1aa-7808-49b9-a7cf-605eaf07b006 + _e8acf6b6-99cb-45ad-b8dc-16c7866a4ddc", cgmesBranchHelper.getIdInNetwork());
    }

    @Test
    public void testAbsentLine() {
        cgmesBranchHelper = new CgmesBranchHelper("_ffbabc27-1ccd-4fdc-b037-e341706c8d20", network);
        assertFalse(cgmesBranchHelper.isValid());
        assertEquals("Branch with id _ffbabc27-1ccd-4fdc-b037-e341706c8d20 was not found in network.", cgmesBranchHelper.getInvalidReason());
    }
}
