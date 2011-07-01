/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ericsson.extjob;

import hudson.Extension;
import hudson.widgets.Widget;

/**
 *
 * @author eanakes
 */


//@Extension
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
