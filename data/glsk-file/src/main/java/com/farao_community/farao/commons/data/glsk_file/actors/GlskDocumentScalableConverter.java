/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file.actors;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.chronology.DataChronologyImpl;
import com.farao_community.farao.commons.data.glsk_file.GlskDocument;
import com.farao_community.farao.commons.data.glsk_file.GlskPoint;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Import and Convert a CIM type GlskDocument to Scalable
 * create a map:
 * Key: country, Value: DataChronology of Scalable
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public final class GlskDocumentScalableConverter {
    private static final String ERROR_MESSAGE = "Error while converting GLSK document to scalables";

    private GlskDocumentScalableConverter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * @param filepath file path in Path
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     */
    public static Map<String, DataChronology<Scalable>> convert(Path filepath, Network network) {
        try {
            InputStream data = new FileInputStream(filepath.toFile());
            return convert(data, network);
        } catch (FileNotFoundException e) {
            throw new FaraoException(ERROR_MESSAGE, e);
        }
    }

    /**
     * @param filepathstring file full path in string
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     */
    public static Map<String, DataChronology<Scalable>> convert(String filepathstring, Network network) {
        try {
            InputStream data = new FileInputStream(filepathstring);
            return convert(data, network);
        } catch (FileNotFoundException e) {
            throw new FaraoException(ERROR_MESSAGE, e);
        }
    }

    /**
     * @param data InputStream
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     */
    public static Map<String, DataChronology<Scalable>> convert(InputStream data, Network network) {
        return convert(GlskDocumentImporter.importGlsk(data), network);
    }

    /**
     * @param glskDocument glsk document object
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     */
    public static Map<String, DataChronology<Scalable>> convert(GlskDocument glskDocument, Network network) {

        List<String> countries = glskDocument.getCountries();

        Map<String, DataChronology<Scalable>> countryScalableDataChronologyMap = new HashMap<>();

        for (String country : countries) {
            DataChronology<Scalable> dataChronology = DataChronologyImpl.create();

            //mapping with DataChronology
            List<GlskPoint> glskPointList = glskDocument.getMapGlskTimeSeries().get(country).getGlskPointListInGlskTimeSeries();
            for (GlskPoint point : glskPointList) {
                Scalable scalable = GlskPointScalableConverter.convert(network, point, TypeGlskFile.CIM);
                dataChronology.storeDataOnInterval(scalable, point.getPointInterval());
            }
            countryScalableDataChronologyMap.put(country, dataChronology);
        }

        return countryScalableDataChronologyMap;
    }

}
