/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.annotation;

import java.lang.annotation.*;

/**
 * Marker annotation for a class that Generates a Reader
 *
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Documented
public @interface ReaderBuilder {
    /**
     * Gets the name of the generated Reader class, which
     * defaults to the name of the class with suffix of "Reader"
     */
    String value() default "__none__";
}
