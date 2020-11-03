/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_.actors;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public interface GlskDocumentScalableConverter {

    /**
     * @param filepath file path in Path
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     */
    static Map<String, DataChronology<Scalable>> convert(Path filepath, Network network) {
        return null;
    }

    /**
     * @param filepathstring file full path in string
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     */
    static Map<String, DataChronology<Scalable>> convert(String filepathstring, Network network) {
        return null;
    }

    /**
     * @param data InputStream
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     */
    static Map<String, DataChronology<Scalable>> convert(InputStream data, Network network) {
        return null;
    }

}
