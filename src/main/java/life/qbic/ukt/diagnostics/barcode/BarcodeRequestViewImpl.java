package life.qbic.ukt.diagnostics.barcode;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;

import java.util.ArrayList;
import java.util.List;


/**
 * View that will display different barcode request cases for
 * the UKT diagnostics scenario:
 *
 *  1) Request new patientID/sampleID pair
 *     Use case: A new patient will be added to CentraXX and a new
 *     ID tuple is needed.
 *
 *  2) Request new sampleID for already existing patientID
 *     Use case: Repeated NGS run on the same tissue.
 *
 */
public class BarcodeRequestViewImpl implements BarcodeRequestView {

    private RadioButtonGroup<String> taskSelection;

    private VerticalLayout fullView;

    private Button patientIdsampleIdButton;

    private Button createSampleForPatientButton;

    private Panel patientIdField;

    private Panel sampleIdPanel;

    private Label patientIdLabel;

    private Label sampleIdLabel;

    private ProgressBar spinner;

    private Label loadingLabel;

    private HorizontalLayout spinnerContainer;

    private VerticalLayout taskCreatePatientContainer;

    private VerticalLayout taskCreateSampleContainer;

    private List<String> patientIdList = new ArrayList<>();
    private ListDataProvider<String> patientIdDataProvider = new ListDataProvider<>(patientIdList);
    private ComboBox<String> patientIdInputField;

    private Label newSampleIdLabel;

    private Panel newPatientIdPanel;

    private Panel newSampleIdPanel;

    public BarcodeRequestViewImpl() {
        initView();
    }

    private void initView() {
        // Init components
        createTaskSelectionView();

        patientIdInputField = new ComboBox<>();
        patientIdInputField.setWidth("100%");
        patientIdInputField.setPlaceholder("Enter patient ID here");
        patientIdInputField.setDataProvider(patientIdDataProvider);
        newSampleIdLabel = new Label("<i>ID will be displayed after request.</i>", ContentMode.HTML);
        newPatientIdPanel = new Panel("Patient ID");
        newSampleIdPanel = new Panel("Sample ID");
        spinner = new ProgressBar();
        loadingLabel = new Label();
        taskCreatePatientContainer = new VerticalLayout();
        taskCreateSampleContainer = new VerticalLayout();

        spinnerContainer = new HorizontalLayout();
        spinnerContainer.addComponents(spinner, loadingLabel);
        spinnerContainer.setSpacing(true);

        patientIdsampleIdButton = new Button("Create new Patient/Sample ID pair");
        createSampleForPatientButton = new Button("Create new Sample ID for patient");
        patientIdField = new Panel();
        sampleIdPanel = new Panel();
        fullView = new VerticalLayout();
        patientIdLabel = new Label("<i>ID will be displayed after request.</i>", ContentMode.HTML);
        sampleIdLabel = new Label("<i>ID will be displayed after request.</i>", ContentMode.HTML);
        HorizontalLayout panelContainer = new HorizontalLayout();
        HorizontalLayout panelContainerTask2 = new HorizontalLayout();

        // Add components
        panelContainer.addComponents(patientIdField, sampleIdPanel);
        panelContainerTask2.addComponents(newPatientIdPanel, newSampleIdPanel);
        fullView.addComponent(new Label("<h1>UKT diagnostics ID request sheet</h1>", ContentMode.HTML));

        // Compose new patient request layout form
        taskCreatePatientContainer.addComponents(patientIdsampleIdButton, panelContainer);
        taskCreatePatientContainer.setSpacing(true);
        taskCreatePatientContainer.setVisible(false);

        // Compose new sample request layout form;
        taskCreateSampleContainer.addComponents(createSampleForPatientButton, panelContainerTask2);
        taskCreateSampleContainer.setSpacing(true);
        taskCreateSampleContainer.setVisible(false);

        // Compose new sample request layout form
        fullView.addComponents(taskSelection, taskCreatePatientContainer, taskCreateSampleContainer, spinnerContainer);
        fullView.setSpacing(true);

        // we want a spinner not a progress bar
        spinner.setIndeterminate(true);
        spinnerContainer.setVisible(false);

        patientIdField.setCaption("Patient ID");
        patientIdField.setWidth("300px");
        sampleIdPanel.setCaption("Sample ID");
        sampleIdPanel.setWidth("300px");
        panelContainer.setSpacing(true);

        newPatientIdPanel.setWidth("300px");
        newSampleIdPanel.setWidth("300px");
        panelContainerTask2.setSpacing(true);

        VerticalLayout innerPatientIdLayout = new VerticalLayout();
        innerPatientIdLayout.addComponent(patientIdLabel);
        innerPatientIdLayout.setMargin(true);
        patientIdField.setContent(innerPatientIdLayout);
        patientIdField.setIcon(VaadinIcons.USER);


        VerticalLayout innerSampleIdLayout = new VerticalLayout();
        innerSampleIdLayout.addComponent(sampleIdLabel);
        innerSampleIdLayout.setMargin(true);
        sampleIdPanel.setContent(innerSampleIdLayout);
        sampleIdPanel.setIcon(VaadinIcons.FILE_O);

        VerticalLayout innerPatientIdLayoutTask2 = new VerticalLayout();
        innerPatientIdLayoutTask2.addComponent(patientIdInputField);
        innerPatientIdLayoutTask2.setMargin(true);
        newPatientIdPanel.setContent(innerPatientIdLayoutTask2);
        newPatientIdPanel.setIcon(VaadinIcons.USER);

        VerticalLayout innerSampleIdLayoutTask2 = new VerticalLayout();
        innerSampleIdLayoutTask2.addComponent(newSampleIdLabel);
        innerSampleIdLayoutTask2.setMargin(true);
        innerSampleIdLayoutTask2.setComponentAlignment(newSampleIdLabel, Alignment.MIDDLE_LEFT);
        innerPatientIdLayoutTask2.setHeight(100, Sizeable.Unit.PERCENTAGE);
        newSampleIdPanel.setContent(innerSampleIdLayoutTask2);
        newSampleIdPanel.setHeight(100, Sizeable.Unit.PERCENTAGE);
        newSampleIdPanel.setIcon(VaadinIcons.FILE_O);



    }

