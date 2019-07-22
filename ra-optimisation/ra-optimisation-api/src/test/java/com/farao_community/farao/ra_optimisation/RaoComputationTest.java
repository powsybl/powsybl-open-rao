/**
 * Copyright (c) 2017-2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.xlsx.model.TimesSeries;
import com.farao_community.farao.data.crac_file.xlsx.service.ImportService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class RaoComputationTest {

    private FileSystem fileSystem;

    private PlatformConfig platformConfig;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
    }

    @After
    public void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    public void run() {
        Assert.assertTrue(true);
    }

    @Test
    public void my_test() throws IOException {

        ImportService importService = new ImportService();
        CracFile cracFile = importService.importContacts(RaoComputationTest.class.getResourceAsStream("/exemple_crac_xlsx.xlsx"), TimesSeries.TIME_1830, "/exemple_crac_xlsx.xlsx");
        InputStream is = RaoComputationTest.class.getResourceAsStream("/networkTest.uct");
        Network net = Importers.loadNetwork("/networkTest.uct", is);
        RaoComputation rao = ComponentDefaultConfig.load().newFactoryImpl(RaoComputationFactory.class).create(net, cracFile, LocalComputationManager.getDefault(), 0);

        RaoComputationParameters parameters = RaoComputationParameters.load(platformConfig);
        RaoComputationResult result = rao.run(net.getVariantManager().getWorkingVariantId(), parameters).join();
    }
}
