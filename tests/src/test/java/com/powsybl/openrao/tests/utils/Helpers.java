/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.utils;

import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.crac.io.cse.CseCracCreationContext;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.RefProgImporter;
import com.powsybl.openrao.tests.steps.CommonTestData;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripCimCracCreationContext;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripCsaProfileCracCreationContext;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripCseCracCreationContext;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripFbConstraintCreationContext;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipInputStream;

public final class Helpers {
    private Helpers() {
        // must nor be used
    }

    public static Network importNetwork(File networkFile, boolean useRdfId) {
        Properties importParams = new Properties();
        if (useRdfId) {
            importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        }
        return Network.read(Paths.get(networkFile.toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }

    public static Pair<Crac, CracCreationContext> importCrac(File cracFile, Network network, CracCreationParameters cracCreationParameters) throws IOException {
        if (cracFile.getName().endsWith(".json")) {
            // for now, the only JSON format is the open rao internal format
            return Pair.of(importCracFromInternalFormat(cracFile, network), null);
        } else {
            CracCreationContext ccc = importCracFromNativeCrac(cracFile, network, cracCreationParameters);
            return Pair.of(ccc.getCrac(), ccc);
        }
    }

    public static Crac importCracFromInternalFormat(File cracFile, Network network) {
        try {
            return roundTripOnCrac(Crac.read("crac.json", new FileInputStream(cracFile), network), network);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static CracCreationContext importCracFromNativeCrac(File cracFile, Network network, CracCreationParameters cracCreationParameters) throws IOException {
        byte[] cracBytes = null;
        try (InputStream cracInputStream = new BufferedInputStream(new FileInputStream(cracFile))) {
            cracBytes = getBytesFromInputStream(cracInputStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new OpenRaoException("Could not load CRAC file", e);
        }
        CracCreationContext cracCreationContext = Crac.readWithContext(cracFile.getName(), new ByteArrayInputStream(cracBytes), network, cracCreationParameters);
        // round-trip CRAC json export/import to test it implicitly
        return roundTripOnCracCreationContext(cracCreationContext, network);
    }

    public static String getCracFormat(File cracFile) {
        if (cracFile.getName().endsWith(".json")) {
            return "JSON";
        }
        byte[] cracBytes = null;
        try (InputStream cracInputStream = new BufferedInputStream(new FileInputStream(cracFile))) {
            cracBytes = getBytesFromInputStream(cracInputStream);
            return Crac.getCracFormat(cracFile.getName(), new ByteArrayInputStream(cracBytes));
        } catch (IOException e) {
            e.printStackTrace();
            throw new OpenRaoException("Could not load CRAC file", e);
        }
    }

    private static CracCreationContext roundTripOnCracCreationContext(CracCreationContext cracCreationContext, Network network) throws IOException {
        Crac crac = roundTripOnCrac(cracCreationContext.getCrac(), network);
        if (cracCreationContext instanceof FbConstraintCreationContext) {
            return new RoundTripFbConstraintCreationContext((FbConstraintCreationContext) cracCreationContext, crac);
        } else if (cracCreationContext instanceof CseCracCreationContext) {
            return new RoundTripCseCracCreationContext((CseCracCreationContext) cracCreationContext, crac);
        } else if (cracCreationContext instanceof CimCracCreationContext) {
            return new RoundTripCimCracCreationContext((CimCracCreationContext) cracCreationContext, crac);
        } else if (cracCreationContext instanceof CsaProfileCracCreationContext) {
            return new RoundTripCsaProfileCracCreationContext((CsaProfileCracCreationContext) cracCreationContext, crac);
        } else {
            throw new NotImplementedException(String.format("%s type is not supported", cracCreationContext.getClass().getName()));
        }
    }

    private static Crac roundTripOnCrac(Crac crac, Network network) throws IOException {
        // export Crac
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        crac.write("JSON", outputStream);

        // import Crac
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return Crac.read("crac.json", inputStream, network);
    }

    public static ZonalData<SensitivityVariableSet> importUcteGlskFile(File glskFile, OffsetDateTime timestamp, Network network) throws IOException {
        InputStream inputStream = new FileInputStream(glskFile);
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(inputStream);

        Instant instant;
        if (timestamp == null) {
            instant = getStartInstantOfUcteGlsk(ucteGlskDocument);
        } else {
            instant = timestamp.toInstant();
        }

        return ucteGlskDocument.getZonalGlsks(network, instant);
    }

    public static ZonalData<Scalable> importMonitoringGlskFile(File monitoringGlskFile, OffsetDateTime timestamp, Network network) throws IOException {
        InputStream inputStream = new FileInputStream(monitoringGlskFile);
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(inputStream);

        Instant instant;
        if (timestamp == null) {
            instant = cimGlskDocument.getInstantStart();
        } else {
            instant = timestamp.toInstant();
        }

        return cimGlskDocument.getZonalScalable(network, instant);
    }

    private static Instant getStartInstantOfUcteGlsk(UcteGlskDocument ucteGlskDocument) {
        return ucteGlskDocument.getGSKTimeInterval().getStart();
    }

    public static OffsetDateTime getOffsetDateTimeFromBrusselsTimestamp(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        return ZonedDateTime.of(LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), ZoneId.of("Europe/Brussels"))
            .toOffsetDateTime();
    }

    public static ReferenceProgram importRefProg(File refProgFile, OffsetDateTime offsetDateTime) throws IOException {
        if (offsetDateTime == null) {
            throw new OpenRaoException("A timestamp should be provided in order to import the refProg file.");
        }

        InputStream refProgInputStream = new FileInputStream(refProgFile);
        return RefProgImporter.importRefProg(refProgInputStream, offsetDateTime);
    }

    public static RaoResult importRaoResult(File raoResultFile) throws IOException {
        InputStream inputStream = getStreamFromZippable(raoResultFile);
        RaoResult raoResult = RaoResult.read(inputStream, CommonTestData.getCrac());
        inputStream.close();
        return raoResult;
    }

    private static InputStream getStreamFromZippable(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        if (!FilenameUtils.getExtension(file.getAbsolutePath()).equals("zip")) {
            return fileInputStream;
        }
        ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
        zipInputStream.getNextEntry();
        return zipInputStream;
    }

    public static File getFile(String path) {
        Objects.requireNonNull(path);
        try {
            return new File(path);
        } catch (Exception e) {
            throw new OpenRaoException(String.format("Could not load file %s", path));
        }
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            org.apache.commons.io.IOUtils.copy(inputStream, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }
}