    private void createTaskSelectionView() {
        taskSelection = new RadioButtonGroup("Choose what you want to do");
        taskSelection.setItems("Request new patient/sample ID pair (Creates new patient ID!)",
                "Create new DNA sample for an existing patient");
    }

    @Override
    public Label getPatientIdField() {
        return this.patientIdLabel;
    }

    @Override
    public Label getSampleIdField() {
        return this.sampleIdLabel;
    }

    @Override
    public RadioButtonGroup<String> getTaskSelectionGroup() {
        return this.taskSelection;
    }

    @Override
    public VerticalLayout getFullView() {
        return fullView;
    }

    @Override
    public Button getPatentIdSampleIdButton() {
        return this.patientIdsampleIdButton;
    }

    @Override
    public ProgressBar getSpinner() {
        return this.spinner;
    }

    @Override
    public Label getLoadingLabel() {
        return this.loadingLabel;
    }

    @Override
    public HorizontalLayout getSpinnerContainer() {
        return this.spinnerContainer;
    }

    @Override
    public VerticalLayout getCreatePatientContainer() {
        return this.taskCreatePatientContainer;
    }

    @Override
    public VerticalLayout getCreateSampleContainer() {
        return this.taskCreateSampleContainer;
    }

    @Override
    public Button getCreateSampleButton() {
        return this.createSampleForPatientButton;
    }

    @Override
    public ComboBox<String> getPatientIdInputField() {
        return this.patientIdInputField;
    }

    @Override
    public ListDataProvider<String> getPatientIdDataProvider() {
        return this.patientIdDataProvider;
    }

    @Override
    public Label getNewSampleIdField() {
        return this.newSampleIdLabel;
    }

}
