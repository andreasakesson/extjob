/******************************************************************************
 * External Job Monitor
 * Copyright Ericsson AB 2011. All Rights Reserved.
 *
 * Software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied.
 *
 ******************************************************************************/
package com.ericsson.extjob;

import hudson.model.Hudson;
import hudson.model.PeriodicWork;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MonitorPeriodicWork extends PeriodicWork {

    public static final int KEEPALIVE_TIME = 2000; //1000*60*5; // 5 minutes
    private transient MonitoredJob parentJob = null;
    private transient volatile boolean isRunning = true;

    MonitorPeriodicWork(MonitoredJob parentJob) {
        this.parentJob = parentJob;
        if (this.parentJob == null) {
            isRunning = false;
        }
    }

    @Override
    public long getRecurrencePeriod() {
        // request latest electric power usage every 15 minutes
        return KEEPALIVE_TIME;
    }

    @Override
    public long getInitialDelay() {
        return 0;
    }

    @Override
    protected void doRun() throws Exception {

        String subscriptionURL = parentJob.getSubscriptionUrl();

        if (subscriptionURL != null) {
            URL url = null;

            try {
                // Try to create the URL from the string
                url = new URL(parentJob.getSubscriptionUrl() + "subscription/subscribe"); // subscriber already has a tailing slash
            } catch (MalformedURLException e) {
                //System.out.println("Erronous subscription URL (" + parentJob.getSubscriptionUrl() + "subscription/subscribe )");
                parentJob.reportThreadStatus(MonitoredJob.SUBSCRIPTION_STATUS_INVALID_URL);
                //return;
            }

            try {
                // Make connection, and make the POST
                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                httpCon.setDoOutput(true);
                httpCon.setRequestMethod("POST");
                OutputStreamWriter out = new OutputStreamWriter(
                        httpCon.getOutputStream());

                // We only write our own address
                out.write(Hudson.getInstance().getRootUrl() + parentJob.getUrl());
                out.flush();

                // We got an error of some kind..
                if (httpCon.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    parentJob.reportThreadStatus(MonitoredJob.SUBSCRIPTION_STATUS_INVALID_RESPONSE);
                } else {
                    parentJob.reportThreadStatus(MonitoredJob.SUBSCRIPTION_STATUS_OK);
                }

                out.close();

            } catch (Exception e) {
                parentJob.reportThreadStatus(MonitoredJob.SUBSCRIPTION_STATUS_ERROR);
            }


        } else {
            parentJob.reportThreadStatus(MonitoredJob.SUBSCRIPTION_STATUS_NOT_SET);
        }
        try {
            Thread.sleep(KEEPALIVE_TIME);
        } catch (InterruptedException ex) {
        }
    }
}
