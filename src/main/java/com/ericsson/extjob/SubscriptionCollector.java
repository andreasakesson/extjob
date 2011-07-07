/******************************************************************************
 * External Job Monitor
 * Copyright Ericsson AB 2011. All Rights Reserved.
 *
 * Software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied.
 *
 ******************************************************************************/
package com.ericsson.extjob;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class SubscriptionCollector
        implements Action {

    final static Logger LOGGER = Logger.getLogger(SubscriptionCollector.class.getName());
    private AbstractProject project;
    private transient List<JobSubscriber> subscriberList = null;

    public SubscriptionCollector(AbstractProject project) {
        this.project = project;
    }

    public String getIconFileName() {
        return "fingerprint.png";
    }

    public String getDisplayName() {
        return "Subscriptions";
    }

    public String getUrlName() {
        return "subscription";
    }

    public AbstractProject getProject() {
        return project;
    }

    /**
     * Adds a subscriber
     * @param subscriber
     */
    private void addSubscriber(JobSubscriber subscriber) {

        if (subscriber != null) {
            if (subscriberList == null) {
                subscriberList = new ArrayList<JobSubscriber>();
            }

            boolean success = subscriberList.add(subscriber);
        }
    }

    /**
     *
     * @return all subscribers
     */
    public final List<JobSubscriber> getSubscribers() {

        if (this.subscriberList == null) {
            this.subscriberList = new ArrayList<JobSubscriber>();
        }

        return this.subscriberList;
    }

    /**
     * Removes a subscriber from the list
     * @param subscriber
     * @return true on success
     */
    public boolean removeSubscriber(JobSubscriber subscriber) {

        if (this.subscriberList != null) {
            return subscriberList.remove(subscriber);
        }
        return false;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public HttpResponse doSubscribe(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException {
        BufferedReader reader = req.getReader();

        String addr = reader.readLine();

        if (addr == null) {
            rsp.sendError(rsp.SC_BAD_REQUEST, "No subscription address provided");
        }

        // Add a tailing slash, if it is missing
        if (!addr.endsWith("/")) {
            addr += "/";
        }

        try {
            URL url = new URL(addr);
        } catch (MalformedURLException e) {
            // Erronous input - reply with error
            rsp.sendError(rsp.SC_BAD_REQUEST, "Erronous subscription address");
        }

        // If the subscriber is already on the list, just beat it (tm)
        if (subscriberList != null) {

            for (JobSubscriber subscriber : subscriberList) {

                if (subscriber.getSubscriber().equals(addr)) {
                    subscriber.beatIt();
                    return createRedirectToMainPage();
                }

            }
        } else {
            subscriberList = new ArrayList<JobSubscriber>();
        }

        // Othervise create a new subscriber
        JobSubscriber subscriber = new JobSubscriber(addr);
        addSubscriber(subscriber);

        return createRedirectToMainPage();
    }

    private HttpRedirect createRedirectToMainPage() {
        return new HttpRedirect(Hudson.getInstance().getRootUrl());
    }
}
