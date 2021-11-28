package urv.imas.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import weka.classifiers.*;
import weka.core.*;



   /** Inner class QueryInitiator.
     * This class implements the behaviour used by the ClassifierAgent to send a
     * REQUEST message to the CoordinatorAgent.
     * @author Anna Garriga Ramon Mateo
     * @version  $Date: 2010-04-08 13:08:55 +0200 (gio, 08 apr 2010) $ $Revision: 6297 $
     * */
public class ClassifierAgent extends Agent
{
    private String CoordName = "coordinator";
    private AID CoordinatorAID;
    private String trainingData;
    Classifier classifier = null;
    Evaluation eval = null;


    public void showMessage(String mss) {
        System.out.println(getLocalName()+" -> "+mss);
    }


    
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
            // 
            switch (msg.getContent()) {
                
                case "train":
                    ACLMessage trainingData = blockingReceive();
                    this.classifier = createClassifier(trainingData.getContent());
                    // retrun accuracy of classifier
                    this.eval = new Evaluation(trainingData.getContent());
                    // evaluem el model
                    double prediction = this.eval.evaluateModel(this.classifier, trainingData.getContent());
                    // send prediction to coordinator agent
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(Double.toString(prediction));
                    send(reply);

                    break;
            
                case "classify":
                    ACLMessage classificationData = blockingReceive();
                    double result = classifyInstance(classificationData.getContent()); // segur que peta aqui
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(Double.toString(result));
                    send(reply);
                    break;
                
                case "getAtributes":
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(get_infoClassifier());
                    send(reply);
                    break;

                case "globalInfo":
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(this.classifier.get_globalInfo());
                    send(reply);
                    break;

                case "getGraph":
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(this.classifier.graph());
                    send(reply);
                    break;

                case default:
                    break;
            }
    
        }

    }
    /**
     * This method is automatically called when the agent is launched.
     * It initiates the conversation with the CoordinatorAgent.
     */
    protected void setup() {
        // Read name of classifiers as arguments (NOT ACTIVE NOW)
        Object[] args = getArguments();
        if (args != null && args.length > 0) {}else{}

        CoordinatorAID = new AID((String) CoordName, AID.ISLOCALNAME);

        //Instanciació del missatge
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        //Afegim un receptor (suposem que el seu AID ja l’havíem trobat abans
        msg.addReceiver(CoordinatorAID);
        //Ens posem a nosaltres com a emissor
        msg.setSender(getAID());
        //Indiquem el contingut (String sense estructura)
        msg.setContent("I am CLASSIFIER");

        QueryInitiator bh1 = new QueryInitiator(this, msg);
        addBehaviour(bh1);

    }
    
    /**
     * This method create a weka J48 classifier and train it with the training data.
     * @param trainingData The training data.
     * @return The J48 classifier.
     */
    
    public weka.classifiers.Classifier createClassifier(String trainingData) {
        Classifier classifier = null;
        try {
            // Create the classifier
            classifier = new classifiers.trees.J48();
            // Train the classifier
            classifier.buildClassifier(new core.Instances(trainingData));
        } catch (Exception e) {
            System.err.println("Error creating classifier: " + e.getMessage());
        }
        return classifier;
    }

    /**
     * Classify the given instance.
     * @param instance intance to classify and predict
     * @return The predicted class.
     */

    public classifyInstance(Instances instance) {
        return this.classifier.classifyInstance(instance);
    }

    /**
     * Get info about classifier
     * return String with info about classifier
     */
    public String get_infoClassifier() {
        return classifier.get_info();
    }
        
}


