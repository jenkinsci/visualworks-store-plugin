package org.jenkinsci.plugins.visualworks_store;


import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.scm.*;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoreSCM extends SCM {
    private final String repositoryName;
    private List<PundleSpec> pundles;
    private final String versionRegex;
    private final String minimumBlessingLevel;
    private final boolean generateParcelBuilderInputFile;
    private final String parcelBuilderInputFilename;

    @DataBoundConstructor
    public StoreSCM(String repositoryName, List<PundleSpec> pundles, String versionRegex, String minimumBlessingLevel,
                    boolean generateParcelBuilderInputFile, String parcelBuilderInputFilename) {
        if (pundles == null) {
            pundles = new ArrayList<PundleSpec>();
        }
        this.repositoryName = repositoryName;
        this.pundles = pundles;
        this.versionRegex = versionRegex;
        this.minimumBlessingLevel = minimumBlessingLevel;
        this.generateParcelBuilderInputFile = generateParcelBuilderInputFile;
        this.parcelBuilderInputFilename = parcelBuilderInputFilename;
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return false;
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher,
                                                      FilePath workspace, TaskListener taskListener,
                                                      SCMRevisionState _baseline)
            throws IOException, InterruptedException {
        if (project.getLastBuild() == null) {
            taskListener.getLogger().println("No existing build. Scheduling a new one.");
            return PollingResult.BUILD_NOW;
        }

        final StoreRevisionState baseline = (StoreRevisionState) _baseline;

        ArgumentListBuilder builder = preparePollingCommand(getDescriptor().getScript());

        String output;
        try {
            output = new StoreCommandRunner().runCommand(builder, launcher, taskListener);
        } catch (StoreCommandFailure error) {
            return PollingResult.NO_CHANGES;
        }

        final StoreRevisionState current = baseline;

        return new PollingResult(baseline, current, PollingResult.Change.NONE);
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> abstractBuild, Launcher launcher, FilePath filePath, BuildListener buildListener, File file) throws IOException, InterruptedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> abstractBuild, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
        // The revision state is added to the build as part of checkout(), so this will not be called.
        return null;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public List<PundleSpec> getPundles() {
        return pundles;
    }

    public String getVersionRegex() {
        return versionRegex;
    }

    public String getMinimumBlessingLevel() {
        return minimumBlessingLevel;
    }

    public boolean isGenerateParcelBuilderInputFile() {
        return generateParcelBuilderInputFile;
    }

    public String getParcelBuilderInputFilename() {
        return parcelBuilderInputFilename;
    }

    public ArgumentListBuilder preparePollingCommand(String storeScript) {
        ArgumentListBuilder builder = new ArgumentListBuilder();

        builder.add(storeScript);
        builder.add("-repository", repositoryName);
        builder.add("-packages");
        for (PundleSpec spec : pundles) {
            builder.add(spec.getName());
        }
        builder.add("-versionRegex", versionRegex);
        builder.add("-blessedAtLeast", minimumBlessingLevel);

        return builder;
    }

    @Extension
    public static final class DescriptorImpl extends SCMDescriptor<StoreSCM> {
        private String script;

        public DescriptorImpl() {
            super(StoreSCM.class, null);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Store";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json);
            save();
            return true;
        }

        @SuppressWarnings("UnusedDeclaration")
        public ListBoxModel doFillMinimumBlessingLevelItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("Broken");
            items.add("Work In Progress");
            items.add("Development");
            items.add("To Review");
            items.add("Patch");
            items.add("Integration-Ready");
            items.add("Integrated");
            items.add("Ready to Merge");
            items.add("Merged");
            items.add("Tested");
            items.add("Internal Release");
            items.add("Released");
            return items;
        }

        @SuppressWarnings("UnusedDeclaration")
        public String getDefaultVersionRegex() {
            return ".+";
        }

        @SuppressWarnings("UnusedDeclaration")
        public String getDefaultMinimumBlessingLevel() {
            return "Development";
        }

        @SuppressWarnings("UnusedDeclaration")
        public String getDefaultParcelBuilderInputFilename() {
            return "parcelsToBuild";
        }

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }
    }
}
