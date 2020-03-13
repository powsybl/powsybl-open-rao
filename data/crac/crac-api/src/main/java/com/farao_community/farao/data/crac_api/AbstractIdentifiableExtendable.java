package com.farao_community.farao.data.crac_api;

import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.commons.extensions.Extendable;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractIdentifiableExtendable<I extends Identifiable> extends AbstractExtendable<I> implements Identifiable, Extendable<I> {
    private final String id;

    protected String name;

    public AbstractIdentifiableExtendable(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public AbstractIdentifiableExtendable(String id) {
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
        AbstractIdentifiableExtendable otherAbstractIdentifiableExtendable = (AbstractIdentifiableExtendable) o;
        return getId().equals(otherAbstractIdentifiableExtendable.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
