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

/**
 * This agent implements a simple Ping Agent that registers itself with the DF and
 * then waits for ACLMessages.
 * If  a REQUEST message is received containing the string "ping" within the content
 * then it replies with an INFORM message whose content will be the string "pong".
 *
 * @author Tiziana Trucco - CSELT S.p.A.
 * @version  $Date: 2010-04-08 13:08:55 +0200 (gio, 08 apr 2010) $ $Revision: 6297 $
 */
public class UserAgent extends Agent
{
    private int nResponders;
    private String CoordName = "coordinator";
    private AID CoordinatorAID;

    public void showMessage(String mss) {
        System.out.println(getLocalName()+" -> "+mss);
    }

    //Nou comportament que estén la part iniciadora d’un Query
    class QueryInitiator extends AchieveREInitiator
    {
        public QueryInitiator (Agent myAgent, ACLMessage msg)
        {
            super(myAgent, msg);
        }
        //Mètodes que caldrà implementar amb les accions pertinents
        protected void handleInform (ACLMessage msg)
        {
            showMessage("'" + msg.getSender().getLocalName()+"' sent: \""+msg.getContent()+"\"");
        }

    }

    protected void setup() {
        // Read name of coordinator as argument (NOT ACTIVE NOW)
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
        msg.setContent("I am USER");

        QueryInitiator bh1 = new QueryInitiator(this, msg);
        addBehaviour(bh1);
        readDataset();
    }

    protected void readDataset()
    {
        try {
            showMessage("Reading dataset...");
            DataSource source = new DataSource("./src/main/resources/audit_risk.arff");
            Instances data = source.getDataSet();
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }
            showMessage(data.toSummaryString());
        }catch(Exception e){showMessage(e.getMessage());}
    }
}


