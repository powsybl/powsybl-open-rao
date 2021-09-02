/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.data.crac_creator_api.mock.CracCreatorMock;
import com.farao_community.farao.data.crac_creator_api.mock.NativeCracMock;
import com.farao_community.farao.data.crac_creator_api.parameters.CracCreatorParameters;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

import static com.farao_community.farao.data.crac_creator_api.CracCreators.*;
import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreatorsTest {

    private Network network;
    private OffsetDateTime offsetDateTime;

    @Before
    public void setUp() {
        network = Mockito.mock(Network.class);
        offsetDateTime = OffsetDateTime.parse("2020-01-01T01:00:00Z");
    }

    @Test
    public void testFindCreatorKnownFormat() {
        CracCreator cracCreator = findCreator("MockedNativeCracFormat");
        assertNotNull(cracCreator);
        assertTrue(cracCreator instanceof CracCreatorMock);
    }

    @Test
    public void testFindCreatorTestFormat() {
        CracCreator cracCreator = findCreator("UnknownFormat");
        assertNull(cracCreator);
    }

    @Test
    public void testCreateCrac() {
        CracCreationContext cracCreationContext = createCrac(new NativeCracMock(true), network, offsetDateTime);
        assertTrue(cracCreationContext.isCreationSuccessful());

        cracCreationContext = createCrac(new NativeCracMock(false), network, offsetDateTime);
        assertFalse(cracCreationContext.isCreationSuccessful());
    }

    @Test
    public void testCreateCracWithFactory() {
        CracCreationContext cracCreationContext = createCrac(new NativeCracMock(true), network, offsetDateTime, new CracCreatorParameters());
        assertTrue(cracCreationContext.isCreationSuccessful());
    }

    @Test
    public void testCreateAndImportCracFromInputStream() {
        CracCreationContext cracCreationContext = CracCreators.importAndCreateCrac("empty.txt", getClass().getResourceAsStream("/empty.txt"), network, offsetDateTime);
        assertTrue(cracCreationContext.isCreationSuccessful());
    }

    @Test
    public void testCreateAndImportCracFromPath() {
        CracCreationContext cracCreationContext = CracCreators.importAndCreateCrac(Paths.get(new File(getClass().getResource("/empty.txt").getFile()).getAbsolutePath()), network, offsetDateTime);
        assertTrue(cracCreationContext.isCreationSuccessful());
    }
}
