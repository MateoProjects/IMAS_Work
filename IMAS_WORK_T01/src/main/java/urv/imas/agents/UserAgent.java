package urv.imas.agents;

import urv.imas.utils.*;

import jade.core.AID;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;

import urv.imas.utils.OurMessage;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.converters.ConverterUtils.DataSource;

import javax.xml.parsers.*;
import java.io.File;
import org.w3c.dom.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * This agent implements a simple Uset Agent that loads settings, datasets and starts training and testing. *
 * @author Team 1
 */
public class UserAgent extends OurAgent
{
    // Predefined settings
    private static final String ResourcesFolderPath = "./src/main/resources";
    private static final String SettingsFileName = "settings.xml";

    // Settings
    private String DatasetFileName;
    private int NumClassifiers;
    private int NumTrainingInstances;
    private int NumTestInstances;
    private int NumTestAttributes;
    private int NumInstancesPerClassifier;
    private int NumValidationInstancesPerClassifier;
    private int NumAttributesPerClassifier;

    // Variables
    private AID CoordinatorAID;
    private Instances Dataset;
    private Instances TrainDataset;
    private Instances TestDataset;


    ///////////////////////////////////////////////////////////////// Initialization /////////////////////////////////////////////////////////////////
    protected void setup() {
        RegisterInDF("user");

        // Read settings XML file
        showMessage("Reading settings");
        readSettings();

        // Read dataset specified
        showMessage("Reading dataset");
        readDataset();

        // Find coordinator
        showMessage("Searching for coordinator");
        CoordinatorAID = blockingGetFromDF("coordinator");
        showMessage("Coordinator found: "+ CoordinatorAID);


        // Create the sequential behaviour for the agent life
        SequentialBehaviour sb = new SequentialBehaviour();

        // Training phase
        ACLMessage trainDatasetMsg = startTrainingOrTestMsg(true, TrainDataset);
        sb.addSubBehaviour(new OurRequestInitiator(this, trainDatasetMsg, "Training phase"));

        // Testing phase
        ACLMessage testDatasetMsg = startTrainingOrTestMsg(false, TestDataset);
        sb.addSubBehaviour(new OurRequestInitiator(this, testDatasetMsg, "Testing phase"));

        // Add the sequential behaviour
        addBehaviour(sb);
    }

    protected void readSettings(){
        File file = new File(ResourcesFolderPath+"/"+SettingsFileName);
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);

            NumClassifiers = Integer.parseInt(document.getElementsByTagName("num_classifiers").item(0).getTextContent());
            DatasetFileName = document.getElementsByTagName("dataset_filename").item(0).getTextContent();
            NumTrainingInstances = Integer.parseInt(document.getElementsByTagName("num_training_instances").item(0).getTextContent());
            NumTestInstances = Integer.parseInt(document.getElementsByTagName("num_test_instances").item(0).getTextContent());
            NumTestAttributes = Integer.parseInt(document.getElementsByTagName("num_test_attributes").item(0).getTextContent());
            NumInstancesPerClassifier = Integer.parseInt(document.getElementsByTagName("num_instances_per_classifier").item(0).getTextContent());
            NumValidationInstancesPerClassifier = Integer.parseInt(document.getElementsByTagName("num_validation_instances_per_classifier").item(0).getTextContent());
            NumAttributesPerClassifier = Integer.parseInt(document.getElementsByTagName("num_training_attributes_per_classifier").item(0).getTextContent());
        }catch(Exception e){
            showMessage("ERROR while reading settings:\n" + e.getMessage());
        }
    }

    protected void readDataset(){
        try {
            // Read dataset
            DataSource source = new DataSource(ResourcesFolderPath+"/"+DatasetFileName);
            Dataset = source.getDataSet();

            // Randomize
            Random rng = new Random();
            Dataset.randomize(rng);

            // Split into training and test (https://www.programcreek.com/java-api-examples/?api=weka.filters.Filter Example 3)
            int iniIdx = 0;
            int amount = NumTrainingInstances;
            TrainDataset = new Instances(Dataset, iniIdx, amount);
            iniIdx = iniIdx + amount;
            amount = NumTestInstances;
            TestDataset = new Instances(Dataset, iniIdx, amount);
            TestDataset.deleteAttributeAt(TestDataset.numAttributes()-1);   // Delete class attribute
            TestDataset.insertAttributeAt(new Attribute("class_label"), TestDataset.numAttributes());
            TestDataset.setClassIndex(TestDataset.numAttributes()-1);
        }catch(Exception e){
            showMessage("ERROR while reading dataset:\n" + e.getMessage());
        }
    }


    ///////////////////////////////////////////////////////////////// Training and test /////////////////////////////////////////////////////////////////
    protected ACLMessage startTrainingOrTestMsg(boolean isTraining, Instances dataset){
        // Create content
        OurMessage content;
        String type;
        Object[] args;
        if (isTraining){
            type = "train";
            args = new Object[2];
            args[0] = dataset;
            args[1] = new int[]{NumClassifiers, NumInstancesPerClassifier,
                    NumValidationInstancesPerClassifier, NumAttributesPerClassifier};   // Set settings
        }
        else{
            type = "test";
            args = new Object[1];
            args[0] = generateTestInstances(dataset);
        }

        return createOurMessageRequest(CoordinatorAID, type, args);
    }

    protected Instances[] generateTestInstances(Instances dataset){
        Instances[] testInstances = new Instances[dataset.numInstances()];
        List<Integer> attributesIndexes = IntStream.range(0, dataset.numAttributes()).boxed().collect(Collectors.toList());
        List<Integer> attributesSublist;
        Instances inst;
        String a;
        for (int i=0; i < dataset.numInstances(); i++){
            inst = new Instances(dataset, i, 1);

            // Get a subset of attributes
            Collections.shuffle(attributesIndexes);
            attributesSublist = attributesIndexes.subList(0, NumTestAttributes);
            filterAttributes(inst, attributesSublist);

            testInstances[i] = inst;

        }

        return testInstances;
    }

    protected Instances filterAttributes(Instances inst, List<Integer> desiredAttrs){
        int deleted = 0;
        int total = inst.numAttributes();
        for (int i  = 0; i < total; i++)
            if (!desiredAttrs.contains(i) && (i-deleted) != inst.classIndex())
            {
                inst.deleteAttributeAt(i-deleted);
                deleted += 1;
            }

        return inst;
    }
}