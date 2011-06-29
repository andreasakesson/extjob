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
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Jean-Baptiste Quenot, Seiji Sogabe, Tom Huybrechts
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
        for (JobSubscriber subscriber : subscriberList) {

            if (subscriber.getSubscriber().equals(addr)) {
                System.out.println("Subscriber " + subscriber.getSubscriber() + " renewed its lease.");
                subscriber.beatIt();
                return createRedirectToMainPage();
            }

        }

        // Othervise create a new subscriber
        JobSubscriber subscriber = new JobSubscriber(addr);
        addSubscriber(subscriber);
        System.out.println("New subscriber " + addr + " added.");

        return createRedirectToMainPage();
    }

    private HttpRedirect createRedirectToMainPage() {
        return new HttpRedirect(Hudson.getInstance().getRootUrl());
    }
}





/**
 * Keeps a list of the parameters defined for a project.
 *
 * <p>
 * This class also implements {@link Action} so that <tt>index.jelly</tt> provides
 * a form to enter build parameters.
 */
//
//@ExportedBean(defaultVisibility=2)
//public class SubscriptionCollector extends JobProperty<AbstractProject<?, ?>>
//        implements Action {
//
//    private final List<ParameterDefinition> parameterDefinitions;
//
//    public SubscriptionCollector(List<ParameterDefinition> parameterDefinitions) {
//        System.out.println("Construct1");
//        this.parameterDefinitions = parameterDefinitions;
//    }
//
//    public SubscriptionCollector(ParameterDefinition... parameterDefinitions) {
//        System.out.println("Construct2");
//        this.parameterDefinitions = Arrays.asList(parameterDefinitions);
//    }
//
//    public AbstractProject<?,?> getOwner() {
//        System.out.println("owner");
//        return owner;
//    }
//
//    @Exported
//    public List<ParameterDefinition> getParameterDefinitions() {
//        System.out.println("paramter definition");
//        return parameterDefinitions;
//    }
//
//    /**
//     * Gets the names of all the parameter definitions.
//     */
//    public List<String> getParameterDefinitionNames() {
//        System.out.println("getparametersdefinitions");
//        return new AbstractList<String>() {
//            public String get(int index) {
//                return parameterDefinitions.get(index).getName();
//            }
//
//            public int size() {
//                return parameterDefinitions.size();
//            }
//        };
//    }
//
//    @Override
//    public Collection<Action> getJobActions(AbstractProject<?, ?> job) {
//        System.out.println("Get actions");
//        return Collections.<Action>singleton(this);
//    }
//
//    public AbstractProject<?, ?> getProject() {
//        System.out.println("Get project");
//        return (AbstractProject<?, ?>) owner;
//    }
//
//    /**
//     * Interprets the form submission and schedules a build for a parameterized job.
//     *
//     * <p>
//     * This method is supposed to be invoked from {@link AbstractProject#doBuild(StaplerRequest, StaplerResponse)}.
//     */
//    public void _doBuild(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
//
//        System.out.println("_dobuild");
//        if(!req.getMethod().equals("POST")) {
//            // show the parameter entry form.
//            req.getView(this,"index.jelly").forward(req,rsp);
//            return;
//        }
//
//        List<ParameterValue> values = new ArrayList<ParameterValue>();
//
//        JSONObject formData = req.getSubmittedForm();
//        JSONArray a = JSONArray.fromObject(formData.get("parameter"));
//
//        for (Object o : a) {
//            JSONObject jo = (JSONObject) o;
//            String name = jo.getString("name");
//
//            ParameterDefinition d = getParameterDefinition(name);
//            if(d==null)
//                throw new IllegalArgumentException("No such parameter definition: " + name);
//            ParameterValue parameterValue = d.createValue(req, jo);
//            values.add(parameterValue);
//        }
//
//    	//Jenkins.getInstance().getQueue().schedule(
//        //        owner, owner.getDelay(req), new ParametersAction(values), new CauseAction(new Cause.UserCause()));
//
//        // send the user back to the job top page.
//        rsp.sendRedirect(".");
//    }
//
//    public void doFsck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
//
//        System.out.println("FSCK!");
//    }
//
//    public void buildWithParameters(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
//
//        System.out.println("buildWithParamters");
//
//        List<ParameterValue> values = new ArrayList<ParameterValue>();
//        for (ParameterDefinition d: parameterDefinitions) {
//        	ParameterValue value = d.createValue(req);
//        	if (value != null) {
//        		values.add(value);
//        	} else {
//        		throw new IllegalArgumentException("Parameter " + d.getName() + " was missing.");
//        	}
//        }
//
//    	//Hudson.getInstance().getQueue().schedule(
//        //        owner, owner.getDelay(req), new ParametersAction(values), owner.getBuildCause(req));
//
//        // send the user back to the job top page.
//        rsp.sendRedirect(".");
//    }
//
//    /**
//     * Gets the {@link ParameterDefinition} of the given name, if any.
//     */
//    public ParameterDefinition getParameterDefinition(String name) {
//        System.out.println("Get parameters definition");
//        for (ParameterDefinition pd : parameterDefinitions)
//            if (pd.getName().equals(name))
//                return pd;
//        return null;
//    }
//
//    @Extension
//    public static class DescriptorImpl extends JobPropertyDescriptor {
//        @Override
//        public boolean isApplicable(Class<? extends Job> jobType) {
//            System.out.println("Is applicable!!!!!!!!!!!! =" + AbstractProject.class.isAssignableFrom(jobType));
//            return AbstractProject.class.isAssignableFrom(jobType);
//        }
//
//        @Override
//        public JobProperty<?> newInstance(StaplerRequest req,
//                                          JSONObject formData) throws FormException {
//
//            System.out.println("New instance called!");
//            if (formData.isNullObject()) {
//                return null;
//            }
//
//            JSONObject parameterized = formData.getJSONObject("parameterized");
//
//            if (parameterized.isNullObject()) {
//            	return null;
//            }
//
//            List<ParameterDefinition> parameterDefinitions = Descriptor.newInstancesFromHeteroList(
//                    req, parameterized, "parameter", ParameterDefinition.all());
//            if(parameterDefinitions.isEmpty())
//                return null;
//
//            return new SubscriptionCollector(parameterDefinitions);
//        }
//
//        @Override
//        public String getDisplayName() {
//            return "DISPLAY NAME!"; //Messages.SubscriptionCollector_DisplayName();
//        }
//    }
//
//    public String getDisplayName() {
//        return null;
//    }
//
//    public String getIconFileName() {
//        return null;
//    }
//
//    public String getUrlName() {
//        return null;
//    }
//}



/*
@ExportedBean(defaultVisibility=2)
public class SubscriptionCollector implements PermalinkProjectAction {

    @DataBoundConstructor
    SubscriptionCollector() {
        System.out.println("Construction");
    }

    public void doSubscribe(org.kohsuke.stapler.StaplerRequest req,
                            org.kohsuke.stapler.StaplerResponse rsp)
                            throws IOException,
                                   javax.servlet.ServletException {
        System.out.println("Subscription");
    }

    public List<PermalinkProjectAction.Permalink> getPermalinks() {

        List<PermalinkProjectAction.Permalink> lst = new ArrayList<PermalinkProjectAction.Permalink>();

        PermalinkProjectAction.Permalink pl;
        pl.


        return lst;
    }
 
    @Override
    public String getUrlName() {
        System.out.println("Url name");
        return "test";
    }

    @Override
    public String getDisplayName() {
        System.out.println("Display name");
        return "Test!!";
    }

    @Override
    public String getIconFileName() {
        System.out.println("Icon");
        return "";
    }


}
*/
