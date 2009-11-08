package org.artifactory.webapp.wicket.page.config.repos.remote;

import com.google.common.collect.Lists;
import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.ajax.ConfirmationAjaxCallDecorator;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import static org.artifactory.common.wicket.component.modal.ModalHandler.resizeAndCenterCurrent;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.component.table.columns.TextFieldColumn;
import org.artifactory.common.wicket.component.table.columns.TooltipLabelColumn;
import org.artifactory.common.wicket.component.table.columns.checkbox.SelectAllCheckboxColumn;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.ComponentUtils;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.webapp.wicket.page.config.repos.RepositoryConfigPage;
import org.artifactory.webapp.wicket.util.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.util.validation.UriValidator;
import org.artifactory.webapp.wicket.util.validation.XsdNCNameValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Enables the user to import shared remote repository settings from a remote artifactory instance
 *
 * @author Noam Y. Tenne
 */
public class RemoteRepoImportPanel extends BaseModalPanel {

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private RepositoryService repositoryService;

    private RepoDataProvider provider;
    private SortableTable repoTable;

    @WicketProperty
    private String url;
    private TitledAjaxSubmitLink importButton;

    public RemoteRepoImportPanel(MutableCentralConfigDescriptor mutableConfig) {
        setWidth(740);
        add(new CssClass("import-remot-repo-panel"));
        Form loadForm = new Form("loadForm");
        add(loadForm);

        MarkupContainer loadBorder = new TitledBorder("loadBorder");
        loadForm.add(loadBorder);

        loadBorder.add(new HelpBubble("urlHelp",
                "Enter the base URL of another Artifactory server you wish to import repository definitions from."));
        FormComponent urlTextField = new TextField("url", new PropertyModel(this, "url"));
        urlTextField.add(new UriValidator("http", "https"));
        urlTextField.setPersistent(true);
        urlTextField.setOutputMarkupId(true);
        urlTextField.setRequired(true);
        urlTextField.setModelObject("http://repo.jfrog.org/artifactory");
        loadBorder.add(urlTextField);
        loadBorder.add(getLoadButton(loadForm));

        Form listForm = new Form("listForm");
        add(listForm);

        MarkupContainer listBorder = new TitledBorder("listBorder");
        listForm.add(listBorder);
        createRepositoryList(listBorder);

        add(new ModalCloseLink("cancel"));
        //Submit button
        importButton = getImportButton(mutableConfig, listForm);
        importButton.setOutputMarkupId(true);
        add(importButton);
        listForm.add(new DefaultButtonBehavior(importButton));
        loadForm.loadPersistentFormComponentValues();
    }


    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        ComponentUtils.updatePersistentFormComponents(this);
    }

    @Override
    public String getTitle() {
        return "Import Remote Repositories";
    }

    /**
     * Constructs and returns the repository list loading button
     *
     * @param form Form to associate button to
     * @return Load button
     */
    private Component getLoadButton(Form form) {
        return new TitledAjaxSubmitLink("load", "Load", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                Session.get().cleanupFeedbackMessages();

                //Reset the provider data
                provider.setData(Lists.<ImportableRemoteRepo>newArrayList());
                try {
                    List<ImportableRemoteRepo> importableRepoList = getSharedRemoteRepos();
                    if (importableRepoList.isEmpty()) {
                        warn("No shared repositories could be found.");
                        return;
                    }
                    provider.setData(importableRepoList);
                    target.addComponent(repoTable);
                    resizeAndCenterCurrent(target);
                } catch (Exception e) {
                    error("An error occured while locating shared repositories: " + e.getMessage());
                } finally {
                    AjaxUtils.refreshFeedback(target);
                }
            }
        };
    }

    /**
     * Constructs and returns the repository import button
     *
     * @param form Form to associate button to
     * @return Import button
     */
    private TitledAjaxSubmitLink getImportButton(final MutableCentralConfigDescriptor mutableConfig, Form form) {
        return new TitledAjaxSubmitLink("import", "Import", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    Session.get().cleanupFeedbackMessages();
                    List<ImportableRemoteRepo> importableRepos = provider.getData();

                    //Validate selected repo lists
                    OrderedMap<String, RemoteRepoDescriptor> reposToImport =
                            validatedAndReturnSelection(importableRepos);

                    //Add new imported repositories if the list isn't empty
                    if (!reposToImport.isEmpty()) {
                        int selectedRepoCount = reposToImport.size();
                        OrderedMap<String, RemoteRepoDescriptor> existingRepos =
                                mutableConfig.getRemoteRepositoriesMap();
                        Collection<RepoPath> reposToZap = new ArrayList<RepoPath>();

                        //Remove existing remote repositories that will be re-imported
                        for (RemoteRepoDescriptor repoToImport : reposToImport.values()) {
                            String key = repoToImport.getKey();
                            existingRepos.remove(key);

                            //Add the repo to the zap list
                            if (repoToImport.isStoreArtifactsLocally()) {
                                reposToZap.add(new RepoPath(key + "-cache", ""));
                            }
                        }

                        //Re-Add existing repositories to the import list
                        reposToImport.putAll(existingRepos);
                        mutableConfig.setRemoteRepositoriesMap(reposToImport);

                        centralConfigService.saveEditedDescriptorAndReload(mutableConfig);

                        //Zap all the new repositories
                        for (RepoPath repoToZap : reposToZap) {
                            repositoryService.zap(repoToZap);
                        }

                        getPage().info(String.format("Successfully imported %s remote repository definitions.",
                                selectedRepoCount));
                        ((RepositoryConfigPage) getPage()).refresh(target);
                        close(target);
                    }
                } finally {
                    AjaxUtils.refreshFeedback(target);
                }
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                //If a repo which key already exists as another remote repo key is selected, warn
                List<ImportableRemoteRepo> importableRepos = provider.getData();
                for (ImportableRemoteRepo importableRepo : importableRepos) {
                    if (importableRepo.isSelected() && importableRepo.isExistsAsRemote()) {
                        return new ConfirmationAjaxCallDecorator(getString("existsAsRemoteWarn"));
                    }
                }
                return super.getAjaxCallDecorator();
            }
        };
    }

    /**
     * Validates and returns the selected repositories out of the list
     *
     * @param importableRepos List of importable repositories
     * @return repos map
     */
    private OrderedMap<String, RemoteRepoDescriptor> validatedAndReturnSelection(Collection<ImportableRemoteRepo>
            importableRepos) {
        OrderedMap<String, RemoteRepoDescriptor> map = new ListOrderedMap<String, RemoteRepoDescriptor>();

        if (importableRepos.isEmpty()) {
            error("Please select at least one repository to import.");
            return map;
        }

        //If a repo which key already exists as another local\virtual repo key is selected, throw an error
        for (ImportableRemoteRepo importableRepo : importableRepos) {
            if (importableRepo.isSelected()) {
                if (importableRepo.isExistsAsLocal() || importableRepo.isExistsAsVirtual()) {
                    error(getString("existsAsLocalOrVirtualWarn"));
                    map.clear();
                    return map;
                }

                map.put(importableRepo.getRepoKey(), importableRepo.getRepoDescriptor());
            }
        }

        if (map.isEmpty()) {
            error("Please select at least one repository to import.");
        }

        return map;
    }

    /**
     * Returns the list of shared remote repositories from the entered URL
     *
     * @return List of importable repositories
     */
    private List<ImportableRemoteRepo> getSharedRemoteRepos() {
        //Append headers
        Map<String, String> headersMap = WicketUtils.getHeadersMap();
        List<RemoteRepoDescriptor> remoteRepoList = repositoryService.getSharedRemoteRepoConfigs(url, headersMap);
        List<ImportableRemoteRepo> importableRepoList = Lists.newArrayList();

        //Indicate if an importable repository key already exists as a local\remote repository's key
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepoList) {
            ImportableRemoteRepo importableRemoteRepo = new ImportableRemoteRepo(remoteRepoDescriptor);

            validateRepoKey(importableRemoteRepo);

            importableRepoList.add(importableRemoteRepo);
        }
        return importableRepoList;
    }

    /**
     * Validate the importable repo key
     *
     * @param importableRemoteRepo Importable repo to validate
     */
    private void validateRepoKey(ImportableRemoteRepo importableRemoteRepo) {
        //Indicate if the key already exists as any type
        boolean existsAsLocal = repositoryService.localRepoDescriptorByKey(importableRemoteRepo.getRepoKey()) != null;
        boolean existsAsRemote = false;
        if (!existsAsLocal) {
            existsAsRemote = repositoryService.remoteRepoDescriptorByKey(importableRemoteRepo.getRepoKey()) != null;
        }
        boolean existsAsVirtual = false;
        if (!existsAsLocal && !existsAsRemote) {
            existsAsVirtual = repositoryService.virtualRepoDescriptorByKey(importableRemoteRepo.getRepoKey()) != null;
        }
        importableRemoteRepo.setExistsAsLocal(existsAsLocal);
        importableRemoteRepo.setExistsAsRemote(existsAsRemote);
        importableRemoteRepo.setExistsAsVirtual(existsAsVirtual);
    }

    /**
     * Constructs the repository list
     *
     * @param listBorder Border to add the list to
     */
    private void createRepositoryList(MarkupContainer listBorder) {
        provider = new RepoDataProvider();
        repoTable = new SortableTable("repoTable", getColumns(), provider, 10);
        repoTable.setOutputMarkupId(true);
        listBorder.add(repoTable);
    }

    /**
     * Returns a list of columns for the repository list
     *
     * @return Columns list
     */
    private List<IColumn> getColumns() {
        List<IColumn> columns = Lists.newArrayList();

        columns.add(new SelectAllCheckboxColumn<ImportableRemoteRepo>("", "selected", null) {
            @Override
            protected void onUpdate(ImportableRemoteRepo rowObject, boolean value, AjaxRequestTarget target) {
                super.onUpdate(rowObject, value, target);
                //On each update, refresh import button to customize the warning messages of the call decorator
                target.addComponent(importButton);
            }

            @Override
            protected void onSelectAllUpdate(AjaxRequestTarget target) {
                target.addComponent(importButton);
            }
        });
        columns.add(new KeyTextFieldColumn());
        columns.add(new TooltipLabelColumn(new Model("Url"), "repoUrl", "repoUrl", 50));
        columns.add(new TooltipLabelColumn(new Model("Description"), "repoDescription", "repoDescription", 25));
        return columns;
    }

    /**
     * Editable repo key text field column
     */
    private class KeyTextFieldColumn extends TextFieldColumn<ImportableRemoteRepo> {
        private KeyTextFieldColumn() {
            super("Key", "repoKey", "repoKey");
        }

        @Override
        protected FormComponent newTextField(String id, IModel model, final ImportableRemoteRepo rowObject) {
            FormComponent textField = super.newTextField(id, model, rowObject);
            textField.setLabel(new Model("Key"));
            textField.setOutputMarkupId(true);
            textField.setRequired(true);
            textField.add(new JcrNameValidator("Invalid repository key '%s'."));
            textField.add(new XsdNCNameValidator("Invalid repository key '%s'."));
            textField.add(new AjaxFormComponentUpdatingBehavior("onkeyup") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    validateRepoKey(rowObject);
                    //On each update, refresh import button to customize the warning messages of the call decorator
                    target.addComponent(importButton);
                }

                @Override
                protected void onError(AjaxRequestTarget target, RuntimeException e) {
                    super.onError(target, e);
                    AjaxUtils.refreshFeedback();
                }

                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    return new NoAjaxIndicatorDecorator();
                }
            });
            return textField;
        }
    }

    /**
     * The importable repository data provider
     */
    private class RepoDataProvider extends SortableDataProvider {

        /**
         * Main content list
         */
        private List<ImportableRemoteRepo> list = Lists.newArrayList();

        /**
         * Default constructor
         */
        private RepoDataProvider() {
            setSort("repoKey", true);
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(list, getSort());
            List<ImportableRemoteRepo> listToReturn = list.subList(first, first + count);
            return listToReturn.iterator();
        }

        public int size() {
            return list.size();
        }

        public IModel model(Object object) {
            return new Model((ImportableRemoteRepo) object);
        }

        /**
         * Returns the data list
         *
         * @return List of importable repositories
         */
        public List<ImportableRemoteRepo> getData() {
            return list;
        }

        /**
         * Sets the given data to the list
         *
         * @param dataToSet List of importable repositories to set
         */
        private void setData(Collection<ImportableRemoteRepo> dataToSet) {
            list.clear();
            list.addAll(dataToSet);
        }
    }
}