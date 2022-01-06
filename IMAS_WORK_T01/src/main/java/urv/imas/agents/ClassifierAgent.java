package urv.imas.agents;

import urv.imas.utils.*;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;

import jade.proto.ContractNetResponder;
import weka.classifiers.*;
import weka.core.*;



/** Inner class QueryInitiator.
 * This class implements the behaviour used by the ClassifierAgent to send a
 * REQUEST message to the CoordinatorAgent.
 * @author Anna Garriga Ramon Mateo
 * @version  $Date: 2010-04-08 13:08:55 +0200 (gio, 08 apr 2010) $ $Revision: 6297 $
 * */
public class ClassifierAgent extends OurAgent
{
    private String CoordName = "coordinator";
    private AID CoordinatorAID;
    private String trainingData;
    protected Classifier classifier = null;
    protected Evaluation eval = null;


    ///////////////////////////////////////////////////////////////// Initialization /////////////////////////////////////////////////////////////////
    /**
     * This method is automatically called when the agent is launched.
     * It initiates the conversation with the CoordinatorAgent.
     */
    protected void setup() {
        // Read name of classifiers as arguments (NOT ACTIVE NOW)
        Object[] args = getArguments();
        if (args != null && args.length > 0) {} else{}

        CoordinatorAID = new AID((String) CoordName, AID.ISLOCALNAME);

        // Create message for the coordinator
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(CoordinatorAID);
        msg.setSender(getAID());
        msg.setContent("I am CLASSIFIER");
        QueryInitiator bh1 = new QueryInitiator(this, msg);
        addBehaviour(bh1);

        addBehaviour(new OurRequestResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                "Training and test phase", (x)->{return prepareResponse(x);}, true));
    }


    ///////////////////////////////////////////////////////////////// Working behaviour /////////////////////////////////////////////////////////////////
    class QueryInitiator extends AchieveREInitiator
    {
        /**
         * Constructor of the class.
         * @param myAgent The agent that is constructing this behaviour.
         * @param msg The message to send.
         */
        public QueryInitiator (Agent myAgent, ACLMessage msg)
        {
            super(myAgent, msg);
        }
        //Mètodes que caldrà implementar amb les accions pertinents
        protected void handleInform (ACLMessage msg)
        {
            showMessage("'" + msg.getSender().getLocalName()+"' sent: \""+msg.getContent()+"\"");
            // receive ACL message from the coordinator to train dataset and dataset from cordinator
            String[] content = msg.getContent().split(" ");

            ACLMessage reply;
            switch (msg.getContent()){
                case "train":
                    ACLMessage trainingData = blockingReceive();
                    //TODO: Recieve training data: 300 instances
                    //TODO: Split train and validation (225 and 75)
                    /*classifier = createClassifier(trainingData);   // TODO: Solventar aixó, perquè segur que peta aqui

                    // retrun accuracy of classifier
                    eval = new Evaluation(validationData);
                    // evaluem el model
                    double accuracy = eval.evaluateModel(classifier);

                    // send prediction to coordinator agent
                    reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(Double.toString(accuracy));
                    send(reply);*/

                    break;
            
                case "classify":
                    ACLMessage classificationData = blockingReceive();
                    /*double result = classifyInstance(classificationData.getContent()); // TODO: Solventar aixó, perquè segur que peta aqui
                    reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(Double.toString(result));
                    send(reply);*/
                    break;
                
                case "getAtributes":
                    reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    //reply.setContent(get_infoClassifier());
                    send(reply);
                    break;

                case "globalInfo":
                    reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    //reply.setContent(classifier.get_globalInfo());
                    send(reply);
                    break;

                case "getGraph":
                    reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    //reply.setContent(classifier.graph());
                    send(reply);
                    break;

                default:    // TODO?
                    break;
            }
    
        }

    }


    /**
     * This method create a weka J48 classifier and train it with the training data.
     * @param trainingData The training data.
     * @return The J48 classifier.
     */
    public Classifier createClassifier(Instances trainingData) { //TODO: fer l'split

        try {
            // Create the classifier
            classifier = new weka.classifiers.trees.J48();
            // Train the classifier
            classifier.buildClassifier(trainingData);
        } catch (Exception e) {
            System.err.println("Error creating classifier: " + e.getMessage());
        }



        return classifier;
    }

    /**
     * Classify the given instance.
     * @param instance instance to classify and predict
     * @return The predicted class.
     */
    public double classifyInstance(Instance instance) {
        double val = -1;
        try{
            val = this.classifier.classifyInstance(instance);
        }catch (Exception e){
            showMessage("ERROR while classifying instance: "+instance);
        }

        return val;
    }

    private double[] classifyInstances(Instances testing){
        int iterations = numInstances(testing);
        double[] predictions = new  double[testing];
        for (int i=0; i<iterations; ++i){
            predictions[i] = classifyInstance(testing.get(i));

        }

        return predictions;

    }
    
    private double computeAcuracy(double[] predictions){
        double correct = 0;
        for (int i=0; i<predictions.length; ++i){
            if (predictions[i] >= 0.5)){
                correct++;
            }
        }
        return correct/predictions.length;
    }

    /**
     * Get info about classifier
     * return String with info about classifier
     */
    /*public String get_infoClassifier() {
        return classifier.get_info();   // TODO: get_info method does not exist
    }*/

    protected ACLMessage prepareResponse (ACLMessage msg) {
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent("Dataset received.");

        if (msg != null) {
            try {
                OurMessage content = (OurMessage) msg.getContentObject();
                String type = content.name;
                Instances dataset = (Instances)content.obj;

                // Start training or test
                if (type.equals("train")){
                    // Split into training and validation (https://www.programcreek.com/java-api-examples/?api=weka.filters.Filter Example 3)
                    int iniIdx = 0;
                    int amount = 225;
                    TrainDataset = new Instances(dataset, iniIdx, amount);
                    iniIdx = iniIdx + amount;
                    amount = 75;
                    TestDataset = new Instances(dataset, iniIdx, amount);
                    createClassifier(TrainDataset);
                    eval = new Evaluation(TestDataset);
                    double accuracy = eval.evaluateModel(classifier);
                    reply.setContentObject(accuracy);
                }
                else if (type.equals("test")){
                    double[] results = classifyInstances(dataset);
                    double accuracy = computeAcuracy(results);
                    reply.setContentObject(accuracy);
                }

            } catch (UnreadableException e) {
                e.printStackTrace();
                showMessage("Could not read message");
            }
        }else{
            showMessage("Message was empty!");
        }
      return reply;
    }


}


