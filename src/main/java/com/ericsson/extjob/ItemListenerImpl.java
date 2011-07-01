/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ericsson.extjob;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eanakes
 */
@Extension
public class ItemListenerImpl
        extends ItemListener {

    private static final Logger LOGGER = Logger.getLogger(ItemListenerImpl.class.getName());

    @Override
    public void onLoaded() {
        System.out.println("On load..");
        for (AbstractProject<?, ?> project : Hudson.getInstance().getAllItems(AbstractProject.class)) {
            addShelveProjectProperty(project);
        }
    }

    @Override
    public void onCreated(Item item) {
        System.out.println("On created");
        if (item instanceof AbstractProject) {
            AbstractProject project = (AbstractProject) item;
            addShelveProjectProperty(project);
        }
    }

    private void addShelveProjectProperty(AbstractProject<?, ?> project) {
        try {
            if (project.getProperty(SubscriptionProperty.class) == null) {
                System.out.println("Add prop");
                project.addProperty(new SubscriptionProperty());
            }
            System.out.println("Done");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to persist " + project, e);
        }
    }
}
