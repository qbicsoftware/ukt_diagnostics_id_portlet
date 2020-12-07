package life.qbic.ukt.diagnostics.barcode;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.CreationId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.ukt.diagnostics.MyPortletUI;
import life.qbic.ukt.diagnostics.helpers.BarcodeFunctions;
import life.qbic.ukt.diagnostics.helpers.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static life.qbic.ukt.diagnostics.helpers.BarcodeFunctions.checksum;

public class BarcodeRequestModelImpl implements BarcodeRequestModel{

    private final OpenBisClient openBisClient;

    private final static String SPACE = "UKT_DIAGNOSTICS";

    private final static String PROJECTID = "/UKT_DIAGNOSTICS/QUK17";

    private final static String CODE = "QUK17";

    private static final Log log = LogFactoryUtil.getLog(MyPortletUI.class);

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVW".toCharArray();

    private static String[] patientSampleIdPair= null;


    public BarcodeRequestModelImpl(OpenBisClient client){
        this.openBisClient = client;
    }

    @Override
    public void requestNewPatientSampleIdPair() {
        patientSampleIdPair = new String[2];

        int[] sizes = getNumberOfSampleTypes();

        // offset is +2, because there is always an attachment sample per project
        String biologicalSampleCodeBlood = createBarcodeFromInteger(sizes[0] + 1 );
        String biologicalSampleCodeTumor = createBarcodeFromInteger(sizes[0] + 2 );

        // offset is +3, because of the previous created sample and the attachement
        String testSampleCodeBlood = createBarcodeFromInteger(sizes[0] + 3);
        String testSampleCodeTumor = createBarcodeFromInteger(sizes[0] + 4);
        String patientId = CODE + "ENTITY-" + (sizes[1] + 1);

        patientSampleIdPair[0] = patientId;
        patientSampleIdPair[1] = testSampleCodeTumor;

        // Logging code block
        log.debug(String.format("Number of non-entity samples: %s", sizes[0]));
        log.info(String.format("%s: New patient ID created %s", MyPortletUI.info.getPortletInfo(), patientSampleIdPair[0]));
        log.info(String.format("%s: New tumor sample ID created %s", MyPortletUI.info.getPortletInfo(), patientSampleIdPair[1]));
        log.info(String.format("%s: New tumor DNA sample ID created %s", MyPortletUI.info.getPortletInfo(), biologicalSampleCodeTumor));
        log.info(String.format("%s: New blood sample ID created %s", MyPortletUI.info.getPortletInfo(), biologicalSampleCodeBlood));
        log.info(String.format("%s: New blood DNA sample ID created %s", MyPortletUI.info.getPortletInfo(), testSampleCodeBlood));

        // In case registration fails, return null
        
        if(!registerNewPatient(patientId, biologicalSampleCodeTumor,
                biologicalSampleCodeBlood, testSampleCodeTumor, testSampleCodeBlood))
            patientSampleIdPair = null;
    }


    /**
     * Determines the number of non entity samples and total samples for a project.
     *
     * @return An 1D array with 2 entries:
     *          array[0] = number of non entities
     *          array[1] = number of total entities
     */
    private int[] getNumberOfSampleTypes(){
        int[] sizes = new int[2];
        // List<Sample> sampleList = this.getSamplesOfProject(CODE);
        List<Sample> sampleList = this.getSamplesOfProject(PROJECTID);
        List<Sample> entities = getEntities(sampleList);

        String highestID = null;

        if (sampleList.isEmpty())
            return sizes;

        // Filter sample list for Q_BIOLOGICAL_SAMPLE and Q_TEST_SAMPLE
        for(Sample s : sampleList) {
            if (s.getType().getCode().equals("Q_BIOLOGICAL_SAMPLE") || s.getType().getCode().equals("Q_TEST_SAMPLE")) {
                String idCount = s.getCode().substring(5, 9);
                if (highestID == null){
                    highestID = idCount;
                } else {
                    if (isIdHigher(highestID, idCount))
                        highestID = idCount;
                }
            }
        }
        log.info(convertIdToInt(highestID));
        sizes[0] = convertIdToInt(highestID);
        sizes[1] = entities.size();
        return sizes;
    }

    private List<Sample> getSamplesOfProject(String id) {
        List<Sample> result = openBisClient.getSamplesOfProject( id );

        return result == null ? new ArrayList<>() : result;
    }

    private boolean isIdHigher(String idA, String idB){

        // Compare the letter first
        // eg: B < Z 
        if (idB.toCharArray()[3] > idA.toCharArray()[3])
            return true;
        // B < Z
        if (idB.toCharArray()[3] < idA.toCharArray()[3])
            return false;
        // Compare the digits
        // eg. 002 > 001
        if (Integer.parseInt(idB.substring(0,3)) > Integer.parseInt(idA.substring(0,3)))
            return true;
        return false;
    }

