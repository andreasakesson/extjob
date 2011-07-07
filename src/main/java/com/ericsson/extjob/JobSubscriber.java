/******************************************************************************
 * External Job Monitor
 * Copyright Ericsson AB 2011. All Rights Reserved.
 *
 * Software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied.
 *
 ******************************************************************************/
package com.ericsson.extjob;

import java.util.Calendar;

public class JobSubscriber {

    private String subscriber;
    private Calendar lastHeartbeat;

    JobSubscriber(String subscriber) {
        this.subscriber = subscriber;
        this.lastHeartbeat = Calendar.getInstance();
    }

    public String getSubscriber() {
        return subscriber;
    }

    public Calendar getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void beatIt() {
        this.lastHeartbeat = Calendar.getInstance();
    }
};
