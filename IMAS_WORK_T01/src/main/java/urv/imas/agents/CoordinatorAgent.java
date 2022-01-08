package urv.imas.agents;

import urv.imas.utils.*;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.AgentController;
import weka.core.Attribute;
import weka.core.Instances;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;



/**
 * This agent implements a simple Ping Agent that registers itself with the DF and
 * then waits for ACLMessages.
 * If  a REQUEST message is received containing the string "ping" within the content
 * then it replies with an INFORM message whose content will be the string "pong".
 *
 * @author Tiziana Trucco - CSELT S.p.A.
 * @version  $Date: 2010-04-08 13:08:55 +0200 (gio, 08 apr 2010) $ $Revision: 6297 $
 */
public class CoordinatorAgent extends OurAgent
{
    // Settings
    private int NumClassifiers;
    private int NumInstancesPerClassifier;
    private int NumValidationInstancesPerClassifier;
    private int NumAttributesPerClassifier;
    private int NumAttributesDataset;

    private List <AID> ClassifiersAIDs;
    private Instances TrainDataset;
    private Instances TestDataset;
    private List<Attribute> [] classifiersAttributes;
    private List<Integer> [] classifiersAttributesInteger;
    private Instances [] classifiersInstances;


    ///////////////////////////////////////////////////////////////// Initialization /////////////////////////////////////////////////////////////////
    protected void setup() {
        RegisterInDF("coordinator");

        // Create the behaviour for the agent life
        addBehaviour(new OurRequestResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                "Training and test phase", this::workingCallback));
    }


    ///////////////////////////////////////////////////////////////// Working behaviour /////////////////////////////////////////////////////////////////
    protected ACLMessage workingCallback(ACLMessage msg){
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        try {
            OurMessage content = (OurMessage) msg.getContentObject();
            String type = content.name;
            Object[] args = (Object[])content.obj;
            Instances dataset = (Instances)args[0];

            // Start training or test
            if (type.equals("train")){
                initialization(args[1]);
                train(dataset);
                reply.setContent("Training done");
            }
            else if (type.equals("test")){
                double[] predictions = test(dataset);
                reply.setContentObject(predictions);
            }
            else{
                String errorMsg = "Request type "+type+" unknown";
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent(errorMsg);
                showErrorMessage(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Could not read the message "+msg+" due to exception:\n"+e.getMessage();
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent(errorMsg);
            showErrorMessage(errorMsg);
        }

        return reply;
    }


    protected void initialization(Object args){
        int[] settings = (int[])args;
        NumClassifiers = settings[0];
        NumInstancesPerClassifier = settings[1];
        NumValidationInstancesPerClassifier = settings[2];
        NumAttributesPerClassifier = settings[3];

        createClassifiers();

        getClassifiersAIDs();
    }

    // Reference from the virtual campus: https://campusvirtual.urv.cat/mod/page/view.php?id=2931726 (modified for creating agents in the current container)
    protected void createClassifiers(){
        showMessage("Creating "+NumClassifiers+" classifiers");
        String classifierName = "";
        String className = "urv.imas.agents.ClassifierAgent";
        Object[] arguments = new Object[2];
        arguments[0] = getLocalName();

        jade.wrapper.AgentContainer containerController = getContainerController();  // Get current container
        AgentController agentController;
        try{

            for (int i = 0; i < NumClassifiers; i++){
                classifierName = "classifier"+i;
                agentController = containerController.createNewAgent(classifierName, className, arguments);
                agentController.start();
            }
            TimeUnit.SECONDS.sleep(1);

        }catch (Exception e){
            showErrorMessage("while creating classifier "+classifierName+"\n"+e.getMessage());
        }
    }

    private void getClassifiersAIDs() {
        ClassifiersAIDs = new LinkedList<>();

        jade.util.leap.List classifiers;
        do{
            classifiers = getFromDF("classifier");
        }while(classifiers.size() < NumClassifiers);
        showMessage("There are "+classifiers.size()+" agents for training");

        for(int c=0; c < classifiers.size(); c++)
            ClassifiersAIDs.add((AID) classifiers.get(c));
    }


    protected void train(Instances dataset){
        // Create the parallel behaviour for classifier creation
        ParallelBehaviour pb = new ParallelBehaviour();

        showMessage("Starting train");
        TrainDataset = dataset;
        //BE CAREFUL WITH -1, IF WE REMOVE THE CLASS LATER WE SHOULD REMOVE IT HERE TOO
        NumAttributesDataset = dataset.numAttributes()-1;

        Random rng = new Random(42);
        classifiersAttributes = new List[NumClassifiers];
        classifiersAttributesInteger = new List[NumClassifiers];
        classifiersInstances = new Instances[NumClassifiers];

        List<Attribute> attributes = Collections.list(dataset.enumerateAttributes());
        List<Integer> attributesIndexes = IntStream.range(0, NumAttributesDataset).boxed().collect(Collectors.toList());
        List<Integer> attributesIndexesOriginal = IntStream.range(0, dataset.numAttributes()).boxed().collect(Collectors.toList());

        Collections.shuffle(attributesIndexes);
        int start = 0;
        int end = NumAttributesPerClassifier;

        // SELECT INDICES IN A WAY THAT EVERY ONE IS SELECTED AT LEAST ONCE. THEN IS RANDOM
        for(int c=0; c < NumClassifiers; c++) {
            // Get selected attributes indexes
            if (start == attributes.size() - 1) {
                Collections.shuffle(attributesIndexes);
                classifiersAttributesInteger[c] = attributesIndexes.subList(0, NumAttributesPerClassifier);
            } else {
                classifiersAttributesInteger[c] = attributesIndexes.subList(start, end);
                start += NumAttributesPerClassifier;
                end += NumAttributesPerClassifier;
                if (start != attributes.size() - 1 && end >= attributes.size()) {
                    end = attributes.size() - 1;
                    start = end - NumAttributesPerClassifier;
                }
            }
            classifiersAttributes[c] = classifiersAttributesInteger[c].stream().map(attributes::get).collect(Collectors.toList());
            dataset.randomize(rng);
            classifiersInstances[c] = new Instances(dataset, 0, NumInstancesPerClassifier);
            classifiersInstances[c].setClassIndex(classifiersInstances[c].numAttributes() - 1);
            classifiersInstances[c] = deleteAttributes(classifiersInstances[c],classifiersAttributesInteger[c]);

            // Send training to classifier
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver (ClassifiersAIDs.get(c));
            Object[] cont = new Object[2];
            cont[0] = classifiersInstances[c];
            cont[1] = NumValidationInstancesPerClassifier;
            OurMessage content = new OurMessage("train",  cont);
            try
            {
                msg.setContentObject(content);
                msg.setLanguage("JavaSerialization");
            } catch(Exception e){
                showErrorMessage("while creating dataset message:\n" + e.getMessage());
            }

            pb.addSubBehaviour(new OurRequestInitiator(this, msg, "Training phase for classifier " + c,(this::printResults)));
        }
        addBehaviour(pb);
    }

    private Instances deleteAttributes(Instances classifiersInstances,List<Integer> classifierAttributes)
    {
        int deleted = 0;
        int total = NumAttributesDataset;
        for (int i  = 0; i < total; i++)
        {
            if (!classifierAttributes.contains(i) && classifiersInstances.classIndex() != (i-deleted))
            {
                classifiersInstances.deleteAttributeAt(i-deleted);
                deleted += 1;
            }
        }
        return classifiersInstances;
    }

    private void printResults(ACLMessage msg){
        try {
            double accuracy  = (double) msg.getContentObject();
            showMessage("Predictions for " + msg.getSender().getLocalName() + ": "+ accuracy);
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
    }


    protected double[] test(Instances dataset){
        showMessage("Starting test");
        TestDataset = dataset;
        double[] results = new double[TestDataset.numInstances()];

        return results;
    }
}


