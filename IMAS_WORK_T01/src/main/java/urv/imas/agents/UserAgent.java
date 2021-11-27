package urv.imas.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import javax.xml.parsers.*;
import java.io.File;
import org.w3c.dom.*;

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
    private int NumTrainingInstances;
    private int NumValidationInstances;
    private int NumTestInstances;
    private int NumTestAttributes;

    // Variables
    private AID CoordinatorAID;
    private Instances Dataset;
    private Instances TrainDataset;
    private Instances ValidationDataset;
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
        readSettings();

        // Read dataset specified
        readDataset();

        // Start comunication with coordinator
        CoordinatorAID = new AID((String) CoordName, AID.ISLOCALNAME);
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM); //Instanciació del missatge
        msg.addReceiver(CoordinatorAID);    //Afegim un receptor (suposem que el seu AID ja l’havíem trobat abans
        msg.setSender(getAID());    //Ens posem a nosaltres com a emissor
        msg.setContent("I am USER");    //Indiquem el contingut (String sense estructura)

        // Create initializaiton behaviour
        QueryInitiator bh1 = new QueryInitiator(this, msg);
        addBehaviour(bh1);
    }

    protected void readArguments(){
        Object[] args = getArguments();
        if (args != null && args.length > 0) {} else{}  // TODO
    }

    protected void readSettings(){
        showMessage("Reading settings...");
        File file = new File(ResourcesFolderPath+"/"+SettingsFileName);
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);

            DatasetFileName = document.getElementsByTagName("dataset_filename").item(0).getTextContent();
            NumTrainingInstances = Integer.parseInt(document.getElementsByTagName("num_training_instances").item(0).getTextContent());
            NumValidationInstances = Integer.parseInt(document.getElementsByTagName("num_validation_instances").item(0).getTextContent());
            NumTestInstances = Integer.parseInt(document.getElementsByTagName("num_test_instances").item(0).getTextContent());
            NumTestAttributes = Integer.parseInt(document.getElementsByTagName("num_training_attributes").item(0).getTextContent());
        }catch(Exception e){showMessage(e.getMessage());}
    }

    protected void readDataset(){
        showMessage("Reading dataset...");
        try {
            // Read dataset
            DataSource source = new DataSource(ResourcesFolderPath+"/"+DatasetFileName);
            Dataset = source.getDataSet();
            if (Dataset.classIndex() == -1) {   // Set class index
                Dataset.setClassIndex(Dataset.numAttributes() - 1);
            }
            showMessage(Dataset.toSummaryString());

            // Split dataset (https://www.programcreek.com/java-api-examples/?api=weka.filters.Filter Example 3)
            int iniIdx = 0;
            int endIdx = NumTrainingInstances;
            TrainDataset = new Instances(Dataset, iniIdx, endIdx);
            iniIdx = endIdx;
            endIdx += NumValidationInstances;
            ValidationDataset = new Instances(Dataset, iniIdx, endIdx);
            iniIdx = endIdx;
            endIdx += NumTestInstances;
            TestDataset = new Instances(Dataset, iniIdx, endIdx);
        }catch(Exception e){showMessage(e.getMessage());}
    }

    // Initialization behaviour
    class QueryInitiator extends AchieveREInitiator
    {
        public QueryInitiator (Agent myAgent, ACLMessage msg)
        {
            super(myAgent, msg);
        }

        protected void handleInform (ACLMessage msg)
        {
            showMessage("'" + msg.getSender().getLocalName()+"' sent: \""+msg.getContent()+"\"");
        }
    }
}


