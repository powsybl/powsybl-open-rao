/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator;

import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.CriticalBranchType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class CriticalBranchReaderTest {

    @Test
    public void isCrossZonalTest() {

        CriticalBranchType.Branch branch = new CriticalBranchType.Branch();

        // branch defined between French node and X node
        branch.setFrom("XNODE123");
        branch.setTo("FNODE123");
        assertTrue(CriticalBranchReader.isCrossZonal(branch));

        // branch defined between two French nodes, and whose name finishes with [XX] where
        // XX is a country code
        /*
         according to CORE CC, the '[FR]' at the end of the branch indicates that it is in series
         with a cross-border element, and that we must monitor the loop-flows on it.
         */
        branch.setFrom("FNODE123");
        branch.setTo("FNODE123");
        branch.setName("[FR-FR] My Branch Name [DIR] [FR]");
        assertTrue(CriticalBranchReader.isCrossZonal(branch));

        // variation of previous case
        branch.setFrom("ZNODE123");
        branch.setTo("ZNODE123");
        branch.setName("[PL-PL] My Branch Name [OPP][PL]");
        assertTrue(CriticalBranchReader.isCrossZonal(branch));

        // branch whose name finishes with [XX] where XX is not a country code
        branch.setName("[PL-PL] My Branch Name [OPP][AB]");
        assertFalse(CriticalBranchReader.isCrossZonal(branch));

        // branch whose name does not finish with [XX]
        branch.setName("[PL-PL] My Branch Name [OPP]");
        assertFalse(CriticalBranchReader.isCrossZonal(branch));
    }
}
