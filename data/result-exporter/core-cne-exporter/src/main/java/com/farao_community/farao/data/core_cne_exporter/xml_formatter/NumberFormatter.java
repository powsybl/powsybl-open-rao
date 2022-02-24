/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter.xml_formatter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class NumberFormatter {

    private static final DecimalFormat FLOAT_PRINT_FORMAT_5D = new DecimalFormat("####0.#####", new DecimalFormatSymbols(new Locale("en", "UK")));
    private static final DecimalFormat FLOAT_PRINT_FORMAT_2D = new DecimalFormat("####0.##", new DecimalFormatSymbols(new Locale("en", "UK")));
    private static final DecimalFormat FLOAT_PRINT_FORMAT_0D = new DecimalFormat("####0", new DecimalFormatSymbols(new Locale("en", "UK")));

    private NumberFormatter() {
    }

    public static String printFloat(Float floatValue) {
        /*
         floats have an accuracy of about 7 digits, depending on the value of the float, we print more or less digits,
         but never more than 7
         */
        if (floatValue > 1e5) {
            return FLOAT_PRINT_FORMAT_0D.format(floatValue);
        } else if (floatValue > 1e2) {
            return FLOAT_PRINT_FORMAT_2D.format(floatValue);
        } else {
            return FLOAT_PRINT_FORMAT_5D.format(floatValue);
        }
    }
}
