package com.farao_community.farao.linear_rao.optimisation;

import com.farao_community.farao.commons.FaraoException;

public class LinearOptimisationException extends FaraoException {

    private String failureReason;

    public String getFailureReason() {
        return failureReason;
    }

    public LinearOptimisationException() {
    }

    public LinearOptimisationException(final String msg) {
        super(msg);
    }

    public LinearOptimisationException(final String msg, final String failureReason) {
        super(msg);
        this.failureReason = failureReason;
    }

    public LinearOptimisationException(final Throwable throwable) {
        super(throwable);
    }

    public LinearOptimisationException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
