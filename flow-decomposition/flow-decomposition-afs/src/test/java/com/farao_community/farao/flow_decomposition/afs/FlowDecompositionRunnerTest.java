/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.afs;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.powsybl.afs.AbstractProjectFileTest;
import com.powsybl.afs.FileExtension;
import com.powsybl.afs.Folder;
import com.powsybl.afs.Project;
import com.powsybl.afs.ProjectFileExtension;
import com.powsybl.afs.ServiceCreationContext;
import com.powsybl.afs.ServiceExtension;
import com.powsybl.afs.ext.base.Case;
import com.powsybl.afs.ext.base.CaseExtension;
import com.powsybl.afs.ext.base.ImportedCase;
import com.powsybl.afs.ext.base.ImportedCaseBuilder;
import com.powsybl.afs.ext.base.ImportedCaseExtension;
import com.powsybl.afs.ext.base.LocalNetworkCacheServiceExtension;
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
import com.powsybl.iidm.network.VariantManager;
import com.farao_community.data.crac_file.afs.ImportedCracFile;
import com.farao_community.data.crac_file.afs.ImportedCracFileBuilder;
import com.farao_community.data.crac_file.afs.ImportedCracFileExtension;
import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowDecompositionRunnerTest extends AbstractProjectFileTest {

    private static FlowDecompositionResults createResult() {
        return new FlowDecompositionResults();
    }

    private static class FlowDecompositionServiceMock implements FlowDecompositionRunningService {

        @Override
        public void run(FlowDecompositionRunner runner) {
            runner.writeResult(createResult());
        }
    }

    @AutoService(ServiceExtension.class)
    public class FlowDecompositionServiceExtensionMock implements ServiceExtension<FlowDecompositionRunningService> {

        @Override
        public ServiceKey<FlowDecompositionRunningService> getServiceKey() {
            return new ServiceKey<>(FlowDecompositionRunningService.class, false);
        }

        @Override
        public FlowDecompositionRunningService createService(ServiceCreationContext context) {
            return new FlowDecompositionServiceMock();
        }
    }

    private class CracFileMemDataSource extends ReadOnlyMemDataSource {
        public CracFileMemDataSource() {
            putData("cracData", FlowDecompositionRunnerTest.class.getResourceAsStream("/cracFileExample.json"));
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
                new FlowDecompositionRunnerExtension(new FlowDecompositionParameters()));
    }

    @Override
    protected List<ServiceExtension> getServiceExtensions() {
        return ImmutableList.of(new FlowDecompositionServiceExtensionMock(),
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
    public void test() throws Exception {
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
                .withName("importedCracFile")
                .withDataSource(new CracFileMemDataSource())
                .withBaseName("cracData")
                .build();

        // create a flow decomposition runner that point to imported case
        FlowDecompositionRunner runner = project.getRootFolder().fileBuilder(FlowDecompositionRunnerBuilder.class)
                .withName("fd")
                .withCase(importedCase)
                .withCracFileProvider(importedCracFile)
                .build();

        // check there is no results
        assertFalse(runner.hasResult());
        assertNull(runner.readResult());

        // check default parameters can be changed
        FlowDecompositionParameters parameters = runner.readParameters();
        assertNotNull(parameters);
        assertFalse(parameters.getLoadFlowParameters().isSpecificCompatibility());
        parameters.getLoadFlowParameters().setSpecificCompatibility(true);
        runner.writeParameters(parameters);
        assertTrue(runner.readParameters().getLoadFlowParameters().isSpecificCompatibility());

        // run flow decomposition
        runner.run();

        // check results
        assertTrue(runner.hasResult());
    }
}