    private int convertIdToInt(String id){
        int iterator = Integer.parseInt(id.substring(0, 3));
        char idChar = id.charAt(3);

        int multiplier = 0;
        for (int i=0; i<ALPHABET.length; i++){
            if (idChar == ALPHABET[i])
                multiplier = i;
        }
        return multiplier * 1000 + iterator;  
    }

    @Override
    public String[] getNewPatientSampleIdPair() {
        return patientSampleIdPair;
    }

    @Override
    public boolean checkIfPatientExists(String sampleID) {

        return openBisClient.getSampleByCode( sampleID ) != null;
    }

    @Override
    public String addNewSampleToPatient(String patientID, String filterProperty) {

        Sample res = openBisClient.getSampleWithParentsAndChildren(patientID);

        if (res == null) {
            log.error("Could not find patient with ID: " + patientID);
            return "";
        }

        List<Sample> children = res.getChildren();

        if (children == null || children.isEmpty()) {
            log.error(String.format("Sample list was empty, patient ID %s seems to have no parents or children", patientID));
            return "";
        }

        List<Sample> bioSamples = children.stream()
                .filter(sample -> sample.getType().getCode().equals("Q_BIOLOGICAL_SAMPLE"))
                .filter(sample -> sample.getProperties().containsValue(filterProperty))
                .collect(Collectors.toList());

        if (bioSamples.isEmpty()){
            log.error(String.format("No samples of type 'Q_BIOLOGICAL_SAMPLE' found for patient ID %s.", patientID));
            return "";
        } else if (bioSamples.size() > 1){
            log.error(String.format("More than 1 sample of type 'Q_BIOLOGICAL_SAMPLE' found for patient ID %s.", patientID));
            return "";
        }

        String sampleBarcode = createBarcodeFromInteger(getNumberOfSampleTypes()[0] + 2);

        if (sampleBarcode.isEmpty()){
            log.error("Retrieval of a new sample barcode failed. " +
                    "No new sample for an existing patient was created.");
            return "";
        }

        return registerTestSample(sampleBarcode, bioSamples.get(0).getIdentifier().toString()) ? sampleBarcode : "";
    }

    @Override
    public List<String> getRegisteredPatients() {

        List<Sample> res = openBisClient.getSamplesOfProject(PROJECTID);

        if (res == null)
            return new ArrayList<>();
        else
            return res.stream()
                .filter(sample -> sample.getType().getCode().equals("Q_BIOLOGICAL_ENTITY"))
                .map(Sample::getCode)
                .collect(Collectors.toList());
    }

    /**
     * Registration of a new patient with samples
     * @param patientId A code for the sample type Q_BIOLOGICAL_ENTITY
     * @param biologicalSampleCodeBlood A code for the sample type Q_BIOLOGICAL_SAMPLE (Blood)
     * @param biologicalSampleCodeTumor A code for the sample type Q_BIOLOGICAL_SAMPLE (Tumor)
     * @param testSampleCodeBlood A code for the sample type Q_TEST_SAMPLE (Blood)
     * @param testSampleCodeTumor A code for the sample type Q_TEST_SAMPLE (Tumor)
     * @return True, if registration was successful, else false
     */
    private boolean registerNewPatient(String patientId, String biologicalSampleCodeTumor, String biologicalSampleCodeBlood,
                                       String testSampleCodeTumor, String testSampleCodeBlood) {
        return registerEntity(patientId) &&
               registerBioSample(biologicalSampleCodeTumor, PROJECTID + "/" + patientId, "tumor tissue") &&
               registerTestSample(testSampleCodeTumor, PROJECTID + "/" + biologicalSampleCodeTumor) &&
               registerBioSample(biologicalSampleCodeBlood, PROJECTID + "/" + patientId, "blood") &&
               registerTestSample(testSampleCodeBlood, PROJECTID + "/" + biologicalSampleCodeBlood);
    }

    /**
     * Registration of a new test sample
     * @param testSampleCode A code for the test sample type Q_TEST_SAMPLE
     * @param parent A code for the parent sample type Q_BIOLOGICAL_SAMPLE
     * @return True, if registration was successful, else false
     */
    private boolean registerTestSample(String testSampleCode, String parent) {

        SampleCreation sample = new SampleCreation();
        sample.setCode( testSampleCode );
        sample.setCreationId( new CreationId(testSampleCode) );
        sample.setTypeId( new EntityTypePermId("Q_TEST_SAMPLE") );

        sample.setSpaceId( new SpacePermId(SPACE) );
        sample.setProjectId( new ProjectIdentifier(PROJECTID) );
        sample.setExperimentId( new ExperimentIdentifier(SPACE, CODE, CODE + "E3") );

        sample.setProperty("Q_SAMPLE_TYPE", "DNA");
        sample.setParentIds( Collections.singletonList(new SampleIdentifier(parent)) );

        List<SamplePermId> res = openBisClient.createSamples( Collections.singletonList(sample) );

        return res != null && !res.isEmpty();


        // Alternative creation that could be implemented in openbis-client-lib
        /*
        Map<String, String> properties = Map.of("Q_SAMPLE_TYPE", "DNA");
        List<SampleIdentifier> parents = List.of(new SampleIdentifier(parent));

         SamplePermId res2 = openBisClient.createSample(
                testSampleCode,
                "Q_TEST_SAMPLE",
                SPACE,
                CODE,
                CODE + "E3",
                properties,
                parents);
        */
    }

