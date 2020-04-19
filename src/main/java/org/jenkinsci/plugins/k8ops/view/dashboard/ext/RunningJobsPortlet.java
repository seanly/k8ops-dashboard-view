package org.jenkinsci.plugins.k8ops.view.dashboard.ext;

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.plugins.view.dashboard.DashboardPortlet;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.*;

/**
 * Portlet to calculate all changes for a build
 * It uses ChangesAggregators to do so.
 *
 * @author suresh
 */
public class RunningJobsPortlet extends DashboardPortlet {
    private static final int MIN_COLUMN_COUNT = 3;

    private int columnCount;
    private boolean fillColumnFirst = false;

    @DataBoundConstructor
    public RunningJobsPortlet(String name, int columnCount, boolean fillColumnFirst) {
        super(name);
        this.columnCount = columnCount;
        this.fillColumnFirst = fillColumnFirst;
    }

    public boolean isFillColumnFirst() {
        return this.fillColumnFirst;
    }

    public int getColumnCount() {
        return columnCount <= 0 ? MIN_COLUMN_COUNT : columnCount;
    }

    private List<Job> getRunningJobs() {
        Jenkins j = Util.getInstance();

        ArrayList jobs = new ArrayList();

        try (ACLContext ctx = ACL.as(ACL.SYSTEM)) {
            for (Job job : j.getAllItems(Job.class)) {
                if (job.isBuilding()) {
                    jobs.add(job);
                }
            }
        }

        return jobs;
    }

    public List<List<Job>> getJobs() {
        List<Job> jobs = this.getRunningJobs();
        Collections.sort(
                jobs,
                new Comparator<Job>() {
                    public int compare(Job p1, Job p2) {
                        return p1.getDisplayName().compareToIgnoreCase(p2.getDisplayName());
                    }
                });

        if (this.fillColumnFirst) {
            return transposed(jobs);
        } else {
            return Lists.partition(jobs, this.getColumnCount());
        }
    }

    private List<List<Job>> transposed(List<Job> jobs) {
        int rowCount = jobs.size() / this.getColumnCount();
        if (jobs.size() % this.getColumnCount() != 0 ) {
            rowCount++;
        }

        List<List<Job>> result = new ArrayList<List<Job>>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            result.add(new ArrayList<Job>(this.getColumnCount()));
        }

        int c = 0;
        for (Job job : jobs) {
            result.get(c).add(job);
            c = (c + 1) % rowCount;
        }
        return result;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DashboardPortlet> {

        public int getDefaultColumnCount() {
            return MIN_COLUMN_COUNT;
        }

        @Override
        public String getDisplayName() {
            return "Running Jobs";
        }
    }
}
