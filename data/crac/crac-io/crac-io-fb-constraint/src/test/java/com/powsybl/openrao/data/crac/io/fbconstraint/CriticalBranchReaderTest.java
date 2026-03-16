/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.fbconstraint;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.CriticalBranchType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class CriticalBranchReaderTest {

    @Test
    void isCrossZonalTest() {

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

    @Test
    void testAddThresholdWithZeroThreshold() throws IOException {
        CracCreationParameters parameters = new CracCreationParameters();
        parameters.setCracFactoryName(CracFactory.findDefault().getName());
        parameters.addExtension(FbConstraintCracCreationParameters.class, new FbConstraintCracCreationParameters());

        // Test that if a threshold is 0, it is not used
        Network network = Network.read("TestCase12Nodes_with_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes_with_Xnodes.uct"));
        OffsetDateTime timestamp = OffsetDateTime.parse("2019-01-08T10:30Z");
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(timestamp);
        CracCreationContext creationContext = Crac.readWithContext("with_zero_limits.xml", getClass().getResourceAsStream("/merged_cb/with_zero_limits.xml"), network, parameters);
        Crac crac = creationContext.getCrac();

        // No ImaxFactor value, PermanentImaxA = 0 => use ImaxA = 10
        assertTrue(crac.getFlowCnec("BE_CBCO_000001 - preventive").getThresholds().stream().allMatch(branchThreshold -> branchThreshold.max().get() == 10.0));
        assertTrue(crac.getFlowCnec("BE_CBCO_000001 - preventive").getThresholds().stream().allMatch(branchThreshold -> branchThreshold.getUnit() == Unit.AMPERE));

        // permanentImaxFactor is 0, have no value for ImaxFactor, no value for PermanentImaxA => should use imaxA value = 20
        // Same temporaryImaxFactor is 0, have no value for ImaxFactor, no value for TemporaryImaxA => should use imaxA value = 20
        assertTrue(crac.getFlowCnec("BE_CBCO_000003 - curative").getThresholds().stream().allMatch(branchThreshold -> branchThreshold.max().get() == 20.0));
        assertTrue(crac.getFlowCnec("BE_CBCO_000003 - curative").getThresholds().stream().allMatch(branchThreshold -> branchThreshold.getUnit() == Unit.AMPERE));
        assertTrue(crac.getFlowCnec("BE_CBCO_000003 - outage").getThresholds().stream().allMatch(branchThreshold -> branchThreshold.max().get() == 20.0));
        assertTrue(crac.getFlowCnec("BE_CBCO_000003 - outage").getThresholds().stream().allMatch(branchThreshold -> branchThreshold.getUnit() == Unit.AMPERE));

        // permanentImaxFactor is 0, but we have a value for imaxFactor = -40 (negative because of direction==opposite)
        assertTrue(crac.getFlowCnec("BE_CBCO_000004 - curative").getThresholds().stream().allMatch(branchThreshold -> branchThreshold.min().get() == -40));
        assertTrue(crac.getFlowCnec("BE_CBCO_000004 - curative").getThresholds().stream().allMatch(branchThreshold -> branchThreshold.getUnit() == Unit.PERCENT_IMAX));

        // No thresholds info, the threshold should be set to -1 IMAX_percent (negative because of direction==opposite)
        assertTrue(crac.getFlowCnec("BE_CBCO_000002 - preventive").getThresholds().stream().allMatch(branchThreshold -> branchThreshold.min().get() == -1));
        assertTrue(crac.getFlowCnec("BE_CBCO_000002 - preventive").getThresholds().stream().allMatch(branchThreshold -> branchThreshold.getUnit() == Unit.PERCENT_IMAX));

    }
}
