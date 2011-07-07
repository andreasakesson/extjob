/******************************************************************************
 * External Job Monitor
 * Copyright Ericsson AB 2011. All Rights Reserved.
 *
 * Software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied.
 *
 ******************************************************************************/
package com.ericsson.extjob;

import hudson.model.RunMap.Constructor;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.ViewJob;
import hudson.triggers.Trigger;
import hudson.util.AlternativeUiTextProvider;
import hudson.widgets.Widget;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class MonitoredJob extends ViewJob<MonitoredJob, MonitoredRun> implements TopLevelItem {

    public static final int SUBSCRIPTION_STATUS_OK = 0;
    public static final int SUBSCRIPTION_STATUS_ERROR = 1;
    public static final int SUBSCRIPTION_STATUS_INVALID_URL = 2;
    public static final int SUBSCRIPTION_STATUS_NOT_SET = 3;
    public static final int SUBSCRIPTION_STATUS_INVALID_RESPONSE = 4;


    private String subscriptionUrl = null;
    private transient int threadStatus = SUBSCRIPTION_STATUS_NOT_SET;
    private transient SubscriptionStatusWidget ssw = null;
    private transient MonitorPeriodicWork keepAliveThread = null;

    public MonitoredJob(String name) {
        this(Hudson.getInstance(), name);
    }

    public MonitoredJob(ItemGroup parent, String name) {
        super(parent, name);
    }

    public String getSubscriptionUrl() {
        return subscriptionUrl;
    }

    public void reportThreadStatus(int status) {

        threadStatus = status;

        // Create widget if needed
        if (ssw == null) {
            ssw = new SubscriptionStatusWidget();
        }

        if (status == SUBSCRIPTION_STATUS_OK) {
            ssw.setStatusOk(true);
        } else {
            ssw.setStatusOk(false);
        }
    }

    @Override
    public void onLoad(ItemGroup<? extends Item> parent, String name)
            throws IOException {
        super.onLoad(parent, name);

        keepAliveThread = new MonitorPeriodicWork(this);
        Trigger.timer.scheduleAtFixedRate(keepAliveThread, 0, 5000);

    }

    @Override
    public List<Widget> getWidgets() {

        // Create the widget if needed, then publish it
        if (ssw == null) {
            ssw = new SubscriptionStatusWidget();
        }
        List<Widget> r = super.getWidgets();
        r.add(ssw);
        return r;
    }

    @Override
    protected void reload() {
        this.runs.load(this, new Constructor<MonitoredRun>() {

            public MonitoredRun create(File dir) throws IOException {
                return new MonitoredRun(MonitoredJob.this, dir);
            }
        });
    }
    // keep track of the previous time we started a build
    private transient long lastBuildStartTime;

    /**
     * Creates a new build of this project for immediate execution.
     *
     * Needs to be synchronized so that two {@link #newBuild()} invocations serialize each other.
     */
    public synchronized MonitoredRun newBuild() throws IOException {
        // make sure we don't start two builds in the same second
        // so the build directories will be different too
        long timeSinceLast = System.currentTimeMillis() - lastBuildStartTime;
        if (timeSinceLast < 1000) {
            try {
                Thread.sleep(1000 - timeSinceLast);
            } catch (InterruptedException e) {
            }
        }
        lastBuildStartTime = System.currentTimeMillis();

        MonitoredRun run = new MonitoredRun(this);
        runs.put(run);

        return run;
    }

    /**
     * Used to check if this is an external job and ready to accept a build result.
     */
    public void doAcceptBuildResult(StaplerResponse rsp) throws IOException, ServletException {
        rsp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Used to post the build result from a remote machine.
     */
    public void doPostBuildResult(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        checkPermission(AbstractProject.BUILD);
        MonitoredRun run = newBuild();
        run.acceptRemoteSubmission(req.getReader());
        rsp.setStatus(HttpServletResponse.SC_OK);
    }

    public TopLevelItemDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        try {
            super.submit(req, rsp);
        } catch (IOException ex) {
        }

        subscriptionUrl = req.getParameter("subscriptionURL");

        if (subscriptionUrl != null) {
            // Add a tailing slash, if it is missing
            if (!subscriptionUrl.endsWith("/")) {
                subscriptionUrl += "/";
            }
        }

        // Start the update thread - kill previous one if needed
        if (keepAliveThread != null) {
            keepAliveThread.cancel();
            keepAliveThread = null;
        }

        keepAliveThread = new MonitorPeriodicWork(this);
        Trigger.timer.scheduleAtFixedRate(keepAliveThread, 0, 5000);


        save();

    }
    @Extension
    public static final TopLevelItemDescriptor DESCRIPTOR = new DescriptorImpl();

    @Override
    public String getPronoun() {
        return AlternativeUiTextProvider.get(PRONOUN, this, "Monitored job");
    }

    public static final class DescriptorImpl extends TopLevelItemDescriptor {

        public String getDisplayName() {
            return "Monitor an external Jenkins job";
        }

        public MonitoredJob newInstance(ItemGroup parent, String name) {
            return new MonitoredJob(parent, name);
        }
    }
}
