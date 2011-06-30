/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ericsson.extjob;

/**
 *
 * @author eanakes
 */
/*
 * The MIT License
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.security.Permission;
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
        if (Hudson.getInstance().hasPermission(Permission.DELETE)) {
            return null;
        } else {
            return null;
        }
    }

    public String getDisplayName() {
        return "Subscription Collector";
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
                    //System.out.println("Subscriber " + subscriber.getSubscriber() + " renewed its lease.");
                    subscriber.beatIt();
                    return createRedirectToMainPage();
                }

            }
        }
        else
            subscriberList = new ArrayList<JobSubscriber>();

        // Othervise create a new subscriber
        JobSubscriber subscriber = new JobSubscriber(addr);
        addSubscriber(subscriber);
        //System.out.println("New subscriber " + addr + " added.");

        return createRedirectToMainPage();
    }

    private HttpRedirect createRedirectToMainPage() {
        return new HttpRedirect(Hudson.getInstance().getRootUrl());
    }
}


