package urv.imas.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;

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

    protected void setup() {
        // Read names of responders as arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            System.out.println("Great!");
            System.out.println(args[0]);
        }
        else {
            System.out.println("Something is missing");
        }
    }
}


