/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class ClosedOptimisationRaoTest {
    @Test
    public void testConstructionWithMocks() {
        Network network = Mockito.mock(Network.class);
        CracFile cracFile = Mockito.mock(CracFile.class);
        ComputationManager computationManager = Mockito.mock(ComputationManager.class);
        LoadFlow.Runner loadflowRunner = Mockito.mock(LoadFlow.Runner.class);
        SensitivityComputationFactory sensitivityComputationFactory = Mockito.mock(SensitivityComputationFactory.class);
        mockNativeLibraryLoader();

        ClosedOptimisationRao closedOptimisationRao = new ClosedOptimisationRao(network, cracFile, computationManager, loadflowRunner, sensitivityComputationFactory);

        assertNotNull(closedOptimisationRao);
    }

    private void mockNativeLibraryLoader() {
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        PowerMockito.doNothing().when(NativeLibraryLoader.class);
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }
}
