package urv.imas.agents;

import urv.imas.utils.*;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


import weka.classifiers.*;
import weka.core.*;



/** Inner class OurAgent.
 * This class implements the behaviour used by the ClassifierAgent to send a
 * REQUEST message to the CoordinatorAgent.
 * @author Anna Garriga & Ramon Mateo
 * @version  $Date: 2022-01-08 
 * */
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
                "Training and test phase","TRAIN-PHASE", this::prepareResponse));
    }


    ///////////////////////////////////////////////////////////////// Working behaviour /////////////////////////////////////////////////////////////////
    /**
     * Prepare response. Receive message from coordinator agent and return results
     * @param request The request message.
     */
    protected ACLMessage prepareResponse (ACLMessage msg) {
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        try {
            OurMessage content = (OurMessage) msg.getContentObject();
            String type = content.name;
            Object[] args = (Object[])content.obj;
            Instances dataset = (Instances)args[0];

            // Start training or test
            if (type.equals("train")){
                double accuracy = train(dataset, args);

                // Set answer
                reply.setContentObject(accuracy);
                reply.setConversationId("TRAIN-PHASE");
            }
            else if (type.equals("test")){
                double[] results = predict(dataset);
                reply.setContentObject(results);
                reply.setConversationId("TEST-PHASE");
            }
            else{
                String errorMessage = "Type ["+type+"] is unknown";
                reply.setPerformative(ACLMessage.FAILURE);
                showErrorMessage(errorMessage);
                reply.setContent(errorMessage);
            }

        } catch (Exception e) {
            String errorMessage = "Exception "+e.getMessage();
            reply.setPerformative(ACLMessage.FAILURE);
            showErrorMessage(errorMessage);
            reply.setContent(errorMessage);
        }

        return reply;
    }


    private double train(Instances dataset, Object[] args) throws Exception{
        // Split into trianing and validation
        int numValidationInstances = (int)args[1];
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
     * @return The J48 classifier.
     */
    public Classifier createClassifier(Instances trainingData) {

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
     * Evaluate the classifier.
     * @param testing The test data.
     * @return The evaluation.
     */
    private double[] predict(Instances testing){
        int numIterations = testing.numInstances();
        double[] predictions = new double[numIterations];
        for (int i=0; i<numIterations; ++i)
            predictions[i] = classifyInstance(testing.get(i));

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
            showMessage("ERROR while classifying instance: "+instance);
        }

        return val;
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


}


