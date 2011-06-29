/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ericsson.extjob;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Descriptor.FormException;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.TopLevelItem;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author eanakes
 */
public class SubscriptionProperty
        extends JobProperty<AbstractProject<?, ?>> {

    @Override
    public Collection<? extends Action> getJobActions(AbstractProject<?, ?> job) {
        final List<Action> actions = new LinkedList<Action>();
        actions.add(new SubscriptionCollector(job));
        return actions;
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
            return null;
        }

        @Override
        public SubscriptionProperty newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return new SubscriptionProperty();
        }
    }
}
