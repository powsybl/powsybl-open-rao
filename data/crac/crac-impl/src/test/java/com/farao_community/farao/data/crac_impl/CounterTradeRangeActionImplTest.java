package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CounterTradeRangeActionImplTest {

    private Network network;
    private Crac crac;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = new CracImpl("test-crac");
    }

    @Test
    void applyTest() {
        CounterTradeRangeAction counterTradeRangeAction = (CounterTradeRangeAction) crac.newCounterTradeRangeAction()
                .withId("counterTradeRangeAction")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withExportingCountry(Country.FR)
                .add();
        Exception e = assertThrows(FaraoException.class, () -> counterTradeRangeAction.apply(network, 100.));
        assertEquals("Can't apply a counter trade range action on a network", e.getMessage());
    }

    @Test
    void getMinMaxAdmissibleSetpointTest() {
        CounterTradeRangeAction ira = (CounterTradeRangeActionImpl) crac.newCounterTradeRangeAction()
                .withId("injectionRangeActionId")
                .newRange().withMin(-1000).withMax(1000).add()
                .newRange().withMin(-1300).withMax(400).add()
                .withExportingCountry(Country.FR)
                .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        assertEquals(-1000, ira.getMinAdmissibleSetpoint(0.0), 1e-3);
        assertEquals(400, ira.getMaxAdmissibleSetpoint(0.0), 1e-3);
    }
}