    /**
     * Registration of a new biological sample
     * @param biologicalSampleCode A code for the sample type Q_BIOLOGICAL_SAMPLE
     * @param parent A code for the parent sample type Q_BIOLOGICAL_ENTITY
     * @return True, if registration was successful, else false
     */
    private boolean registerBioSample(String biologicalSampleCode, String parent, String tissue) {

        SampleCreation sample = new SampleCreation();
        sample.setCode( biologicalSampleCode );
        sample.setCreationId( new CreationId(biologicalSampleCode) );
        sample.setTypeId( new EntityTypePermId("Q_BIOLOGICAL_SAMPLE") );

        sample.setSpaceId( new SpacePermId(SPACE) );
        sample.setProjectId( new ProjectIdentifier(PROJECTID) );
        sample.setExperimentId( new ExperimentIdentifier(SPACE, CODE, CODE + "E2") );

        sample.setProperty("Q_PRIMARY_TISSUE",
                tissue.equals("blood") ? "PBMC" : "TUMOR_TISSUE_UNSPECIFIED");

        sample.setProperty("Q_TISSUE_DETAILED", tissue);
        sample.setParentIds( Collections.singletonList(new SampleIdentifier(parent)) );

        List<SamplePermId> res = openBisClient.createSamples( Collections.singletonList(sample) );

        return res != null && !res.isEmpty();


        // Alternative creation that could be implemented in openbis-client-lib
        /*
        Map<String, String> properties = Map.of(
                "Q_PRIMARY_TISSUE", tissue.equals("blood") ? "PBMC" : "TUMOR_TISSUE_UNSPECIFIED",
                "Q_TISSUE_DETAILED", tissue
        );
        List<SampleIdentifier> parents = List.of(new SampleIdentifier(parent));

         SamplePermId res2 = openBisClient.createSample(
                testSampleCode,
                "Q_TEST_SAMPLE",
                SPACE,
                CODE,
                CODE + "E2",
                properties,
                parents);
        */
    }

    /**
     * Registration of a new test sample
     * @param patientId A code for the sample type Q_BIOLOGICAL_ENTITY
     * @return True, if registration was successful, else false
     */
    private boolean registerEntity(String patientId) {

        Map<String, String> properties = Map.of("Q_NCBI_ORGANISM", "9606");

        SamplePermId res = openBisClient.createSample(
                patientId,
                "Q_BIOLOGICAL_ENTITY",
                SPACE,
                CODE,
                CODE + "E1",
                properties);

        return res != null;


        // Alternative creation which is more verbose
        /*
        SampleCreation sample = new SampleCreation();
        sample.setCode( patientId );
        sample.setCreationId( new CreationId(patientId) );
        sample.setTypeId( new EntityTypePermId("Q_BIOLOGICAL_ENTITY") );

        sample.setSpaceId( new SpacePermId(SPACE) );
        sample.setProjectId( new ProjectIdentifier(PROJECTID) );
        sample.setExperimentId( new ExperimentIdentifier(SPACE, CODE, CODE + "E1") );

        sample.setProperty("Q_NCBI_ORGANISM", "9606");

        List<SamplePermId> res = openBisClient.createSamples( Collections.singletonList(sample) );

        return res != null && !res.isEmpty();
        */
    }


    /**
     * Get a sample list with samples from type
     * 'Q_BIOLOGICAL_ENTITY' from a list of samples
     * @param sampleList The sample list to be filtered
     * @return The filtered list
     */
    private List<Sample> getEntities(List<Sample> sampleList){
        List<Sample> filteredList = new ArrayList<>();

        for(Sample s : sampleList){
            if (s.getType().getCode().equals("Q_BIOLOGICAL_ENTITY")){
                filteredList.add(s);
            }
        }

        return filteredList;

    }

    /**
     * Creates a complete barcode from a given number using
     * the global project code prefix.
     * Checksum calculation and barcode verification is included.
     * @param number An integer number
     * @return A fully formatted valid QBiC barcode
     */
    private String createBarcodeFromInteger(int number){
        int multiplicator = number / 1000;
        char letter = ALPHABET[multiplicator];

        int remainingCounter = number - multiplicator*1000;

        if (remainingCounter > 999 || remainingCounter < 0){
            return "";
        }

        String preBarcode = CODE + Utils.createCountString(remainingCounter, 3) + letter;

        String barcode = preBarcode + checksum(preBarcode);

        if (!BarcodeFunctions.isQbicBarcode(barcode)){
            log.error(String.format("%s: Barcode created from Integer is not a valid barcode: %s",
                    MyPortletUI.info.getPortletInfo(), barcode));
            barcode = "";
        }

        return barcode;

    }

}
