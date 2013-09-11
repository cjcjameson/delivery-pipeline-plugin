package se.diabol.jenkins.pipeline;

import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.export.Exported;
import se.diabol.jenkins.pipeline.model.Component;
import se.diabol.jenkins.pipeline.model.Pipeline;
import se.diabol.jenkins.pipeline.util.ProjectUtil;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class DeliveryPipelineView extends View {

    private List<ComponentSpec> componentSpecs;
    private int noOfPipelines = 1;
    private boolean showAggregatedPipeline = false;
    private int noOfColumns = 1;

    @DataBoundConstructor
    public DeliveryPipelineView(String name, int noOfColumns, List<ComponentSpec> componentSpecs,
                                int noOfPipelines, boolean showAggregatedPipeline) {
        super(name);
        this.componentSpecs = componentSpecs;
        this.noOfColumns = noOfColumns;
        this.noOfPipelines = noOfPipelines;
        this.showAggregatedPipeline = showAggregatedPipeline;
    }

    public List<ComponentSpec> getComponentSpecs() {
        return componentSpecs;
    }

    public void setComponentSpecs(List<ComponentSpec> componentSpecs) {
        this.componentSpecs = componentSpecs;
    }

    public int getNoOfPipelines() {
        return noOfPipelines;
    }

    public boolean isShowAggregatedPipeline() {
        return showAggregatedPipeline;
    }

    public void setNoOfPipelines(int noOfPipelines) {
        this.noOfPipelines = noOfPipelines;
    }

    public void setShowAggregatedPipeline(boolean showAggregatedPipeline) {
        this.showAggregatedPipeline = showAggregatedPipeline;
    }

    public int getNoOfColumns() {
        return noOfColumns;
    }

    public void setNoOfColumns(int noOfColumns) {
        this.noOfColumns = noOfColumns;
    }

    @Override
    public void onJobRenamed(Item item, String oldName, String newName) {
        for (ComponentSpec componentSpec : componentSpecs) {
            if (componentSpec.getFirstJob().equals(oldName)) {
                componentSpec.setFirstJob(newName);
            }
        }
    }

    @Exported
    public List<Component> getPipelines()
    {
        PipelineFactory pipelineFactory = new PipelineFactory();
        List<Component> components = new ArrayList<>();
        for (ComponentSpec componentSpec : componentSpecs) {
            Jenkins jenkins = Jenkins.getInstance();
            AbstractProject firstJob = jenkins.getItem(componentSpec.getFirstJob(), jenkins, AbstractProject.class);
            Pipeline prototype = pipelineFactory.extractPipeline(componentSpec.getName(), firstJob);
            List<Pipeline> pipelines = new ArrayList<>();
            if(showAggregatedPipeline)
                pipelines.add(pipelineFactory.createPipelineAggregated(prototype));
            pipelines.addAll(pipelineFactory.createPipelineLatest(prototype, noOfPipelines));
            components.add(new Component(componentSpec.getName(), pipelines));
        }
        return components;
    }

    public String getRootUrl() {
        return Jenkins.getInstance().getRootUrl();
    }

    @Override
    public Collection<TopLevelItem> getItems()
    {
        return Jenkins.getInstance().getItems();
    }

    @Override
    public boolean contains(TopLevelItem item)
    {
        return getItems().contains(item);
    }

    @Override
    protected void submit(StaplerRequest req) throws IOException, ServletException, Descriptor.FormException {
        req.bindJSON(this, req.getSubmittedForm());
    }

    @Override
    public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        return getOwner().getPrimaryView().doCreateItem(req, rsp);
    }


    @Extension
    public static class DescriptorImpl extends ViewDescriptor
    {
        public ListBoxModel doFillNoOfColumnsItems(@AncestorInPath ItemGroup<?> context) {
            ListBoxModel options = new ListBoxModel();
            options.add("1", "1");
            options.add("2", "2");
            options.add("3", "3");
            return options;
        }
        public ListBoxModel doFillNoOfPipelinesItems(@AncestorInPath ItemGroup<?> context) {
            ListBoxModel options = new ListBoxModel();
            for(int i = 0; i <= 10; i++) {
                String opt = String.valueOf(i);
                options.add(opt, opt);
            }
            return options;
        }

        @Override
        public String getDisplayName() {
            return "Delivery Pipeline View";
        }
    }


    public static class ComponentSpec extends AbstractDescribableImpl<ComponentSpec>
    {
        private String name;
        private String firstJob;

        @DataBoundConstructor
        public ComponentSpec(String name, String firstJob) {
            this.name = name;
            this.firstJob = firstJob;
        }

        public String getName() {
            return name;
        }

        public String getFirstJob() {
            return firstJob;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setFirstJob(String firstJob) {
            this.firstJob = firstJob;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<ComponentSpec> {
            @Override
            public String getDisplayName() {
                return "";
            }

            public ListBoxModel doFillFirstJobItems(@AncestorInPath ItemGroup<?> context) {
                return ProjectUtil.fillAllProjects(context);
            }

            public FormValidation doCheckName(@QueryParameter String value) {
                if (value != null && !value.trim().equals("")) {
                    return FormValidation.ok();
                } else {
                    return FormValidation.error("Please supply a title!");
                }
            }

        }
    }
}
