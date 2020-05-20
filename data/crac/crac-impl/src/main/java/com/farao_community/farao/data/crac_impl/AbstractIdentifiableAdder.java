package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;

public abstract class AbstractIdentifiableAdder<T extends AbstractIdentifiableAdder<T>> {

    protected Object parent;
    protected String id;
    protected String name;

    protected void checkId() {
        if (this.id == null) {
            throw new FaraoException("Cannot add an identifiable object with no specified id. Please use setId.");
        } else if (this.name == null) {
            this.name = this.id;
        }
    }

    /**
     * Set the ID of the identifiable to add
     * @param id: ID to set
     * @return the identifiable adder instance
     */
    public T setId(String id) {
        this.id = id;
        return (T) this;
    }

    /**
     * Set the name of the identifiable to add
     * @param name: NAME to set
     * @return the identifiable adder instance
     */
    public T setName(String name) {
        this.name = name;
        return (T) this;
    }

}
