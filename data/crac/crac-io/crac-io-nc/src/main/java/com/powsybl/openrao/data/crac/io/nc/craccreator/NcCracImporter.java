/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.io.Importer;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.nc.NcCrac;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcKeyword;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcVersion;
import com.powsybl.triplestore.api.TripleStore;
import com.powsybl.triplestore.api.TripleStoreFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
@AutoService(Importer.class)
public class NcCracImporter implements Importer {

    @Override
    public String getFormat() {
        return "NC";
    }

    /**
     * Default maximum number of entries of a NC zip package, checked to avoid a ddos attack with a malicious zip file.
     * A caller that has to import a larger package (e.g. a full multi-region test package) can raise it
     * through {@link #importNativeCrac(InputStream, int)}.
     */
    public static final int DEFAULT_MAX_NB_ZIP_ENTRIES = 200;

    /**
     * @param inputStream : zip file inputStream
     * @return nc native crac, the tripleStore contains data of every rdf file included in the zip
     * each context of the tripleStore contains one rdf file data
     */
    NcCrac importNativeCrac(InputStream inputStream) {
        return importNativeCrac(inputStream, DEFAULT_MAX_NB_ZIP_ENTRIES);
    }

    NcCrac importNativeCrac(InputStream inputStream, int maxNbEntries) {
        TripleStore tripleStoreNcProfile = TripleStoreFactory.create(NcConstants.TRIPLESTORE_RDF4J_NAME);
        ZipEntry zipEntry;
        Map<String, Set<String>> keywordMap = new HashMap<>();
        Set<String> ncp25Files = new LinkedHashSet<>();
        Set<String> ncp25ConformsToFiles = new LinkedHashSet<>();
        Set<String> ncp24ConformsToFiles = new LinkedHashSet<>();
        Pattern keywordPattern = Pattern.compile("<dcat:keyword>([A-Z]{2,3})</dcat:keyword>");
        Pattern ncp25Pattern = Pattern.compile(NcConstants.NCP25_NAMESPACE_PATTERN);
        Pattern ncp25ConformsToPattern = Pattern.compile(NcConstants.NCP25_CONFORMS_TO_PATTERN);
        Pattern ncp24ConformsToPattern = Pattern.compile(NcConstants.NCP24_CONFORMS_TO_PATTERN);
        int maxSizeEntry = 1_000_000_000;
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            int countEntries = 0;
            while ((zipEntry = zipInputStream.getNextEntry()) != null && countEntries < maxNbEntries) { //NOSONAR
                countEntries++;
                if (!zipEntry.isDirectory()) {
                    importZipEntry(zipEntry, zipInputStream, new NcZipImportContext(
                        maxSizeEntry, keywordPattern, ncp25Pattern, ncp25ConformsToPattern, ncp24ConformsToPattern,
                        keywordMap, ncp25Files, ncp25ConformsToFiles, ncp24ConformsToFiles, tripleStoreNcProfile));
                }
            }
            if (zipInputStream.getNextEntry() != null) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("NC crac import: the zip contains more than {} entries, "
                    + "the remaining entries are not imported", maxNbEntries);
            }
        } catch (IOException e) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.error("NC crac import interrupted, cause : {}", e.getMessage());
        }
        NcVersion ncVersion = NcVersionDetector.detectVersion(ncp25Files, ncp25ConformsToFiles, ncp24ConformsToFiles);
        OpenRaoLoggerProvider.BUSINESS_LOGS.info("NC crac import: detected NCP version {}", ncVersion.getLabel());
        return new NcCrac(tripleStoreNcProfile, keywordMap, ncVersion);
    }

    /**
     * Shared state of the NC zip import, passed to every entry so that the per-entry method
     * stays below the parameter count and cognitive complexity thresholds.
     */
    private record NcZipImportContext(int maxSizeEntry,
                                      Pattern keywordPattern,
                                      Pattern ncp25Pattern,
                                      Pattern ncp25ConformsToPattern,
                                      Pattern ncp24ConformsToPattern,
                                      Map<String, Set<String>> keywordMap,
                                      Set<String> ncp25Files,
                                      Set<String> ncp25ConformsToFiles,
                                      Set<String> ncp24ConformsToFiles,
                                      TripleStore tripleStoreNcProfile) {
    }

    private static void importZipEntry(ZipEntry zipEntry,
                                       ZipInputStream zipInputStream,
                                       NcZipImportContext context) throws IOException {
        OpenRaoLoggerProvider.BUSINESS_LOGS.info("NC crac import : import of file {}", zipEntry.getName());
        File tempFile;
        boolean tempFileOk;

        if (SystemUtils.IS_OS_UNIX) {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
            tempFile = Files.createTempFile("openRaoNc", ".tmp", attr).toFile(); // Compliant
            tempFileOk = true;
        } else {
            tempFile = Files.createTempFile("prefix", "suffix").toFile();  //NOSONAR
            //sonar wants us to set readable and writable right after creating file
            //but it counts it as a bug if you don't use the return variable
            //and doesn't see the calls if you use the return variable...
            tempFileOk = tempFile.setReadable(true, true) &&
                tempFile.setWritable(true, true) &&
                tempFile.setExecutable(true, true);
        }
        if (tempFileOk) {
            boolean isKeywordInFile = copyAndScanEntry(zipEntry, zipInputStream, tempFile, context);
            if (!isKeywordInFile) {
                String keyword = NcKeyword.CGMES.toString();
                Set<String> newFilesSet = NcCracUtils.addFileToSet(context.keywordMap(), "contexts:" + zipEntry.getName(), keyword);
                context.keywordMap().put(keyword, newFilesSet);
            }
            FileInputStream fileInputStream = new FileInputStream(tempFile);
            context.tripleStoreNcProfile().read(fileInputStream, NcConstants.RDF_BASE_URL, zipEntry.getName());
        }
        try {
            Files.delete(tempFile.toPath());
        } catch (IOException ioException) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("temporary file for NC crac import can't be deleted");
            tempFile.deleteOnExit();
        }
    }

    private static boolean copyAndScanEntry(ZipEntry zipEntry,
                                            ZipInputStream zipInputStream,
                                            File tempFile,
                                            NcZipImportContext context) throws IOException {
        boolean isKeywordInFile = false;
        int currentSizeEntry = 0;
        InputStream in = new BufferedInputStream(zipInputStream);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
            int nBytes = -1;
            byte[] buffer = new byte[2048];

            while ((nBytes = in.read(buffer)) > 0 && currentSizeEntry < context.maxSizeEntry()) {
                out.write(buffer, 0, nBytes);
                currentSizeEntry += nBytes;
                String stringBuffer = new String(buffer, StandardCharsets.UTF_8);
                Matcher matcher = context.keywordPattern().matcher(stringBuffer);
                if (matcher.find()) {
                    String keyword = matcher.group(1);
                    Set<String> newFilesSet = NcCracUtils.addFileToSet(context.keywordMap(), "contexts:" + zipEntry.getName(), keyword);
                    context.keywordMap().put(keyword, newFilesSet);
                    isKeywordInFile = true;
                }
                if (context.ncp25Pattern().matcher(stringBuffer).find()) {
                    context.ncp25Files().add(zipEntry.getName());
                }
                if (context.ncp25ConformsToPattern().matcher(stringBuffer).find()) {
                    context.ncp25ConformsToFiles().add(zipEntry.getName());
                }
                if (context.ncp24ConformsToPattern().matcher(stringBuffer).find()) {
                    context.ncp24ConformsToFiles().add(zipEntry.getName());
                }
            }
        }
        return isKeywordInFile;
    }

    @Override
    public boolean exists(String filename, InputStream inputStream) {
        if (!FilenameUtils.getExtension(filename).equals(NcConstants.EXTENSION_FILE_NC_PROFILE)) {
            return false;
        }
        TripleStore tripleStoreNcProfile = TripleStoreFactory.create(NcConstants.TRIPLESTORE_RDF4J_NAME);
        tripleStoreNcProfile.read(inputStream, NcConstants.RDF_BASE_URL, "");
        return true;
    }

    @Override
    public CracCreationContext importData(InputStream inputStream, CracCreationParameters cracCreationParameters, Network network) {
        return new NcCracCreator().createCrac(importNativeCrac(inputStream), network, cracCreationParameters);
    }
}
