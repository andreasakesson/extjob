/******************************************************************************
 * External Job Monitor
 * Copyright Ericsson AB 2011. All Rights Reserved.
 *
 * Software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied.
 *
 ******************************************************************************/
package com.ericsson.extjob;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.User;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

public class ExtJobPublisher extends Notifier {

    public static final int SUBSCRIPTION_LEASE = 1000 * 60 * 10; // 10 minutes
    private static final Logger LOGGER = Logger.getLogger(ExtJobPublisher.class.getName());

    @DataBoundConstructor
    public ExtJobPublisher() {
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public String getHelpFile() {
            return "";
        }

        @Override
        public String getDisplayName() {
            return "Allow external monitoring";
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {

        // TODO: Make this a thread...

        // Get all subscribers from collector
        List<JobSubscriber> subscriberList = null;
        SubscriptionCollector collector = build.getProject().getAction(SubscriptionCollector.class);

        // If the collector hasn't been initialized, bail out
        if (collector == null) {
            return false;
        }

        subscriberList = collector.getSubscribers();

        // Send the update to all subscribers
        if (subscriberList != null) {

            String message = "<run>\n\t<log>Log goes here</log>\n\t<result>";

            if (build.getResult().isBetterOrEqualTo(Result.SUCCESS)) {
                message += "0</result>\n";
            } else {
                message += "1</result>\n";
            }

            message += "\t<duration>" + build.getDuration() + "</duration>\n";

            // Culprits
            message += "\t<culprits>\n";
            Iterator<User> iter = build.getCulprits().iterator();

            int i = 0;
            //while (iter.hasNext()) {
            while (i < 4) {
                //User user = iter.next();
                //message += "\t\t<culprit>" + user.getFullName() + "</culprit>\n";
                message += "\t\t<culprit>CULPRIT " + i + "</culprit>\n";
                i++;
            }

            message += "\t</culprits>\n";
            message += "</run>";

            System.out.println("Message to send subscribers is " + message);

            for (JobSubscriber subscriber : subscriberList) {

                // Check if the subscriber has timed out. In that case, remove it
                long delta = Calendar.getInstance().compareTo(subscriber.getLastHeartbeat());

                if (delta > SUBSCRIPTION_LEASE) {
                    System.out.println("Subscription '" + subscriber.getSubscriber() + "' has timed out.");
                    collector.removeSubscriber(subscriber);
                    continue;
                }

                System.out.println("Sending update to " + subscriber.getSubscriber());

                URL url = new URL(subscriber.getSubscriber() + "postBuildResult"); // subscriber already has a tailing slash
                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                httpCon.setDoOutput(true);
                httpCon.setRequestMethod("POST");
                OutputStreamWriter out = new OutputStreamWriter(
                        httpCon.getOutputStream());
                out.write(message);
                out.flush();

                // We got an error of some kind..
                if (httpCon.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    // Remove subscriber
                    collector.removeSubscriber(subscriber);
                }

                out.close();
            }
        }

        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }
}
