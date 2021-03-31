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
import com.farao_community.farao.data.crac_impl.range_domain.PstRangeImpl;
import com.farao_community.farao.data.crac_api.RangeType;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.*;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.threshold.*;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUseImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.OnStateImpl;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;

import static com.farao_community.farao.data.crac_api.RangeDefinition.CENTERED_ON_ZERO;
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
        SimpleCrac simpleCrac = new SimpleCrac("cracId", "cracId", Set.of(Instant.OUTAGE));

        String preventiveStateId = simpleCrac.getPreventiveState().getId();

        Contingency contingency = simpleCrac.addContingency("contingencyId", "neId");
        simpleCrac.addContingency("contingency2Id", "neId1", "neId2");

        simpleCrac.addCnec("cnec1prev", "neId1", "operator1",
            Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -500., null, BranchThresholdRule.ON_LEFT_SIDE)),
            preventiveStateId);

        Set<BranchThreshold> thresholds = new HashSet<>();
        thresholds.add(new BranchThresholdImpl(Unit.PERCENT_IMAX, -0.3, null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, -800., null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, -800., null, BranchThresholdRule.ON_HIGH_VOLTAGE_LEVEL));
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, null, 1200., BranchThresholdRule.ON_LOW_VOLTAGE_LEVEL));

        simpleCrac.addCnec("cnec2prev", "neId2", "operator2", thresholds, preventiveStateId);
        simpleCrac.addCnec("cnec1cur", "neId1", "operator1",
            Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -800., null, BranchThresholdRule.ON_LEFT_SIDE)),
            simpleCrac.getState(contingency, Instant.OUTAGE).getId());

        double positiveFrmMw = 20.0;
        BranchThreshold absoluteFlowThreshold = new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_LEFT_SIDE);
        Set<BranchThreshold> thresholdSet = new HashSet<>();
        thresholdSet.add(absoluteFlowThreshold);
        simpleCrac.addCnec("cnec3prevId", "cnec3prevName", "neId2", "operator3",
            thresholdSet, preventiveStateId, positiveFrmMw, false, true);
        simpleCrac.addCnec("cnec4prevId", "cnec4prevName", "neId2", "operator4",
            thresholdSet, preventiveStateId, 0.0, true, true);

        List<UsageRule> usageRules = new ArrayList<>();
        usageRules.add(new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE));
        usageRules.add(new OnStateImpl(UsageMethod.FORCED, simpleCrac.getState(contingency, Instant.OUTAGE)));

        simpleCrac.addNetworkElement(new NetworkElement("pst"));
        simpleCrac.addNetworkElement(new NetworkElement("injection"));
        simpleCrac.addNetworkAction(new PstSetpoint("pstSetpointId", "pstSetpointName", "RTE", usageRules, simpleCrac.getNetworkElement("pst"), 15, CENTERED_ON_ZERO));

        Set<AbstractElementaryNetworkAction> elementaryNetworkActions = new HashSet<>();
        PstSetpoint pstSetpoint = new PstSetpoint(
                "pstSetpointId",
                "pstSetpointName",
                "RTE",
                new ArrayList<>(),
                simpleCrac.getNetworkElement("pst"),
                5,
                CENTERED_ON_ZERO
        );
        Topology topology = new Topology(
                "topologyId",
                "topologyName",
                "RTE",
                new ArrayList<>(),
                simpleCrac.getNetworkElement("neId"),
                ActionType.CLOSE
        );
        elementaryNetworkActions.add(pstSetpoint);
        elementaryNetworkActions.add(topology);
        ComplexNetworkAction complexNetworkAction = new ComplexNetworkAction(
                "complexNetworkActionId",
                "complexNetworkActionName",
                "RTE",
                new ArrayList<>(),
                elementaryNetworkActions
        );
        simpleCrac.addNetworkAction(complexNetworkAction);

        InjectionSetpoint injectionSetpoint = new InjectionSetpoint(
                "injectionSetpointId",
                "injectioSetpointName",
                "RTE",
                new ArrayList<>(),
                simpleCrac.getNetworkElement("injection"),
                150
        );
        simpleCrac.addNetworkAction(injectionSetpoint);

        simpleCrac.addRangeAction(new PstRangeActionImpl(
                "pstRangeId",
                "pstRangeName",
                "RTE",
                Collections.singletonList(new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE)),
                Arrays.asList(new PstRangeImpl(0, 16, RangeType.ABSOLUTE, RangeDefinition.STARTS_AT_ONE),
                        new PstRangeImpl(-3, 3, RangeType.RELATIVE_TO_INITIAL_NETWORK, CENTERED_ON_ZERO)),
                simpleCrac.getNetworkElement("pst")
        ));

        simpleCrac.addRangeAction(new PstRangeActionImpl(
                "pstRangeId2",
                "pstRangeName2",
                "RTE",
                Collections.singletonList(new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE)),
                Arrays.asList(new PstRangeImpl(0, 16, RangeType.ABSOLUTE, RangeDefinition.STARTS_AT_ONE),
                        new PstRangeImpl(-3, 3, RangeType.RELATIVE_TO_INITIAL_NETWORK, CENTERED_ON_ZERO)),
                simpleCrac.addNetworkElement("pst2"),
                "1"
        ));

        simpleCrac.setNetworkDate(new DateTime(2020, 5, 14, 11, 35));

        simpleCrac.addContingency(new XnodeContingency("unsynced-xnode-cont-id", "unsynced-xnode-cont-name",
                Set.of("xnode1", "xnode2")));

        Crac crac = roundTrip(simpleCrac, SimpleCrac.class);

        assertEquals(6, crac.getNetworkElements().size());
        assertEquals(2, crac.getInstants().size());
        assertEquals(3, crac.getContingencies().size());
        assertEquals(5, crac.getBranchCnecs().size());
        assertEquals(2, crac.getRangeActions().size());
        assertEquals(3, crac.getNetworkActions().size());
        assertEquals(4, crac.getBranchCnec("cnec2prev").getThresholds().size());
        assertFalse(crac.getBranchCnec("cnec3prevId").isOptimized());
        assertTrue(crac.getBranchCnec("cnec4prevId").isMonitored());
        assertTrue(crac.getNetworkAction("pstSetpointId") instanceof PstSetpoint);
        assertTrue(crac.getNetworkAction("injectionSetpointId") instanceof InjectionSetpoint);
        assertEquals("1", crac.getRangeAction("pstRangeId2").getGroupId().orElseThrow());
        assertTrue(crac.getRangeAction("pstRangeId").getGroupId().isEmpty());
        assertEquals(CENTERED_ON_ZERO, ((PstSetpoint) crac.getNetworkAction("pstSetpointId")).getRangeDefinition());

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
