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
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.TopLevelItem;
import java.util.Collection;
import java.util.Collections;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public class SubscriptionProperty
        extends JobProperty<AbstractProject<?, ?>> {

    @Override
    public Collection<? extends Action> getJobActions(AbstractProject<?, ?> job) {

        return Collections.<Action>singleton(new SubscriptionCollector(job));
    }

    @Override
    public JobProperty<?> reconfigure(org.kohsuke.stapler.StaplerRequest req,
            net.sf.json.JSONObject form)
            throws Descriptor.FormException {
        return this;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Extension
    public static final class DescriptorImpl
            extends JobPropertyDescriptor {

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return TopLevelItem.class.isAssignableFrom(jobType);
        }

        public String getDisplayName() {
            return "Subscription property";
        }

        @Override
        public SubscriptionProperty newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return new SubscriptionProperty();
        }
    }
}
