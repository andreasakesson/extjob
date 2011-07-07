/******************************************************************************
 * External Job Monitor
 * Copyright Ericsson AB 2011. All Rights Reserved.
 *
 * Software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied.
 *
 ******************************************************************************/
package com.ericsson.extjob;

import hudson.widgets.Widget;

public class SubscriptionStatusWidget extends Widget {

    private String errorMsg;
    private boolean statusOk;

    public SubscriptionStatusWidget() {
    }

    @Override
    public String getUrlName() {
        return "subscriptionStatus";
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String msg) {
        errorMsg = msg;
    }

    public boolean getStatusOk() {
        return statusOk;
    }

    public void setStatusOk(boolean status) {
        statusOk = status;
    }
}
