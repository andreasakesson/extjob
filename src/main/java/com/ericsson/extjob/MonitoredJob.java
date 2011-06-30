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
import hudson.util.AlternativeUiTextProvider;
import hudson.widgets.Widget;
import java.io.File;
import java.io.IOException;
//import java.lang.reflect.Constructor;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, id:cactusman
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
/**
 * Job that runs outside Hudson whose result is submitted to Hudson
 * (either via web interface, or simply by placing files on the file system,
 * for compatibility.)
 *
 * @author Kohsuke Kawaguchi
 */
public class MonitoredJob extends ViewJob<MonitoredJob, MonitoredRun> implements TopLevelItem {

    private static final int THREAD_STATUS_OK = 0;
    private static final int THREAD_STATUS_ERROR = 1;
    private String subscriptionUrl = null;
    private transient int threadStatus = THREAD_STATUS_OK;
    private transient SubscriptionStatusWidget ssw = null;

    /**
     * Threading class which refreshes the keepalive of the subscription
     */
    private class SubscriptionKeepAlive implements Runnable {

        public static final int KEEPALIVE_TIME = 2000; //1000*60*5; // 5 minutes
        private transient MonitoredJob parentJob = null;
        private transient volatile boolean isRunning = true;

        SubscriptionKeepAlive(MonitoredJob parentJob) {
            
            this.parentJob = parentJob;
            if (this.parentJob == null)
                isRunning = false;
        }

        void terminate() {
            isRunning = false;
        }

        @Override
        public void run() {

                while (isRunning) {
                    String subscriptionURL = parentJob.getSubscriptionUrl();

                    if (subscriptionURL != null) {
                        URL url = null;

                        try {
                            url = new URL(parentJob.getSubscriptionUrl() + "subscription/subscribe"); // subscriber already has a tailing slash
                        } catch (MalformedURLException e) {
                            System.out.println("Erronous subscription URL (" + parentJob.getSubscriptionUrl() + "subscription/subscribe )");
                            parentJob.reportThreadStatus(THREAD_STATUS_ERROR);
                            return;
                        }

                        try {
                            
                            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                            httpCon.setDoOutput(true);
                            httpCon.setRequestMethod("POST");
                            OutputStreamWriter out = new OutputStreamWriter(
                                    httpCon.getOutputStream());
                            out.write(Hudson.getInstance().getRootUrl() + parentJob.getUrl());
                            out.flush();

                            // We got an error of some kind..
                            if (httpCon.getResponseCode() != HttpURLConnection.HTTP_OK) {
                                parentJob.reportThreadStatus(THREAD_STATUS_ERROR);
                                System.out.println("Error???");
                            } else {
                                parentJob.reportThreadStatus(THREAD_STATUS_OK);
                            }

                            out.close();

                        } catch (Exception e) {
                            System.out.println("Unable to connect to server: " + e.getMessage());
                            parentJob.reportThreadStatus(THREAD_STATUS_ERROR);
                        }


                    }
                    try {
                        Thread.sleep(KEEPALIVE_TIME);
                    } catch (InterruptedException ex) {}
                }
            
        }
    }
    private transient SubscriptionKeepAlive keepAliveThread = null;

    public MonitoredJob(String name) {
        this(Hudson.getInstance(), name);
    }

    public MonitoredJob(ItemGroup parent, String name) {
        super(parent, name);
    }
/*
    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        // Not sure if this function is ever called
        if (keepAliveThread != null) {
            keepAliveThread.terminate();
        }
    }
*/

    public String getSubscriptionUrl() {
        return subscriptionUrl;
    }

    private void reportThreadStatus(int status) {
        threadStatus = status;

        if (ssw == null)
            ssw = new SubscriptionStatusWidget();

        if (status == THREAD_STATUS_OK)
            ssw.setStatusOk(true);
        else
            ssw.setStatusOk(false);

    }

    @Override
    public void onLoad(ItemGroup<? extends Item> parent, String name)
                                                    throws IOException {
        super.onLoad(parent, name);
        if (subscriptionUrl != null) {
            keepAliveThread = new SubscriptionKeepAlive(this);
            Thread t = new Thread(keepAliveThread);
            t.setName(this.getDisplayName() + " update thread");
            t.start();
        }
    }

    @Override
    public List<Widget> getWidgets() {
        if (ssw == null)
            ssw = new SubscriptionStatusWidget();
        ArrayList<Widget> r = new ArrayList<Widget>();
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
            //Logger.getLogger(XFPanelView.class.getName()).log(Level.SEVERE, null, ex);
        }

        subscriptionUrl = req.getParameter("subscriptionURL");

        if (subscriptionUrl != null) {
            // Add a tailing slash, if it is missing
            if (!subscriptionUrl.endsWith("/")) {
                subscriptionUrl += "/";
            }
        }

        //System.out.println("Got job URL " + subscriptionUrl);
        // Start the update thread - kill previous one if needed
        if (keepAliveThread != null) {
            keepAliveThread.terminate();
            keepAliveThread = null;
        }

        //System.out.println("Monitored job " + this.getDisplayName() + " trying to subscripe to " + subscriptionUrl);

        keepAliveThread = new SubscriptionKeepAlive(this);
        Thread t = new Thread(keepAliveThread);
        t.setName(this.getDisplayName() + " update thread");
        t.start();

        save();

    }
    @Extension
    public static final TopLevelItemDescriptor DESCRIPTOR = new DescriptorImpl();

    @Override
    public String getPronoun() {
        return AlternativeUiTextProvider.get(PRONOUN, this, "Monitored job");//Messages.MonitoredJob_Pronoun());
    }

    public static final class DescriptorImpl extends TopLevelItemDescriptor {

        public String getDisplayName() {
            return "Monitor an external Jenkins job"; //Messages.MonitoredJob_DisplayName();
        }

        public MonitoredJob newInstance(ItemGroup parent, String name) {
            return new MonitoredJob(parent, name);
        }
    }
}
