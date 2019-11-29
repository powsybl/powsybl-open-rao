/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_file.crac_merging;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import org.junit.Test;

import java.util.Arrays;

import static com.farao_community.farao.data.crac_file.crac_merging.CracMerging.merge;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test of CRAC merging function
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracMergingTest {

    @Test
    public void testMergeIdenticalFiles() {
        CracFile crac1 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample.json"));
        CracFile crac1Same = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample.json"));

        CracFile mergeIdentical = merge(Arrays.asList(crac1, crac1Same), "idTest1", "MergeIdentical");
        assertEquals(crac1.getSourceFormat(), mergeIdentical.getSourceFormat());
        assertEquals(crac1.getPreContingency().getMonitoredBranches().size(), mergeIdentical.getPreContingency().getMonitoredBranches().size());
        assertEquals(crac1.getContingencies().size(), mergeIdentical.getContingencies().size());
        assertEquals(crac1.getRemedialActions().size(), mergeIdentical.getRemedialActions().size());
    }

    @Test
    public void testMergeWithDifferentPreContingencies() {
        CracFile crac1 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample.json"));
        CracFile crac2 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample2.json"));

        CracFile mergePreCont = merge(Arrays.asList(crac1, crac2), "idTest2", "DifferentPreContingencies");
        assertEquals(7, mergePreCont.getPreContingency().getMonitoredBranches().size());
    }

    @Test
    public void testMergeWithDifferentContingencies() {
        CracFile crac1 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample.json"));
        CracFile crac3 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample3.json"));

        CracFile mergeCont = merge(Arrays.asList(crac1, crac3), "idTest3", "DifferentContingencies");
        assertEquals(crac1.getContingencies().size() + 1, mergeCont.getContingencies().size());
        assertEquals(crac1.getContingencies().get(1).getMonitoredBranches().size() + 1, mergeCont.getContingencies().get(1).getMonitoredBranches().size());
    }

    @Test
    public void testMergeWithDifferentRemedialActions() {
        CracFile crac1 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample.json"));
        CracFile crac4 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample4.json"));

        CracFile mergeRemedialActions = merge(Arrays.asList(crac1, crac4), "idTest4", "DifferentRemedialActions");
        assertEquals(3, mergeRemedialActions.getRemedialActions().size());
    }

    @Test
    public void testMergeWithDifferentFormatSourceField() {
        CracFile crac1 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample.json"));
        CracFile crac5 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample5.json"));

        CracFile mergeHeader = merge(Arrays.asList(crac1, crac5), "idTest5", "DifferentSourceFormat");
        assertEquals("Hybrid format", mergeHeader.getSourceFormat());
    }

    @Test
    public void testMergeWithEmptyDescription() {
        CracFile crac6 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample6.json"));
        CracFile crac6Same = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample6.json"));

        CracFile mergeHeader = merge(Arrays.asList(crac6, crac6Same), "idTest6", "Empty description");
        assertEquals("Merged CRAC.", mergeHeader.getDescription());
    }

    @Test
    public void testMergeWithSomeEmptyDescription() {
        CracFile crac1 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample.json"));
        CracFile crac6 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample6.json"));

        CracFile mergePartEmpty =  merge(Arrays.asList(crac6, crac1), "idTest7", "Partially empty description");
        assertEquals(
            "Merged CRAC: " + System.lineSeparator() + crac6.getName() + ": None" + System.lineSeparator() + crac1.getName() + ": " + crac1.getDescription(),
            mergePartEmpty.getDescription());
    }

    @Test
    public void testMergeNothing() {
        try {
            merge(Arrays.asList(), "idTest8", "Nothing");
            fail();
        } catch (FaraoException e) {
            // Should fail
        }
    }

    @Test
    public void testMergeOneCracFile() {
        CracFile crac1 = JsonCracFile.read(CracFile.class.getResourceAsStream("/cracFileExample.json"));

        CracFile mergeOne = merge(Arrays.asList(crac1), "idTest9", "OneCRAC");
        assertEquals(crac1.getSourceFormat(), mergeOne.getSourceFormat());
        assertEquals(crac1.getPreContingency().getMonitoredBranches().size(), mergeOne.getPreContingency().getMonitoredBranches().size());
        assertEquals(crac1.getContingencies().size(), mergeOne.getContingencies().size());
        assertEquals(crac1.getRemedialActions().size(), mergeOne.getRemedialActions().size());
    }
}
