/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ericsson.extjob;

import java.util.Calendar;

/**
 *
 * @author eanakes
 */
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
