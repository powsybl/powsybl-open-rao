/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;
import static com.powsybl.openrao.data.raoresult.io.json.deserializers.Utils.checkDeprecatedField;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultDeserializer extends JsonDeserializer<RaoResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoResultDeserializer.class);

    private Crac crac;

    private final boolean checkHeaderOnly;

    public RaoResultDeserializer(boolean checkHeaderOnly) {
        this.checkHeaderOnly = checkHeaderOnly;
    }

    public RaoResultDeserializer(Crac crac) {
        this.crac = crac;
        this.checkHeaderOnly = false;
    }

    @Override
    public RaoResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        RaoResultImpl raoResult = new RaoResultImpl(crac);
        List<Extension<RaoResult>> extensions = Collections.emptyList();

        String jsonFileVersion = isValid(jsonParser, raoResult);
        if (checkHeaderOnly) {
            return null;
        }

        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {
                case INFO:
                    //no need to import this
                    jsonParser.nextToken();
                    break;

                case COMPUTATION_STATUS:
                    raoResult.setComputationStatus(deserializeStatus(jsonParser.nextTextValue()));
                    break;

                case OPTIMIZATION_STEPS_EXECUTED:
                    checkDeprecatedField(jsonParser, RAO_RESULT_TYPE, jsonFileVersion, "1.6");
                    raoResult.setExecutionDetails(jsonParser.nextTextValue());
                    break;

                case EXECUTION_DETAILS:
                    raoResult.setExecutionDetails(jsonParser.nextTextValue());
                    break;

                case COMPUTATION_STATUS_MAP:
                    jsonParser.nextToken();
                    ComputationStatusMapDeserializer.deserialize(jsonParser, raoResult, crac);
                    break;

                case COST_RESULTS:
                    jsonParser.nextToken();
                    CostResultMapDeserializer.deserialize(jsonParser, raoResult, jsonFileVersion, crac);
                    break;

                case FLOWCNEC_RESULTS:
                    jsonParser.nextToken();
                    FlowCnecResultArrayDeserializer.deserialize(jsonParser, raoResult, crac, jsonFileVersion);
                    break;

                case ANGLECNEC_RESULTS:
                    jsonParser.nextToken();
                    AngleCnecResultArrayDeserializer.deserialize(jsonParser, raoResult, crac, jsonFileVersion);
                    break;

                case VOLTAGECNEC_RESULTS:
                    jsonParser.nextToken();
                    VoltageCnecResultArrayDeserializer.deserialize(jsonParser, raoResult, crac, jsonFileVersion);
                    break;

                case NETWORKACTION_RESULTS:
                    jsonParser.nextToken();
                    NetworkActionResultArrayDeserializer.deserialize(jsonParser, raoResult, crac);
                    break;

                case DeprecatedRaoResultJsonConstants.HVDCRANGEACTION_RESULTS:
                    checkDeprecatedField(jsonParser, RAO_RESULT_TYPE, jsonFileVersion, "1.1");
                    importRangeAction(jsonParser, raoResult, jsonFileVersion);
                    break;

                case STANDARDRANGEACTION_RESULTS, PSTRANGEACTION_RESULTS:
                    checkDeprecatedField(jsonParser, RAO_RESULT_TYPE, jsonFileVersion, "1.2");
                    importRangeAction(jsonParser, raoResult, jsonFileVersion);
                    break;

                case RANGEACTION_RESULTS:
                    importRangeAction(jsonParser, raoResult, jsonFileVersion);
                    break;
                case "extensions":
                    jsonParser.nextToken();
                    extensions = JsonUtil.updateExtensions(jsonParser, deserializationContext, RaoResultJsonUtils.getExtensionSerializers(), raoResult);
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field (%s)", jsonParser.getCurrentName()));
            }
        }
        extensions.forEach(extension -> raoResult.addExtension((Class) extension.getClass(), extension));
        return raoResult;
    }

    public static String isValid(JsonParser jsonParser, RaoResultImpl raoResult) throws IOException {
        String firstFieldName = jsonParser.nextFieldName();
        String jsonFileVersion;

        if (firstFieldName.equals(COMPUTATION_STATUS)) {
            /*
             it is assumed that the document version is 1.0
             at this time, there were not the headers with TYPE, VERSION and INFO of the document
             */
            jsonFileVersion = "1.0";
            raoResult.setComputationStatus(deserializeStatus(jsonParser.nextTextValue()));
        } else {
            if (!jsonParser.nextTextValue().equals(RAO_RESULT_TYPE)) {
                throw new OpenRaoException(String.format("type of document must be %s", RAO_RESULT_TYPE));
            }
            if (!jsonParser.nextFieldName().equals(VERSION)) {
                throw new OpenRaoException(String.format("%s must contain a version in its second field", RAO_RESULT_TYPE));
            }
            jsonFileVersion = jsonParser.nextTextValue();
        }

        checkVersion(jsonFileVersion);
        return jsonFileVersion;
    }

    private void importRangeAction(JsonParser jsonParser, RaoResultImpl raoResult, String jsonFileVersion) throws IOException {
        jsonParser.nextToken();
        RangeActionResultArrayDeserializer.deserialize(jsonParser, raoResult, crac, jsonFileVersion);
    }

    private static void checkVersion(String raoResultVersion) {

        if (getPrimaryVersionNumber(RAO_RESULT_IO_VERSION) > getPrimaryVersionNumber(raoResultVersion)) {
            throw new OpenRaoException(String.format("RAO-result importer %s is no longer compatible with json RAO-result version %s", RAO_RESULT_IO_VERSION, raoResultVersion));
        }
        if (getPrimaryVersionNumber(RAO_RESULT_IO_VERSION) < getPrimaryVersionNumber(raoResultVersion)) {
            throw new OpenRaoException(String.format("RAO-result importer %s cannot handle json RAO-result version %s, consider upgrading open-rao version", RAO_RESULT_IO_VERSION, raoResultVersion));
        }
        if (getSubVersionNumber(RAO_RESULT_IO_VERSION) < getSubVersionNumber(raoResultVersion)) {
            LOGGER.warn("RAO-result importer {} might not be compatible with json RAO-result version {}, consider upgrading open-rao version", RAO_RESULT_IO_VERSION, raoResultVersion);
        }

        // otherwise, all is good !
    }
}
