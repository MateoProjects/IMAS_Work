package urv.imas.agents;

import urv.imas.utils.*;

import jade.core.AID;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;

import urv.imas.utils.OurMessage;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import javax.xml.parsers.*;
import java.io.File;
import org.w3c.dom.*;
import java.util.Random;


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
        ACLMessage trainDatasetMsg = startTrainingOrTestMsg("train", TrainDataset);
        sb.addSubBehaviour(new OurRequestInitiator(this, trainDatasetMsg, "Training phase"));

        // Testing phase
        //ACLMessage testDatasetMsg = startTrainingOrTestMsg("test", TestDataset);
        //sb.addSubBehaviour(new OurRequestInitiator(this, testDatasetMsg, "Testing phase"));
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
            Random rnd = Dataset.getRandomNumberGenerator(42);
            Dataset.randomize(rnd);

            // Split into training and test (https://www.programcreek.com/java-api-examples/?api=weka.filters.Filter Example 3)
            int iniIdx = 0;
            int amount = NumTrainingInstances;
            TrainDataset = new Instances(Dataset, iniIdx, amount);
            iniIdx = iniIdx + amount;
            amount = NumTestInstances;
            TestDataset = new Instances(Dataset, iniIdx, amount);
        }catch(Exception e){
            showMessage("ERROR while reading dataset:\n" + e.getMessage());
        }
    }


    ///////////////////////////////////////////////////////////////// Training and test /////////////////////////////////////////////////////////////////
    protected ACLMessage startTrainingOrTestMsg(String type, Instances dataset){
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(CoordinatorAID);

        // Create content
        OurMessage content;
        Object[] args;
        if (type.equals("train")){
            args = new Object[2];
            args[0] = dataset;
            args[1] = new int[]{NumClassifiers, NumInstancesPerClassifier,
                    NumValidationInstancesPerClassifier, NumAttributesPerClassifier};
        }
        else if (type.equals("test")){
            args = new Object[1];
            args[0] = dataset;
        } else {
            showErrorMessage("Unknown type: " + type);
            args = null;
        }
        content = new OurMessage(type, args);

        // Set content and language
        try
        {
            msg.setContentObject(content);
            msg.setLanguage("JavaSerialization");
        } catch(Exception e){
            showErrorMessage("while creating dataset message:\n" + e.getMessage());
        }

        return msg;
    }
}