/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.afs;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.powsybl.afs.*;
import com.powsybl.afs.ext.base.*;
import com.powsybl.afs.mapdb.storage.MapDbAppStorage;
import com.powsybl.afs.storage.AppStorage;
import com.powsybl.afs.storage.NodeGenericMetadata;
import com.powsybl.afs.storage.NodeInfo;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ReadOnlyMemDataSource;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.import_.ImportersLoader;
import com.powsybl.iidm.import_.ImportersLoaderList;
import com.powsybl.iidm.network.Network;
import com.farao_community.data.crac_file.afs.ImportedCracFile;
import com.farao_community.data.crac_file.afs.ImportedCracFileBuilder;
import com.farao_community.data.crac_file.afs.ImportedCracFileExtension;
import com.farao_community.farao.ra_optimisation.RaoComputationParameters;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.powsybl.iidm.network.VariantManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class RaoComputationRunnerTest extends AbstractProjectFileTest {


    private static RaoComputationResult createResult() {
        return new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
    }

    private static class RaoComputationServiceMock implements RaoComputationRunningService {

        @Override
        public void run(RaoComputationRunner runner) {
            runner.writeResult(createResult());
        }
    }

    @AutoService(ServiceExtension.class)
    public class RaoComputationServiceExtensionMock implements ServiceExtension<RaoComputationRunningService> {

        @Override
        public ServiceKey<RaoComputationRunningService> getServiceKey() {
            return new ServiceKey<>(RaoComputationRunningService.class, false);
        }

        @Override
        public RaoComputationRunningService createService(ServiceCreationContext context) {
            return new RaoComputationServiceMock();
        }
    }


    private class CracFileMemDataSource extends ReadOnlyMemDataSource {
        public CracFileMemDataSource() {
            putData("cracData", RaoComputationRunnerTest.class.getResourceAsStream("/cracFileExample.json"));
        }
    }


    private static class ImporterMock implements Importer {

        static final String FORMAT = "net";

        @Override
        public String getFormat() {
            return FORMAT;
        }

        @Override
        public String getComment() {
            return "";
        }

        @Override
        public boolean exists(ReadOnlyDataSource dataSource) {
            return true;
        }

        @Override
        public Network importData(ReadOnlyDataSource dataSource, Properties parameters) {
            Network network = Mockito.mock(Network.class);
            VariantManager variantManager = Mockito.mock(VariantManager.class);
            Mockito.when(variantManager.getWorkingVariantId()).thenReturn("s1");
            Mockito.when(network.getVariantManager()).thenReturn(variantManager);
            return network;
        }

        @Override
        public void copy(ReadOnlyDataSource fromDataSource, DataSource toDataSource) {
        }
    }


    private final ImportersLoader importersLoader = new ImportersLoaderList(Collections.singletonList(new ImporterMock()));

    @Override
    protected AppStorage createStorage() {
        return MapDbAppStorage.createHeap("mem");
    }

    @Override
    protected List<FileExtension> getFileExtensions() {
        return ImmutableList.of(new CaseExtension(importersLoader));
    }

    @Override
    protected List<ProjectFileExtension> getProjectFileExtensions() {
        return ImmutableList.of(new ImportedCaseExtension(importersLoader, new ImportConfig()),
                new ImportedCracFileExtension(),
                new RaoComputationRunnerExtension(new RaoComputationParameters()));
    }

    @Override
    protected List<ServiceExtension> getServiceExtensions() {
        return ImmutableList.of(new RaoComputationServiceExtensionMock(),
                new LocalNetworkCacheServiceExtension());
    }

    @Before
    public void setup() throws IOException {
        super.setup();

        // create network.net
        NodeInfo rootFolderInfo = storage.createRootNodeIfNotExists("root", Folder.PSEUDO_CLASS);
        storage.createNode(rootFolderInfo.getId(), "network", Case.PSEUDO_CLASS, "", Case.VERSION,
                new NodeGenericMetadata().setString(Case.FORMAT, ImporterMock.FORMAT));
    }

    @Test
    public void test() {
        Case aCase = afs.getRootFolder().getChild(Case.class, "network")
                .orElseThrow(AssertionError::new);

        // create project in the root folder
        Project project = afs.getRootFolder().createProject("project");

        // import network.net in root folder of the project
        ImportedCase importedCase = project.getRootFolder().fileBuilder(ImportedCaseBuilder.class)
                .withCase(aCase)
                .build();

        // create crac file
        ImportedCracFile importedCracFile = project.getRootFolder().fileBuilder(ImportedCracFileBuilder.class)
                .withName("cracFileExample")
                .withDataSource(new CracFileMemDataSource())
                .build();

        // create a security analysis runner that point to imported case
        RaoComputationRunner runner = project.getRootFolder().fileBuilder(RaoComputationRunnerBuilder.class)
                .withName("sa")
                .withCase(importedCase)
                .withCracFileProvider(importedCracFile)
                .build();

        // check there is no results
        assertNull(runner.readResult());

        // check default parameters can be changed
        RaoComputationParameters parameters = runner.readParameters();
        assertNotNull(parameters);
        assertFalse(parameters.getLoadFlowParameters().isSpecificCompatibility());
        parameters.getLoadFlowParameters().setSpecificCompatibility(true);
        runner.writeParameters(parameters);
        assertTrue(runner.readParameters().getLoadFlowParameters().isSpecificCompatibility());

        // run security analysis
        runner.run();

        // check results
        RaoComputationParameters result = runner.readParameters();
        assertNotNull(result);
        assertNotNull(result);

    }
}