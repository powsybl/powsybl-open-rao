/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystematicSensitivityAnalysisService.class, NativeLibraryLoader.class})
public class LinearRaoTest {

    private LinearRao linearRao;
    private ComputationManager computationManager;
    private RaoParameters raoParameters;
    private SystematicAnalysisEngine systematicAnalysisEngine;
    private LinearOptimisationEngine linearOptimisationEngine;
    private Network network;
    private Crac crac;
    private String variantId;

    @Before
    public void setUp() {
        mockNativeLibraryLoader();

        linearRao = Mockito.mock(LinearRao.class);

        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        variantId = network.getVariantManager().getWorkingVariantId();
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParameters.json"));
        computationManager = LocalComputationManager.getDefault();

        systematicAnalysisEngine = Mockito.mock(SystematicAnalysisEngine.class);
        linearOptimisationEngine = Mockito.mock(LinearOptimisationEngine.class);
    }

    private void mockNativeLibraryLoader() {
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        PowerMockito.doNothing().when(NativeLibraryLoader.class);
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    @Test
    public void runWithSensitivityComputationException() {
        assertNotNull(crac);
    }

    private static SimpleCrac create() {
        SimpleCrac crac = CommonCracCreation.create();

        // RAs
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");
        PstRange pstRange = new PstWithRange("RA PST BE", pstElement);
        crac.addRangeAction(pstRange);

        return crac;
    }
}
