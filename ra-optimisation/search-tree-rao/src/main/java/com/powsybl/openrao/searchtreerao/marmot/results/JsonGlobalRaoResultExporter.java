/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.GlobalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class JsonGlobalRaoResultExporter {

    private static final String GLOBAL_RAO_RESULT_SUMMARY_FILE = "globalRaoResult_summary.json";
    private static final DateTimeFormatter FILE_NAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final String FILE_NAME_TEMPLATE = "raoResult_%s.json";

    public void exportData(GlobalRaoResult globalRaoResult, ZipOutputStream zipOutputStream, Properties properties, TemporalData<Crac> cracs) throws IOException {

        globalRaoResult.getTimestamps().forEach(timestamp -> {
            try {
                addRaoResultToZipArchive(timestamp, zipOutputStream, globalRaoResult.getIndividualRaoResult(timestamp), cracs.getData(timestamp).orElseThrow(), properties);
            } catch (IOException e) {
                throw new OpenRaoException("Could not serialize RAO Result for timestamp %s.".formatted(timestamp.format(FILE_NAME_DATE_TIME_FORMATTER)), e);
            }
        });
        addSummaryToZipArchive(globalRaoResult, zipOutputStream);
        zipOutputStream.close();
    }

    private void addSummaryToZipArchive(GlobalRaoResult globalRaoResult, ZipOutputStream zipOutputStream) throws IOException {
        ZipEntry entry = new ZipEntry(GLOBAL_RAO_RESULT_SUMMARY_FILE);
        zipOutputStream.putNextEntry(entry);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectMapper objectMapper = JsonUtil.createObjectMapper();
            SimpleModule module = new JsonGlobalRaoResultSerializerModule();
            objectMapper.registerModule(module);
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(byteArrayOutputStream, globalRaoResult);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        byte[] bytes = new byte[1024];
        int length;
        InputStream is = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        while ((length = is.read(bytes)) >= 0) {
            zipOutputStream.write(bytes, 0, length);
        }

        is.close();
        byteArrayOutputStream.close();
        zipOutputStream.closeEntry();
    }

    private static void addRaoResultToZipArchive(OffsetDateTime timestamp, ZipOutputStream zipOutputStream, RaoResult raoResult, Crac crac, Properties properties) throws IOException {
        ZipEntry entry = new ZipEntry(FILE_NAME_TEMPLATE.formatted(timestamp.format(FILE_NAME_DATE_TIME_FORMATTER)));

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
