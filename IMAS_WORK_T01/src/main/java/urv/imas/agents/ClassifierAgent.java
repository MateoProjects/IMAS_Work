package urv.imas.agents;

import urv.imas.utils.*;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import weka.classifiers.*;
import weka.core.*;

import java.util.*;



/**
 * This agent implements the Classifier Agent that performs the training and testing requests from the coordinator.
 * @author Team 1: Sergi Albiach, Anna Garriga, Benet Manzanares and Ramon Mateo.
 * @version  $Date: 2022-01-09 14:00:00 +0100 (Barcelona, 09 January 2022) $ $Revision: 1 $
 */
public class ClassifierAgent extends OurAgent
{
    private AID CoordinatorAID;
    protected Classifier classifier = null;
    protected Evaluation eval = null;


    ///////////////////////////////////////////////////////////////// Initialization /////////////////////////////////////////////////////////////////
    /**
     * This method is automatically called when the agent is launched.
     * It initiates the conversation with the CoordinatorAgent.
     */
    protected void setup() {
        RegisterInDF("classifier");

        CoordinatorAID = blockingGetFromDF("coordinator");

        addBehaviour(new OurRequestResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                "Training and test phase", this::prepareResponse));
    }


    ///////////////////////////////////////////////////////////////// Working behaviour /////////////////////////////////////////////////////////////////
    /**
     * Prepare response. Receive message from coordinator agent and return results
     * @param msg The request message.
     */
    protected ACLMessage prepareResponse (ACLMessage msg) {
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        try {
            if (!msg.getSender().equals(CoordinatorAID)) {
                throw new Exception("Sender ["+msg.getSender()+"] different from coordinator ("+CoordinatorAID+")");
            }
            OurMessage content = (OurMessage) msg.getContentObject();
            String type = content.name;
            Object[] args = (Object[]) content.obj;

            // Start training or test
            if (type.equals("train")) {
                Instances dataset = (Instances) args[0];
                int numValidationInstances = (int)args[1];
                double accuracy = train(dataset, numValidationInstances);
                reply.setContentObject(accuracy);
            } else if (type.equals("test")) {
                List<Instances> instances = (List<Instances>)args[0];
                double[] results = test(instances);
                reply.setContentObject(results);
            } else
                throw new Exception("Request type ["+type+"] unknown");

        } catch (Exception e) {
            String errorMessage = "Exception:" + e.getMessage();
            reply.setPerformative(ACLMessage.FAILURE);
            showErrorMessage(errorMessage);
            reply.setContent(errorMessage);
        }

        return reply;
    }


    ///////////////////////////////////////////////////////////////// Training /////////////////////////////////////////////////////////////////
    private double train(Instances dataset, int numValidationInstances) throws Exception{
        showMessage("Starting training");
        // Split into training and validation
        int iniIdx = 0;
        int amountTraining = dataset.numInstances() - numValidationInstances;
        Instances trainDataset = new Instances(dataset, iniIdx, amountTraining);
        iniIdx = iniIdx + amountTraining;
        Instances validationDataset = new Instances(dataset, iniIdx, numValidationInstances);

        // Perform training
        createClassifier(trainDataset);

        // Perform evaluation
        eval = new Evaluation(validationDataset);
        double[] predictions = eval.evaluateModel(classifier, validationDataset);

        return computeAccuracy(predictions, validationDataset);
    }

    /**
     * This method create a weka J48 classifier and train it with the training data.
     * @param trainingData The training data.
     */
    protected void createClassifier(Instances trainingData) {

        try {
            // Create the classifier
            classifier = new weka.classifiers.trees.J48();
            // Train the classifier
            classifier.buildClassifier(trainingData);
        } catch (Exception e) {
            System.err.println("Error creating classifier: " + e.getMessage());
        }
    }

    /**
     * Compute the accuracy of the classifier.
     * @param predictions The predictions that classifier does.
     * @param test_dataset The test data.
     * @return The accuracy. 
     */
    private double computeAccuracy(double[] predictions, Instances test_dataset){
        double correct = 0;
        for (int i=0; i<predictions.length; ++i)
            if (predictions[i] == test_dataset.get(i).classValue())
                correct++;

        return correct/predictions.length;
    }


    ///////////////////////////////////////////////////////////////// Test /////////////////////////////////////////////////////////////////
    /**
     * Evaluate the classifier.
     * @param instances The test data.
     * @return The evaluation.
     */
    private double[] test(List<Instances> instances){
        showMessage("Starting testing");
        int numInstances = instances.size();
        double[] predictions = new double[numInstances];
        for (int i=0; i<numInstances; i++){
            predictions[i] = classifyInstance(instances.get(i).get(0));
        }

        return predictions;
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
            showMessage("ERROR while classifying instance: "+instance+" "+e.getMessage());
        }

        return val;
    }

}


