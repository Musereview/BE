package com.mr.domain.subscriptions.exception;

import com.mr.global.apipayload.code.BaseCode;
import com.mr.global.apipayload.exception.GeneralException;

public class SubscriptionException extends GeneralException {
    public SubscriptionException(BaseCode code) {
        super(code);
    }
}
