/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.data.crac_file.afs;

import com.google.common.collect.ImmutableList;
import com.powsybl.afs.AbstractProjectFileTest;
import com.powsybl.afs.FileExtension;
import com.powsybl.afs.Folder;
import com.powsybl.afs.Project;
import com.powsybl.afs.ProjectFileExtension;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ProjectNode;
import com.powsybl.afs.mapdb.storage.MapDbAppStorage;
import com.powsybl.afs.storage.AppStorage;
import com.farao_community.farao.commons.FaraoException;
import com.powsybl.commons.datasource.ReadOnlyMemDataSource;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ImportedCracFileTest extends AbstractProjectFileTest {

    @Override
    protected AppStorage createStorage() {
        return MapDbAppStorage.createHeap("mem");
    }

    @Override
    protected List<FileExtension> getFileExtensions() {
        return ImmutableList.of(new AfsCracFileExtension());
    }

    @Override
    protected List<ProjectFileExtension> getProjectFileExtensions() {
        return ImmutableList.of(new ImportedCracFileExtension());
    }

    @Test
    public void test() {
        Folder root = afs.getRootFolder();

        // check AfsCracFile exists
        ReadOnlyMemDataSource dataSource = new ReadOnlyMemDataSource("/cracFileExampleValid.json");
        dataSource.putData("/cracFileExampleValid.json", ImportedCracFileTest.class.getResourceAsStream("/cracFileExampleValid.json"));
        assertTrue(!dataSource.getBaseName().isEmpty());

        // create project
        Project project = root.createProject("project");
        assertNotNull(project);

        // create project folder
        ProjectFolder folder = project.getRootFolder().createFolder("folder");
        assertTrue(folder.getChildren().isEmpty());

        // import CRAC into project
        try {
            folder.fileBuilder(ImportedCracFileBuilder.class)
                    .build();
            fail();
        } catch (FaraoException ignored) {
        }
        ImportedCracFile importedCracFile = folder.fileBuilder(ImportedCracFileBuilder.class)
                .withName("cracFileExample")
                .withDataSource(dataSource)
                .build();
        assertNotNull(importedCracFile);
        assertFalse(importedCracFile.isFolder());
        assertNotNull(importedCracFile.getCracFile());
        assertTrue(importedCracFile.getDependencies().isEmpty());

        // try to reload the imported CRAC
        assertEquals(1, folder.getChildren().size());
        ProjectNode projectNode = folder.getChildren().get(0);
        assertNotNull(projectNode);
        assertTrue(projectNode instanceof ImportedCracFile);
        ImportedCracFile importedCase2 = (ImportedCracFile) projectNode;

        assertTrue(folder.getChild(ImportedCracFile.class, "cracFileExample").isPresent());

        // delete imported CRAC
        projectNode.delete();
        assertTrue(folder.getChildren().isEmpty());
    }

    @Test
    public void testImportWithBaseName() {
        Folder root = afs.getRootFolder();

        // check AfsCracFile exists
        ReadOnlyMemDataSource dataSource = new ReadOnlyMemDataSource("/cracFileExampleValid.json");
        dataSource.putData("otherBaseName.file", ImportedCracFileTest.class.getResourceAsStream("/cracFileExampleValid.json"));
        assertTrue(!dataSource.getBaseName().isEmpty());

        // create project
        Project project = root.createProject("project");
        assertNotNull(project);

        // create project folder
        ProjectFolder folder = project.getRootFolder().createFolder("folder");
        assertTrue(folder.getChildren().isEmpty());

        // Try without baseName, should not work
        try {
            folder.fileBuilder(ImportedCracFileBuilder.class)
                    .withName("cracFileExample")
                    .withDataSource(dataSource)
                    .build();
            fail();
        } catch (FaraoException expected) {
            //expected
        }
        ImportedCracFile importedCracFile = folder.fileBuilder(ImportedCracFileBuilder.class)
                .withName("cracFileExample")
                .withDataSource(dataSource)
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
        assertTrue(projectNode instanceof ImportedCracFile);
        ImportedCracFile importedCase2 = (ImportedCracFile) projectNode;

        assertTrue(folder.getChild(ImportedCracFile.class, "cracFileExample").isPresent());

        // delete imported CRAC
        projectNode.delete();
        assertTrue(folder.getChildren().isEmpty());
    }

    @Test
    public void throwsWhenLackingName() {
        Folder root = afs.getRootFolder();

        // check AfsCracFile exists
        ReadOnlyMemDataSource dataSource = new ReadOnlyMemDataSource("/cracFileExampleValid.json");
        dataSource.putData("/cracFileExampleValid.json", ImportedCracFileTest.class.getResourceAsStream("/cracFileExampleValid.json"));
        assertTrue(!dataSource.getBaseName().isEmpty());

        // create project
        Project project = root.createProject("project");
        assertNotNull(project);

        // create project folder
        ProjectFolder folder = project.getRootFolder().createFolder("folder");
        assertTrue(folder.getChildren().isEmpty());

        // Try without baseName, should not work
        try {
            folder.fileBuilder(ImportedCracFileBuilder.class)
                    .withDataSource(dataSource)
                    .build();
            fail();
        } catch (FaraoException expected) {
            //expected
        }
    }
}
