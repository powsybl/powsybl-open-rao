/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;
import static com.farao_community.farao.data.rao_result_json.deserializers.DeprecatedRaoResultJsonConstants.HVDCRANGEACTION_RESULTS;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultDeserializer extends JsonDeserializer<RaoResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoResultDeserializer.class);

    private Crac crac;

    private RaoResultDeserializer() {
    }

    public RaoResultDeserializer(Crac crac) {
        this.crac = crac;
    }

    @Override
    public RaoResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        RaoResultImpl raoResult = new RaoResultImpl();

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
                throw new FaraoException(String.format("type of document must be %s", RAO_RESULT_TYPE));
            }
            if (!jsonParser.nextFieldName().equals(VERSION)) {
                throw new FaraoException(String.format("%s must contain a version in its second field", RAO_RESULT_TYPE));
            }
            jsonFileVersion = jsonParser.nextTextValue();
        }

        checkVersion(jsonFileVersion);

        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {

                case INFO:
                    //no need to import this
                    jsonParser.nextToken();
                    break;

                case COMPUTATION_STATUS:
                    raoResult.setComputationStatus(deserializeStatus(jsonParser.nextTextValue()));
                    break;

                case COST_RESULTS:
                    jsonParser.nextToken();
                    CostResultMapDeserializer.deserialize(jsonParser, raoResult);
                    break;

                case FLOWCNEC_RESULTS:
                    jsonParser.nextToken();
                    FlowCnecResultArrayDeserializer.deserialize(jsonParser, raoResult, crac);
                    break;

                case NETWORKACTION_RESULTS:
                    jsonParser.nextToken();
                    NetworkActionResultArrayDeserializer.deserialize(jsonParser, raoResult, crac);
                    break;

                case PSTRANGEACTION_RESULTS:
                    jsonParser.nextToken();
                    PstRangeActionResultArrayDeserializer.deserialize(jsonParser, raoResult, crac, jsonFileVersion);
                    break;

                case HVDCRANGEACTION_RESULTS:
                    // used in version <=1.1
                    // now called standardRangeAction as it contains a more generic result object
                    if (getPrimaryVersionNumber(jsonFileVersion) > 1 && getSubVersionNumber(jsonFileVersion) > 1) {
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: field %s is not supported in file version %s", jsonParser.getCurrentName(), jsonFileVersion));
                    }
                    // else, considered HvdcRangeActions as StandardRangeActions

                case STANDARDRANGEACTION_RESULTS:
                    jsonParser.nextToken();
                    StandardRangeActionResultArrayDeserializer.deserialize(jsonParser, raoResult, crac, jsonFileVersion);
                    break;

                default:
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field (%s)", jsonParser.getCurrentName()));
            }
        }
        return raoResult;
    }

    private void checkVersion(String raoResultVersion) {

        if (getPrimaryVersionNumber(RAO_RESULT_IO_VERSION) > getPrimaryVersionNumber(raoResultVersion)) {
            throw new FaraoException(String.format("RAO-result importer %s is no longer compatible with json RAO-result version %s", RAO_RESULT_IO_VERSION, raoResultVersion));
        }
        if (getPrimaryVersionNumber(RAO_RESULT_IO_VERSION) < getPrimaryVersionNumber(raoResultVersion)) {
            throw new FaraoException(String.format("RAO-result importer %s cannot handle json RAO-result version %s, consider upgrading farao-core version", RAO_RESULT_IO_VERSION, raoResultVersion));
        }
        if (getSubVersionNumber(RAO_RESULT_IO_VERSION) < getSubVersionNumber(raoResultVersion)) {
            LOGGER.warn("RAO-result importer {} might not be compatible with json RAO-result version {}, consider upgrading farao-core version", RAO_RESULT_IO_VERSION, raoResultVersion);
        }

        // otherwise, all is good !
    }
}

