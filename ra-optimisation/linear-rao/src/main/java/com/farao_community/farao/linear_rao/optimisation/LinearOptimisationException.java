package com.farao_community.farao.linear_rao.optimisation;

import com.farao_community.farao.commons.FaraoException;

public class LinearOptimisationException extends FaraoException {

    private final String failureReason;

    public String getFailureReason() {
        return failureReason;
    }

    public LinearOptimisationException() {
        failureReason = "";
    }

    public LinearOptimisationException(final String msg) {
        super(msg);
        failureReason = "";
    }

    public LinearOptimisationException(final String msg, final String failureReason) {
        super(msg);
        this.failureReason = failureReason;
    }

    public LinearOptimisationException(final Throwable throwable) {
        super(throwable);
        failureReason = "";
    }

    public LinearOptimisationException(final String message, final Throwable cause) {
        super(message, cause);
        failureReason = "";
    }

}
