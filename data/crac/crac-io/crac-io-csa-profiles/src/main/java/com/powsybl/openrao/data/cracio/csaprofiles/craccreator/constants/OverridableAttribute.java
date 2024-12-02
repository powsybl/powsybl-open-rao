/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum OverridableAttribute {
    ARMED("armed"),
    AVAILABLE("available"),
    ENABLED("enabled"),
    MUST_STUDY("mustStudy"),
    VALUE("value");

    private final String attributeName;

    OverridableAttribute(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getOverridingName() {
        return attributeName;
    }

    public String getDefaultName() {
        return "normal" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1);
    }
}
