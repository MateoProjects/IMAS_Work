package urv.imas.agents;

import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import urv.imas.utils.*;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetInitiator;


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
        RegisterInDF("classifier");

        CoordinatorAID = new AID((String) CoordName, AID.ISLOCALNAME);  // TODO: Do this using DF

        // Create message for the coordinator
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(CoordinatorAID);
        msg.setSender(getAID());
        msg.setContent("I am CLASSIFIER");

        ParallelBehaviour sb = new ParallelBehaviour();// TODO: Change by sequential

        sb.addSubBehaviour( new OurRequestInitiator(this, msg, "Init phase"));

        sb.addSubBehaviour(new OurRequestResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                "Training and test phase", (x)->{return prepareResponse(x);}));

        addBehaviour(sb);
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
        int iterations = testing.numInstances();
        double[] predictions = new double[iterations];
        for (int i=0; i<iterations; ++i){
            predictions[i] = classifyInstance(testing.get(i));

        }

        return predictions;

    }
    
    private double computeAcuracy(double[] predictions, Instances test_dataset){
        double correct = 0;
        for (int i=0; i<predictions.length; ++i){
            if (predictions[i] == test_dataset.get(i).classValue()){
                correct++;
            }
        }
        return correct/predictions.length;
    }

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
                    Instances trainDataset = new Instances(dataset, iniIdx, amount);
                    iniIdx = iniIdx + amount;
                    amount = 75;
                    Instances testDataset = new Instances(dataset, iniIdx, amount);
                    createClassifier(trainDataset);
                    eval = new Evaluation(testDataset);
                    double[] predictions = eval.evaluateModel(classifier, testDataset);

                    double accuracy = computeAcuracy(predictions, testDataset);
                    reply.setContentObject(accuracy);
                }
                else if (type.equals("test")){
                    double[] results = classifyInstances(dataset);
                    reply.setContentObject(results);
                }

            } catch (Exception e) {
                e.printStackTrace();
                showMessage("Exception during processing");
            }
        }else{
            showMessage("Message was empty!");
        }
      return reply;
    }

}


