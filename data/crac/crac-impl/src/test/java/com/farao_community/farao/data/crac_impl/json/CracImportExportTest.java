/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.*;
import com.farao_community.farao.data.crac_impl.TapRangeImpl;
import com.farao_community.farao.data.crac_api.RangeType;
import com.farao_community.farao.data.crac_impl.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.threshold.*;
import com.farao_community.farao.data.crac_impl.FreeToUseImpl;
import com.farao_community.farao.data.crac_impl.OnStateImpl;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;

import static com.farao_community.farao.data.crac_api.TapConvention.CENTERED_ON_ZERO;
import static com.farao_community.farao.data.crac_impl.json.RoundTripUtil.roundTrip;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImportExportTest {

    @Test
    public void cracTest() {
        SimpleCrac simpleCrac = new SimpleCrac("cracId");

        Contingency contingency = simpleCrac.addContingency("contingencyId", "neId");
        simpleCrac.addContingency("contingency2Id", "neId1", "neId2");

        simpleCrac.addPreventiveCnec("cnec1prev", "neId1", "operator1",
            Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -500., null, BranchThresholdRule.ON_LEFT_SIDE)));

        Set<BranchThreshold> thresholds = new HashSet<>();
        thresholds.add(new BranchThresholdImpl(Unit.PERCENT_IMAX, -0.3, null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, -800., null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, -800., null, BranchThresholdRule.ON_HIGH_VOLTAGE_LEVEL));
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, null, 1200., BranchThresholdRule.ON_LOW_VOLTAGE_LEVEL));

        simpleCrac.addPreventiveCnec("cnec2prev", "neId2", "operator2", thresholds);
        simpleCrac.addCnec("cnec1cur", "neId1", "operator1",
            Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -800., null, BranchThresholdRule.ON_LEFT_SIDE)),
            contingency, Instant.OUTAGE);

        double positiveFrmMw = 20.0;
        BranchThreshold absoluteFlowThreshold = new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_LEFT_SIDE);
        Set<BranchThreshold> thresholdSet = new HashSet<>();
        thresholdSet.add(absoluteFlowThreshold);
        simpleCrac.addPreventiveCnec("cnec3prevId", "cnec3prevName", "neId2", "operator3",
            thresholdSet, positiveFrmMw, false, true);
        simpleCrac.addPreventiveCnec("cnec4prevId", "cnec4prevName", "neId2", "operator4",
            thresholdSet, 0.0, true, true);

        List<UsageRule> usageRules = new ArrayList<>();
        usageRules.add(new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE));
        usageRules.add(new OnStateImpl(UsageMethod.FORCED, simpleCrac.getState(contingency, Instant.OUTAGE)));

        simpleCrac.addNetworkElement(new NetworkElement("pst"));
        simpleCrac.addNetworkElement(new NetworkElement("injection"));

        // network action with one pst set point
        PstSetpointImpl pstSetpoint1 = new PstSetpointImpl(
            simpleCrac.getNetworkElement("pst"),
            15,
            CENTERED_ON_ZERO);

        simpleCrac.addNetworkAction(new NetworkActionImpl("pstSetpointRaId", "pstSetpointRaName", "RTE", usageRules, Set.of(pstSetpoint1)));

        // complex network action with one pst set point and one topology
        PstSetpointImpl pstSetpoint2 = new PstSetpointImpl(
                simpleCrac.getNetworkElement("pst"),
                5,
                CENTERED_ON_ZERO);

        TopologicalActionImpl topology = new TopologicalActionImpl(
                simpleCrac.getNetworkElement("neId"),
                ActionType.CLOSE);

        NetworkActionImpl complexNetworkAction = new NetworkActionImpl(
                "complexNetworkActionId",
                "complexNetworkActionName",
                "RTE",
                Collections.singletonList(new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE)),
                Set.of(pstSetpoint2, topology));

        simpleCrac.addNetworkAction(complexNetworkAction);

        // network action with one injection set point
        InjectionSetpointImpl injectionSetpoint = new InjectionSetpointImpl(
                simpleCrac.getNetworkElement("injection"),
                150);

        simpleCrac.addNetworkAction(new NetworkActionImpl("injectionSetpointRaId", "injectioSetpointRaName", "RTE", usageRules, Set.of(injectionSetpoint)));

        simpleCrac.addRangeAction(new PstRangeActionImpl(
                "pstRangeId",
                "pstRangeName",
                "RTE",
                Collections.singletonList(new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE)),
                Arrays.asList(new TapRangeImpl(0, 16, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE),
                        new TapRangeImpl(-3, 3, RangeType.RELATIVE_TO_INITIAL_NETWORK, CENTERED_ON_ZERO)),
                simpleCrac.getNetworkElement("pst")
        ));

        simpleCrac.addRangeAction(new PstRangeActionImpl(
                "pstRangeId2",
                "pstRangeName2",
                "RTE",
                Collections.singletonList(new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE)),
                Arrays.asList(new TapRangeImpl(0, 16, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE),
                        new TapRangeImpl(-3, 3, RangeType.RELATIVE_TO_INITIAL_NETWORK, CENTERED_ON_ZERO)),
                simpleCrac.addNetworkElement("pst2"),
                "1"
        ));

        simpleCrac.setNetworkDate(new DateTime(2020, 5, 14, 11, 35));

        simpleCrac.addContingency(new XnodeContingency("unsynced-xnode-cont-id", "unsynced-xnode-cont-name",
                Set.of("xnode1", "xnode2")));

        Crac crac = roundTrip(simpleCrac, SimpleCrac.class);

        assertEquals(6, crac.getNetworkElements().size());
        assertEquals(2, crac.getStates().size());
        assertEquals(3, crac.getContingencies().size());
        assertEquals(5, crac.getBranchCnecs().size());
        assertEquals(2, crac.getRangeActions().size());
        assertEquals(3, crac.getNetworkActions().size());
        assertEquals(4, crac.getBranchCnec("cnec2prev").getThresholds().size());
        assertFalse(crac.getBranchCnec("cnec3prevId").isOptimized());
        assertTrue(crac.getBranchCnec("cnec4prevId").isMonitored());
        assertEquals(1, crac.getNetworkAction("pstSetpointRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("pstSetpointRaId").getElementaryActions().iterator().next() instanceof PstSetpointImpl);
        assertEquals(1, crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().iterator().next() instanceof InjectionSetpointImpl);
        assertEquals(2, crac.getNetworkAction("complexNetworkActionId").getElementaryActions().size());
        assertEquals("1", crac.getRangeAction("pstRangeId2").getGroupId().orElseThrow());
        assertTrue(crac.getRangeAction("pstRangeId").getGroupId().isEmpty());

        assertEquals("operator1", crac.getBranchCnec("cnec1prev").getOperator());
        assertEquals("operator1", crac.getBranchCnec("cnec1cur").getOperator());
        assertEquals("operator2", crac.getBranchCnec("cnec2prev").getOperator());
        assertEquals("operator3", crac.getBranchCnec("cnec3prevId").getOperator());
        assertEquals("operator4", crac.getBranchCnec("cnec4prevId").getOperator());

        assertTrue(crac.getContingency("unsynced-xnode-cont-id") instanceof XnodeContingency);
        XnodeContingency xnodeContingency = (XnodeContingency) crac.getContingency("unsynced-xnode-cont-id");
        assertFalse(xnodeContingency.isSynchronized());
        assertEquals(2, xnodeContingency.getXnodeIds().size());
    }
}
