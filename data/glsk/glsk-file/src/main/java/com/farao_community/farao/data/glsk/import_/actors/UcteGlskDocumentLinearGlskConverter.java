/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_.actors;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.chronology.DataChronologyImpl;
import com.farao_community.farao.data.glsk.import_.GlskPoint;
import com.farao_community.farao.data.glsk.import_.UcteGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Import and Convert a UCTE GLSK document to LinearGlsk
 * return a map :
 * Key: country, Value: DataChronology of LinearGlsk
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public final class UcteGlskDocumentLinearGlskConverter {
    private static final String ERROR_MESSAGE = "Error while converting GLSK document to LinearGlsk sensitivity computation input";

    private UcteGlskDocumentLinearGlskConverter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * @param filepath file path as Path
     * @param network iidm network
     * @return A map associating a DataChronology of LinearGlsk for each country
     */
    public static Map<String, DataChronology<LinearGlsk>> convert(Path filepath, Network network) {
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
     * @return A map associating a DataChronology of LinearGlsk for each country
     */
    public static Map<String, DataChronology<LinearGlsk>> convert(String filepathstring, Network network) {
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
     * @return A map associating a DataChronology of LinearGlsk for each country
     */
    public static Map<String, DataChronology<LinearGlsk>> convert(InputStream data, Network network) {
        return convert(UcteGlskDocumentImporter.importGlsk(data), network);
    }

    /**
     * @param ucteGlskDocument UcteGlskDocument object
     * @param network iidm network
     * @return A map associating a DataChronology of LinearGlsk for each country
     */
    public static Map<String, DataChronology<LinearGlsk>> convert(UcteGlskDocument ucteGlskDocument, Network network) {
        Map<String, DataChronology<LinearGlsk>> chronologyLinearGlskMap = new HashMap<>();

        List<String> countries = ucteGlskDocument.getCountries();

        for (String country : countries) {
            DataChronology<LinearGlsk> dataChronology = DataChronologyImpl.create();
            List<GlskPoint> glskPointList = ucteGlskDocument.getUcteGlskPointsByCountry().get(country);
            for (GlskPoint point : glskPointList) {
                LinearGlsk linearGlsk = GlskPointLinearGlskConverter.convert(network, point, TypeGlskFile.UCTE);
                dataChronology.storeDataOnInterval(linearGlsk, point.getPointInterval());
            }
            chronologyLinearGlskMap.put(country, dataChronology);
        }

        return chronologyLinearGlskMap;

    }
}
