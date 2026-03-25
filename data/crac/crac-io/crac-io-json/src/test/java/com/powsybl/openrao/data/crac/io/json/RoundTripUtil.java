/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.io.utils.BufferSize;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.crac.api.io.utils.TmpFile;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.json.serializers.CracJsonSerializerModule;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RoundTripUtil {

    private static final ObjectMapper OBJECT_MAPPER = JsonUtil.createObjectMapper();

    static {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER.registerModule(new CracJsonSerializerModule());
    }

    private static final ObjectWriter WRITER = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    private RoundTripUtil() {

    }

    /**
     * This utilitary class enable to export an object through ObjectMapper in an OutputStream and
     * then re-import this stream as the object. The purpose is to see if the whole export/import
     * process works fine.
     *
     * @param object object to export/import
     * @return the object exported and re-imported
     */
    static Crac implicitJsonRoundTrip(Crac object, Network network) {
        try (var tmp = TmpFile.create("implicitJsonRoundTrip.json", BufferSize.MEDIUM)) {
            tmp.withWriteStream(os -> object.write("JSON", os));
            return Crac.read(tmp.getTempFile().toFile(), network);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Crac explicitJsonRoundTrip(Crac object, Network network) {
        try (var tmp = TmpFile.create("explicitJsonRoundTrip.json", BufferSize.MEDIUM)) {
            // export Crac to TmpFile
            tmp.withWriteStream(os -> WRITER.writeValue(os, object));
            // import Crac from TmpFile
            var reader = SafeFileReader.create(tmp.getTempFile(), BufferSize.MEDIUM);
            return new JsonImport().importData(reader, CracCreationParameters.load(), network)
                .getCrac();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
