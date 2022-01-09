package urv.imas.agents;

import urv.imas.utils.*;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import weka.classifiers.trees.J48;
import weka.classifiers.*;
import weka.core.*;

import java.util.*;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import weka.classifiers.*;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;

/**
 * This agent implements the Classifier Agent that performs the training and testing requests from the coordinator.
 * @author Team 1: Sergi Albiach, Anna Garriga, Benet Manzanares and Ramon Mateo.
 * @version  $Date: 2022-01-09 14:00:00 +0100 (Barcelona, 09 January 2022) $ $Revision: 1 $
 */
public class ClassifierAgent extends OurAgent
{
    private AID CoordinatorAID;
    protected J48 Tree = null;
    protected Evaluation Eval = null;


    ///////////////////////////////////////////////////////////////// Initialization /////////////////////////////////////////////////////////////////
    /**
     * This method is automatically called when the agent is launched.
     * It initiates the conversation with the CoordinatorAgent.
     */
    protected void setup() {
        RegisterInDF("classifier");

        CoordinatorAID = blockingGetFromDF("coordinator");

        addBehaviour(new OurRequestResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                this::prepareResponse));
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
        PlotClassifier();

        // Perform evaluation
        Eval = new Evaluation(validationDataset);
        double[] predictions = Eval.evaluateModel(Tree, validationDataset);

        showMessage("Training completed");
        return computeAccuracy(predictions, validationDataset);
    }

    /**
     * This method create a weka J48 classifier and train it with the training data.
     * @param trainingData The training data.
     */
    protected void createClassifier(Instances trainingData) throws Exception{

        try {
            // Create the classifier
            Tree = new weka.classifiers.trees.J48();
            // Train the classifier
            Tree.buildClassifier(trainingData);
        } catch (Exception e) {
            throw new Exception("Problem creating classifier: " + e.getMessage());
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


    protected void PlotClassifier() throws Exception{
        final javax.swing.JFrame jf =
                new javax.swing.JFrame(getLocalName()+" tree");
        jf.setSize(500,400);
        jf.getContentPane().setLayout(new BorderLayout());
        TreeVisualizer tv = new TreeVisualizer(null,
                Tree.graph(),
                new PlaceNode2());
        jf.getContentPane().add(tv, BorderLayout.CENTER);
        jf.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                jf.dispose();
            }
        });
        jf.setVisible(true);
        tv.fitToScreen();
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
        for (int i=0; i<numInstances; i++)
            predictions[i] = classifyInstance(instances.get(i).get(0));

        showMessage("Testing completed");
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
            val = Tree.classifyInstance(instance);
        }catch (Exception e){
            showErrorMessage("while classifying instance: "+instance+" "+e.getMessage());
        }

        return val;
    }

}


