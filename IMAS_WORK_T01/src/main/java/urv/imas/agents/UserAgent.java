package urv.imas.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

import jade.proto.ContractNetInitiator;
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
public class UserAgent extends Agent
{
    // Predefined settings
    private static String ResourcesFolderPath = "./src/main/resources";
    private static String SettingsFileName = "settings.xml";
    private static String CoordName = "coordinator";

    // Settings
    private String DatasetFileName;
    private int NumClassifiers;
    private int NumTrainingInstances;
    private int NumTestInstances;
    private int NumTestAttributes;
    private int NumInstancesPerClassifier;
    private int NumTrainingInstancesPerClassifier;
    private int NumValidationInstancesPerClassifier;
    private int NumAttributesPerClassifier;

    // Variables
    private AID CoordinatorAID;
    private Instances Dataset;
    private Instances TrainDataset;
    private Instances TestDataset;


    ///////////////////////////////////////////////////////////////// Auxiliar methods /////////////////////////////////////////////////////////////////
    public void showMessage(String mss) {
        System.out.println(getLocalName()+" -> "+mss);
    }


    ///////////////////////////////////////////////////////////////// Initialization /////////////////////////////////////////////////////////////////
    protected void setup() {
        // Read creation arguments
        readArguments();

        // Read settings XML file
        showMessage("Reading settings");
        readSettings();

        // Read dataset specified
        showMessage("Reading dataset");
        readDataset();

        // Create the sequential behaviour for the agent life
        SequentialBehaviour sb = new SequentialBehaviour();

        // Start comunication with coordinator (initialization behaviour)
        ACLMessage msg = initCoordinatorMsg();
        sb.addSubBehaviour(new QueryInitiator(this, msg, "Initializing coordinator"));

        // Send training dataset to coordinator
        ACLMessage trainDatasetMsg = createDatasetMessage("train", TrainDataset);
        sb.addSubBehaviour(new TrainingInitiator(this, trainDatasetMsg, "Initializing training phase"));

        // Add the sequential behaviour
        addBehaviour(sb);
    }

    protected void readArguments(){
        Object[] args = getArguments();
        if (args != null && args.length > 0) {} else{}  // TODO: Maybe read XML settings filepath (otherwise remove this method)
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
            NumTrainingInstancesPerClassifier = Integer.parseInt(document.getElementsByTagName("num_training_instances_per_classifier").item(0).getTextContent());
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

    private ACLMessage initCoordinatorMsg() {
        CoordinatorAID = new AID((String) CoordName, AID.ISLOCALNAME);
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver( CoordinatorAID );
        msg.setSender(getAID());

        int[] settings = new int[]{NumClassifiers, NumInstancesPerClassifier, NumTrainingInstancesPerClassifier,
                NumValidationInstancesPerClassifier, NumAttributesPerClassifier};
        OurMessage content = new OurMessage("ini", settings);
        try{
            msg.setContentObject(content);
        }catch(java.io.IOException e){
            showMessage("ERROR while initializing coordinator: "+e.getMessage());
        }

        return msg;
    }

    // Initialization behaviour
    class QueryInitiator extends AchieveREInitiator{
        private String startMsg;
        public QueryInitiator (Agent myAgent, ACLMessage msg, String startMsg)
        {
            super(myAgent, msg);
            this.startMsg = startMsg;
        }

        @Override
        public void onStart() {
            super.onStart();
            showMessage(startMsg);
        }

        protected void handleInform (ACLMessage msg)
        {
            showMessage("'" + msg.getSender().getLocalName()+"' sent: \""+msg.getContent()+"\"");
        }


    }


    ///////////////////////////////////////////////////////////////// Training /////////////////////////////////////////////////////////////////
    class TrainingInitiator extends ContractNetInitiator{
        private String startMsg;
        public TrainingInitiator(Agent myAgent, ACLMessage CfpMsg, String startMsg)
        {
            super(myAgent, CfpMsg);
            this.startMsg = startMsg;
        }

        @Override
        public void onStart() {
            super.onStart();
            showMessage(startMsg);
        }

        protected void handleNotUnderstood (ACLMessage msg) {
            showMessage(msg.getSender().getLocalName()+" did not understand the message");
        }
        protected void handleRefuse (ACLMessage msg) {
            showMessage(msg.getSender().getLocalName()+" refused the message");
        }
        protected void handleInform (ACLMessage msg) {
            showMessage(msg.getSender().getLocalName()+" informs: " + msg.getContent());
        }
        protected void handleFailure (ACLMessage msg) {
            showMessage(msg.getSender().getLocalName()+" failed");
        }
    }

    protected ACLMessage createDatasetMessage(String type, Instances dataset){
        ACLMessage msg = new ACLMessage();
        msg.addReceiver (CoordinatorAID);
        msg.setPerformative(ACLMessage.REQUEST);
        OurMessage content = new OurMessage(type, dataset);
        try
        {
            msg.setContentObject(content);
            msg.setLanguage("JavaSerialization");
        } catch(Exception e){
            showMessage("ERROR while creating dataset message:\n" + e.getMessage());
        }
        return msg;
    }


    ///////////////////////////////////////////////////////////////// Test /////////////////////////////////////////////////////////////////
    // TODO
}




