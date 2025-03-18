/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.GlobalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class RaoResultArchiveManager {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm"); // TODO: use file name pattern as a property? if yes, content must be checked

    private RaoResultArchiveManager() {
    }

    // default export format is JSON, see later to set export format
    public static void exportAndZipResults(ZipOutputStream zipOutputStream, GlobalRaoResult globalRaoResult, TemporalData<Crac> cracs, Properties properties) throws IOException {
        for (OffsetDateTime timestamp : globalRaoResult.getTimestamps()) {
            addRaoResultToZipArchive(timestamp, zipOutputStream, globalRaoResult.getIndividualRaoResult(timestamp), cracs.getData(timestamp).orElseThrow(), properties);
        }
        // TODO: include serialized summary in ZIP archive and test that it is present
        zipOutputStream.close();
    }

    private static void addRaoResultToZipArchive(OffsetDateTime timestamp, ZipOutputStream zipOutputStream, RaoResult raoResult, Crac crac, Properties properties) throws IOException {
        ZipEntry entry = new ZipEntry("raoResult_%s.json".formatted(timestamp.format(DATE_TIME_FORMATTER)));

        zipOutputStream.putNextEntry(entry);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        raoResult.write("JSON", crac, properties, baos);

        byte[] bytes = new byte[1024];
        int length;
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        while ((length = is.read(bytes)) >= 0) {
            zipOutputStream.write(bytes, 0, length);
        }
        is.close();
        baos.close();

        zipOutputStream.closeEntry();
    }
}
