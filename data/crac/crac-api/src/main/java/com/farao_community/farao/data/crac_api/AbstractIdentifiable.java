/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Common abstract class to inherit from for all identifiable classes
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class AbstractIdentifiable implements Identifiable {
    private final String id;

    protected String name;

    public AbstractIdentifiable(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public AbstractIdentifiable(String id) {
        this(id, id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name != null ? name : id;
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Check if abstract identifiables are equals. Abstract identifiables are considered equals when IDs are equals.
     *
     * @param o: If it's null or another object than AbstractIdentifiable it will return false.
     * @return A boolean true if objects are equals, otherwise false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractIdentifiable abstractIdentifiable = (AbstractIdentifiable) o;
        return getId().equals(abstractIdentifiable.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
