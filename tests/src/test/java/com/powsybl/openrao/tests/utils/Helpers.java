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
import com.powsybl.openrao.data.crac.api.io.utils.BufferSize;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.crac.api.io.utils.TmpFile;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.crac.io.cse.CseCracCreationContext;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.RefProgImporter;
import com.powsybl.openrao.tests.steps.CommonTestData;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripCimCracCreationContext;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripCseCracCreationContext;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripFbConstraintCreationContext;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripNcCracCreationContext;
import com.powsybl.sensitivity.SensitivityVariableSet;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
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
            return roundTripOnCrac(Crac.read(cracFile, network), network);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static CracCreationContext importCracFromNativeCrac(File cracFile, Network network, CracCreationParameters cracCreationParameters) throws IOException {
        CracCreationContext cracCreationContext = Crac.readWithContext(cracFile, network, cracCreationParameters);
        // round-trip CRAC json export/import to test it implicitly
        return roundTripOnCracCreationContext(cracCreationContext, network);
    }

    public static String getCracFormat(File cracFile) {
        if (cracFile.getName().endsWith(".json")) {
            return "JSON";
        }
        return Crac.getCracFormat(cracFile);
    }

    private static CracCreationContext roundTripOnCracCreationContext(CracCreationContext cracCreationContext, Network network) throws IOException {
        Crac crac = roundTripOnCrac(cracCreationContext.getCrac(), network);
        if (cracCreationContext instanceof FbConstraintCreationContext) {
            return new RoundTripFbConstraintCreationContext((FbConstraintCreationContext) cracCreationContext, crac);
        } else if (cracCreationContext instanceof CseCracCreationContext) {
            return new RoundTripCseCracCreationContext((CseCracCreationContext) cracCreationContext, crac);
        } else if (cracCreationContext instanceof CimCracCreationContext) {
            return new RoundTripCimCracCreationContext((CimCracCreationContext) cracCreationContext, crac);
        } else if (cracCreationContext instanceof NcCracCreationContext) {
            return new RoundTripNcCracCreationContext((NcCracCreationContext) cracCreationContext, crac);
        } else {
            throw new NotImplementedException(String.format("%s type is not supported", cracCreationContext.getClass().getName()));
        }
    }

    private static Crac roundTripOnCrac(Crac crac, Network network) throws IOException {
        try (var tmp = TmpFile.create("round.json", BufferSize.MEDIUM)) {
            // export Crac
            tmp.withWriteStream(os -> crac.write("JSON", os));
            // import Crac
            return Crac.read(tmp.getTempFile().toFile(), network);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ZonalData<SensitivityVariableSet> importUcteGlskFile(File glskFile, OffsetDateTime timestamp, Network network) throws IOException {
        UcteGlskDocument ucteGlskDocument = SafeFileReader.create(glskFile, BufferSize.MEDIUM).withReadStream(UcteGlskDocument::importGlsk);

        Instant instant;
        if (timestamp == null) {
            instant = getStartInstantOfUcteGlsk(ucteGlskDocument);
        } else {
            instant = timestamp.toInstant();
        }

        return ucteGlskDocument.getZonalGlsks(network, instant);
    }

    public static ZonalData<Scalable> importMonitoringGlskFile(File monitoringGlskFile, OffsetDateTime timestamp, Network network) throws IOException {
        CimGlskDocument cimGlskDocument = SafeFileReader.create(monitoringGlskFile, BufferSize.MEDIUM).withReadStream(CimGlskDocument::importGlsk);

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

        return SafeFileReader.create(refProgFile, BufferSize.SMALL).withReadStream(is -> RefProgImporter.importRefProg(is, offsetDateTime));
    }

    public static RaoResult importRaoResult(File raoResultFile) throws IOException {
        RaoResult raoResult = RaoResult.read(raoResultFile, CommonTestData.getCrac());
        return raoResult;
    }

    /*
    private static InputStream getStreamFromZippable(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        if (!FilenameUtils.getExtension(file.getAbsolutePath()).equals("zip")) {
            return fileInputStream;
        }
        ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
        zipInputStream.getNextEntry();
        return zipInputStream;
    }
    */

    public static File getFile(String path) {
        Objects.requireNonNull(path);
        try {
            return new File(path);
        } catch (Exception e) {
            throw new OpenRaoException(String.format("Could not load file %s", path));
        }
    }

}
