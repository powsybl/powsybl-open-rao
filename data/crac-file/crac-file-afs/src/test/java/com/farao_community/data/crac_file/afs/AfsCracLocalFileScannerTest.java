/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.data.crac_file.afs;

import com.powsybl.afs.local.storage.LocalFile;
import com.powsybl.afs.local.storage.LocalFileScannerContext;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class AfsCracLocalFileScannerTest {

    private JSONObject jsonSchema;

    @Before
    public void setUp() {
        // given
        jsonSchema = new JSONObject(
                new JSONTokener(AfsCracLocalFileScanner.class.getResourceAsStream("/CracFileSchema.json")));
    }

    @After
    public void tearDown() {
        jsonSchema = null;
    }

    @Test
    public void shouldBeScanCracFileWhenCracFileIsValid() {
        //Given
        AfsCracLocalFileScanner fileScanner = new AfsCracLocalFileScanner();
        //Action
        LocalFile afsCracLocalFile = fileScanner.scanFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()), Mockito.mock(LocalFileScannerContext.class));
        //Asserts
        assertNotNull(afsCracLocalFile);
        assertEquals("cracFile", afsCracLocalFile.getPseudoClass());
        assertEquals("cracFileExampleValid", afsCracLocalFile.getName());
    }

    @Test
    public void shouldBeNotScanCracFileWhenCracFileIsInvalid() {
        //Given
        AfsCracLocalFileScanner fileScanner = new AfsCracLocalFileScanner();
        //Action
        LocalFile afsCracLocalFile = fileScanner.scanFile(Paths.get(getClass().getResource("/cracFileExampleInvalid.json").getPath()), Mockito.mock(LocalFileScannerContext.class));
        //Asserts
        assertNull(afsCracLocalFile);
    }

    @Test
    public void shouldBeValidCracFileWhenCracFileIsValid() {
        //Given
        JSONObject jsonSubject = new JSONObject(
                new JSONTokener(AfsCracLocalFileScannerTest.class.getResourceAsStream("/cracFileExampleValid.json")));
        Schema schema = SchemaLoader.load(jsonSchema);
        //Action
        schema.validate(jsonSubject);
    }

    @Test(expected = ValidationException.class)
    public void shouldThrowExceptionIfNotValidated() {
        //Given
        JSONObject jsonSubject = new JSONObject(
                new JSONTokener(AfsCracLocalFileScannerTest.class.getResourceAsStream("/cracFileExampleInvalid.json")));
        Schema schema = SchemaLoader.load(jsonSchema);
        //Action
        schema.validate(jsonSubject);
    }
}
