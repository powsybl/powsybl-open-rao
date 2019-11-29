/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.afs.crac_file.xlsx;

import com.farao_community.farao.commons.FaraoException;
import com.google.common.collect.ImmutableList;
import com.powsybl.afs.*;
import com.powsybl.afs.mapdb.storage.MapDbAppStorage;
import com.powsybl.afs.storage.AppStorage;
import com.powsybl.commons.datasource.ReadOnlyMemDataSource;
import org.junit.Test;

import java.io.*;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ImportedXlsxCracFileTest extends AbstractProjectFileTest {

    InputStream cracFileTest;

    @Override
    protected AppStorage createStorage() {
        return MapDbAppStorage.createHeap("mem");
    }

    @Override
    protected List<FileExtension> getFileExtensions() {
        return ImmutableList.of(new AfsXlsxCracFileExtension());
    }

    @Override
    protected List<ProjectFileExtension> getProjectFileExtensions() {
        return ImmutableList.of(new ImportedXlsxCracFileExtension());
    }

    @Test
    public void test() {
        Folder root = afs.getRootFolder();

        // check AfsXlsxCracFile exists
        ReadOnlyMemDataSource dataSource = new ReadOnlyMemDataSource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx");
        dataSource.putData("/20170215_xlsx_crac_fr_v01_v2.3.xlsx", ImportedXlsxCracFileTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v01_v2.3.xlsx"));
        assertTrue(!dataSource.getBaseName().isEmpty());

        // create project
        Project project = root.createProject("project");
        assertNotNull(project);

        // create project folder
        ProjectFolder folder = project.getRootFolder().createFolder("folder");
        assertTrue(folder.getChildren().isEmpty());

        // import CRAC into project
        try {
            folder.fileBuilder(ImportedXlsxCracFileBuilder.class)
                    .build();
            fail();
        } catch (FaraoException ignored) {
            //expected
        }
        ImportedXlsxCracFile importedCracFile = folder.fileBuilder(ImportedXlsxCracFileBuilder.class)
                .withName("cracFileExample")
                .withDataSource(dataSource)
                .withHour("TIME_1030")
                .build();
        assertNotNull(importedCracFile);
        assertFalse(importedCracFile.isFolder());
        assertNotNull(importedCracFile.getCracFile());
        assertTrue(importedCracFile.getDependencies().isEmpty());

        // try to reload the imported CRAC
        assertEquals(1, folder.getChildren().size());
        ProjectNode projectNode = folder.getChildren().get(0);
        assertNotNull(projectNode);
        assertTrue(projectNode instanceof ImportedXlsxCracFile);

        assertTrue(folder.getChild(ImportedXlsxCracFile.class, "cracFileExample_1030").isPresent());

        // delete imported CRAC
        projectNode.delete();
        assertTrue(folder.getChildren().isEmpty());
    }

    @Test
    public void testImportWithBaseName() {
        Folder root = afs.getRootFolder();

        // check AfsXlsxCracFile exists
        ReadOnlyMemDataSource dataSource = new ReadOnlyMemDataSource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx");
        dataSource.putData("otherBaseName.file", ImportedXlsxCracFileTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v01_v2.3.xlsx"));
        assertTrue(!dataSource.getBaseName().isEmpty());

        // create project
        Project project = root.createProject("project");
        assertNotNull(project);

        // create project folder
        ProjectFolder folder = project.getRootFolder().createFolder("folder");
        assertTrue(folder.getChildren().isEmpty());

        // import CRAC into project
        try {
            folder.fileBuilder(ImportedXlsxCracFileBuilder.class)
                    .withName("cracFileExample")
                    .withDataSource(dataSource)
                    .withHour("TIME_1030")
                    .build();
            fail();
        } catch (FaraoException expected) {
            //expected
        }
        ImportedXlsxCracFile importedCracFile = folder.fileBuilder(ImportedXlsxCracFileBuilder.class)
                .withName("cracFileExample")
                .withDataSource(dataSource)
                .withHour("TIME_1030")
                .withBaseName("otherBaseName.file")
                .build();
        assertNotNull(importedCracFile);
        assertFalse(importedCracFile.isFolder());
        assertNotNull(importedCracFile.getCracFile());
        assertTrue(importedCracFile.getDependencies().isEmpty());

        // try to reload the imported CRAC
        assertEquals(1, folder.getChildren().size());
        ProjectNode projectNode = folder.getChildren().get(0);
        assertNotNull(projectNode);
        assertTrue(projectNode instanceof ImportedXlsxCracFile);

        assertTrue(folder.getChild(ImportedXlsxCracFile.class, "cracFileExample_1030").isPresent());

        // delete imported CRAC
        projectNode.delete();
        assertTrue(folder.getChildren().isEmpty());
    }

    @Test
    public void throwsWhenLackingName() {
        Folder root = afs.getRootFolder();

        // check AfsXlsxCracFile exists
        ReadOnlyMemDataSource dataSource = new ReadOnlyMemDataSource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx");
        dataSource.putData("/20170215_xlsx_crac_fr_v01_v2.3.xlsx", ImportedXlsxCracFileTest.class.getResourceAsStream("/20170215_xlsx_crac_fr_v01_v2.3.xlsx"));
        assertTrue(!dataSource.getBaseName().isEmpty());

        // create project
        Project project = root.createProject("project");
        assertNotNull(project);

        // create project folder
        ProjectFolder folder = project.getRootFolder().createFolder("folder");
        assertTrue(folder.getChildren().isEmpty());

        // import CRAC into project
        try {
            folder.fileBuilder(ImportedXlsxCracFileBuilder.class)
                    .withDataSource(dataSource)
                    .withHour("TIME_1030")
                    .build();
            fail();
        } catch (FaraoException expected) {
            //expected
        }
    }
}
