/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.importer;

import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.native_crac_io_api.NativeCracImporter;
import com.google.auto.service.AutoService;
import com.powsybl.triplestore.api.TripleStore;
import com.powsybl.triplestore.api.TripleStoreFactory;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
@AutoService(NativeCracImporter.class)
public class CsaProfileCracImporter implements NativeCracImporter<CsaProfileCrac> {

    @Override
    public String getFormat() {
        return "CsaProfileCrac";
    }

    /**
     * @param inputStream : zip file inputStream
     * @return csa profile native crac, the tripleStore contains data of every rdf file included in the zip
     * each context of the tripleStore contains one rdf file data
     */
    @Override
    public CsaProfileCrac importNativeCrac(InputStream inputStream) {
        TripleStore tripleStoreCsaProfile = TripleStoreFactory.create(CsaProfileConstants.TRIPLESTORE_RDF4J_NAME);
        ZipEntry zipEntry;
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            //max number of entries and max size of entry are checked to avoid ddos attack with malicious zip file
            //TODO parametrization for gridcapa_swe_csa service
            int maxNbEntries = 200;
            int maxSizeEntry = 1_000_000_000;
            int countEntries = 0;
            while ((zipEntry = zipInputStream.getNextEntry()) != null && countEntries < maxNbEntries) {
                countEntries++;
                if (!zipEntry.isDirectory()) {
                    FaraoLoggerProvider.BUSINESS_LOGS.info("csa profile crac import : import of file {}", zipEntry.getName());
                    int currentSizeEntry = 0;
                    File tempFile = File.createTempFile("faraoCsaProfile", ".tmp");
                    boolean tempFileOk = tempFile.setReadable(true, true) &&
                        tempFile.setWritable(true, true);
                    if (tempFileOk) {
                        InputStream in = new BufferedInputStream(zipInputStream);
                        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                            int nBytes = -1;
                            byte[] buffer = new byte[2048];

                            while ((nBytes = in.read(buffer)) > 0 && currentSizeEntry < maxSizeEntry) {
                                out.write(buffer, 0, nBytes);
                                currentSizeEntry += nBytes;

                            }
                        }
                        FileInputStream fileInputStream = new FileInputStream(tempFile);
                        tripleStoreCsaProfile.read(fileInputStream, CsaProfileConstants.RDF_BASE_URL, zipEntry.getName());
                    }
                    if (!tempFile.delete()) {
                        FaraoLoggerProvider.TECHNICAL_LOGS.warn("temporary file for csa profile crac import can't be deleted");
                        tempFile.deleteOnExit();
                    }
                }
            }
        } catch (IOException e) {
            FaraoLoggerProvider.TECHNICAL_LOGS.error("csa profile crac import interrupted, cause : {}", e.getMessage());
        }

        return new CsaProfileCrac(tripleStoreCsaProfile);
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        TripleStore tripleStoreCsaProfile = TripleStoreFactory.create(CsaProfileConstants.TRIPLESTORE_RDF4J_NAME);
        tripleStoreCsaProfile.read(inputStream, CsaProfileConstants.RDF_BASE_URL, "");
        return FilenameUtils.getExtension(fileName).equals(CsaProfileConstants.EXTENSION_FILE_CSA_PROFILE);
    }
}
