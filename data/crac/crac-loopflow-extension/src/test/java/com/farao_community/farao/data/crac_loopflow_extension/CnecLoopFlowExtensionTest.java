/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Math.sqrt;
import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CnecLoopFlowExtensionTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    private Network network;
    private Cnec cnec;
    private double iMax;
    private double nominalV;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();
        crac.synchronize(network);
        cnec = crac.getCnec("cnec2basecase");

        iMax = 1500.0;
        nominalV = 380.0;
    }

    @Test
    public void basicSetterAndGetterTest() {
        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension(100, Unit.PERCENT_IMAX);

        cnecLoopFlowExtension.setLoopFlowConstraintInMW(200);
        Assert.assertEquals(200, cnecLoopFlowExtension.getLoopFlowConstraintInMW(), DOUBLE_TOLERANCE);
        Assert.assertEquals(100, cnecLoopFlowExtension.getInputThreshold(), DOUBLE_TOLERANCE);
        Assert.assertEquals(Unit.PERCENT_IMAX, cnecLoopFlowExtension.getInputThresholdUnit());
    }

    @Test
    public void convertFromPercent() {
        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension(50, Unit.PERCENT_IMAX);
        cnec.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);

        assertEquals(50, cnecLoopFlowExtension.getInputThreshold(Unit.PERCENT_IMAX, network), DOUBLE_TOLERANCE);
        assertEquals(0.5 * iMax, cnecLoopFlowExtension.getInputThreshold(Unit.AMPERE, network), DOUBLE_TOLERANCE);
        assertEquals(0.5 * iMax * nominalV * sqrt(3) / 1000, cnecLoopFlowExtension.getInputThreshold(Unit.MEGAWATT, network), DOUBLE_TOLERANCE);
    }

    @Test
    public void convertFromA() {
        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension(750, Unit.AMPERE);
        cnec.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);

        assertEquals(750 * 100 / iMax, cnecLoopFlowExtension.getInputThreshold(Unit.PERCENT_IMAX, network), DOUBLE_TOLERANCE);
        assertEquals(750, cnecLoopFlowExtension.getInputThreshold(Unit.AMPERE, network), DOUBLE_TOLERANCE);
        assertEquals(750 * nominalV * sqrt(3) / 1000, cnecLoopFlowExtension.getInputThreshold(Unit.MEGAWATT, network), DOUBLE_TOLERANCE);
    }

    @Test
    public void convertFromMW() {
        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension(1000, Unit.MEGAWATT);
        cnec.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);

        assertEquals(1000 * 1000 * 100 / (nominalV * sqrt(3) * iMax), cnecLoopFlowExtension.getInputThreshold(Unit.PERCENT_IMAX, network), DOUBLE_TOLERANCE);
        assertEquals(1000 * 1000 / (nominalV * sqrt(3)), cnecLoopFlowExtension.getInputThreshold(Unit.AMPERE, network), DOUBLE_TOLERANCE);
        assertEquals(1000, cnecLoopFlowExtension.getInputThreshold(Unit.MEGAWATT, network), DOUBLE_TOLERANCE);
    }
}
