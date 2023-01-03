import aima.core.environment.wumpusworld.AgentPercept;
import aima.core.environment.wumpusworld.AgentPosition;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class EnvironmentAgent extends Agent {

    private AID spelAgent;

    public final int STENCH = 1;
    public final int BREEEZE = 2;
    public final int GLITTER = 4;
    public final int WUMPUS = 8;
    public final int PIT = 16;

    private int record = 0;
    private int[][] rooms = new int[4][4];
    private AgentPosition ap = new AgentPosition(1,1, AgentPosition.Orientation.FACING_EAST);
    private AgentPercept nextPercept;
    private int caveXY = 2;
    private boolean agentHasArrow = true;
    private boolean agentHasGold = false;
    private boolean agentAlive = true;
    private boolean inGame = true;
    private int wapmpI;
    private int wampJ;

    public EnvironmentAgent()
    {
        this(2,0,2,1,new int[]{0,1,2},new int[]{2,3,2});
    }

    public EnvironmentAgent(int wapmpI, int wampJ, int goldI, int goldJ, int[] pitX, int[] pitY)
    {
        this.wapmpI = wapmpI;
        this.wampJ = wampJ;
        setWumpus(wapmpI, wampJ);
        setGold(goldI, goldJ);
        for (int i= 0; i < pitX.length; i++)
            setPit(pitX[i],pitY[i]);
        nextPercept = new AgentPercept(checkStench(1,1),checkBreeze(1,1),checkGlitter(1,1),false,false);
    }

    private void setPit(int i, int j) {
        rooms[i][j] += PIT;
        rooms[i+1][j] += BREEEZE;
        rooms[i][j-1] += BREEEZE;
    }

    private void setGold(int i, int j) {
        rooms[i][j] += GLITTER;
    }

    private void setWumpus(int i, int j)
    {
        rooms[i][j] += WUMPUS;
        rooms[i+1][j] += STENCH;
        rooms[i][j+1] += STENCH;
        rooms[i-1][j] += STENCH;
        rooms[i][j] += STENCH;
    }

    public void changeWorld(String action) {
        record -= 1;
        if (!agentAlive || !inGame)
            return;
        nextPercept = new AgentPercept(false, false, false, false, false);
        AgentPosition.Orientation n = ap.getOrientation();
        switch (action) {
            case "TurnLeft":
                if (n == AgentPosition.Orientation.FACING_EAST)
                    n = AgentPosition.Orientation.FACING_NORTH;
                else if (n == AgentPosition.Orientation.FACING_NORTH)
                    n = AgentPosition.Orientation.FACING_WEST;
                else if (n == AgentPosition.Orientation.FACING_WEST)
                    n = AgentPosition.Orientation.FACING_SOUTH;
                else if (n == AgentPosition.Orientation.FACING_SOUTH)
                    n = AgentPosition.Orientation.FACING_EAST;
                ap = new AgentPosition(ap.getRoom(), n);
                break;
            case "TurnRight":
                if (n == AgentPosition.Orientation.FACING_NORTH)
                    n = AgentPosition.Orientation.FACING_EAST;
                else if (n == AgentPosition.Orientation.FACING_WEST)
                    n = AgentPosition.Orientation.FACING_NORTH;
                else if (n == AgentPosition.Orientation.FACING_SOUTH)
                    n = AgentPosition.Orientation.FACING_WEST;
                else if (n == AgentPosition.Orientation.FACING_EAST)
                    n = AgentPosition.Orientation.FACING_SOUTH;
                ap = new AgentPosition(ap.getRoom(), n);
                break;
            case "Forward":
                if (n == AgentPosition.Orientation.FACING_EAST) {
                    if (ap.getX() < caveXY)
                        ap = new AgentPosition(ap.getX() + 1, ap.getY(), ap.getOrientation());
                    else
                        nextPercept.setBump(true);
                } else if (n == AgentPosition.Orientation.FACING_NORTH) {
                    if (ap.getY() < caveXY)
                        ap = new AgentPosition(ap.getX(), ap.getY() + 1, ap.getOrientation());
                    else
                        nextPercept.setBump(true);
                } else if (n == AgentPosition.Orientation.FACING_WEST) {
                    if (ap.getX() > 1)
                        ap = new AgentPosition(ap.getX() - 1, ap.getY(), ap.getOrientation());
                    else
                        nextPercept.setBump(true);
                } else if (n == AgentPosition.Orientation.FACING_SOUTH) {
                    if (ap.getX() > 1)
                        ap = new AgentPosition(ap.getX(), ap.getY() - 1, ap.getOrientation());
                    else
                        nextPercept.setBump(true);
                }
                if (checkPit(ap.getX(), ap.getY()) || checkWumpus(ap.getX(), ap.getY())) {
                    agentAlive = false;
                    inGame = false;
                    record -= 1000;
                }
                break;
            case "Shoot":
                if (!agentHasArrow)
                    throw new IllegalStateException("Agent don't have an arrow!");
                else {
                    record -= 9;
                    agentHasArrow = false;
                    boolean killed = false;
                    if (n == AgentPosition.Orientation.FACING_NORTH) {
                        if (wampJ > ap.getY() && wapmpI == ap.getX())
                            killed = true;
                    } else if (n == AgentPosition.Orientation.FACING_WEST) {
                        if (wampJ == ap.getY() && wapmpI < ap.getX())
                            killed = true;
                    } else if (n == AgentPosition.Orientation.FACING_SOUTH) {
                        if (wampJ < ap.getY() && wapmpI == ap.getX())
                            killed = true;
                    } else if (n == AgentPosition.Orientation.FACING_EAST) {
                        if (wampJ == ap.getY() && wapmpI > ap.getX())
                            killed = true;
                    }
                    if (killed) {
                        nextPercept.setScream(true);
                    }
                }
                break;
            case "Climb":
                inGame = false;
                if (agentHasGold)
                    record += 1000;
                break;
            case "Grab":
                if (checkGlitter(ap.getX(), ap.getY())) {
                    rooms[ap.getX()][ap.getY()] -= GLITTER;
                    agentHasGold = true;
                } else {
                    throw new IllegalStateException("At this room there isn't any gold!");
                }
                break;
        }
        nextPercept.setBreeze(checkBreeze(ap.getX(), ap.getY()));
        nextPercept.setStench(checkStench(ap.getX(), ap.getY()));
        nextPercept.setGlitter(checkGlitter(ap.getX(), ap.getY()));
    }

    private boolean checkBreeze(int x, int y) {
        return (rooms[x][y]/BREEEZE) % 2 == 1;
    }
    private boolean checkStench(int x, int y) {
        return (rooms[x][y]/STENCH) % 2 == 1;
    }
    private boolean checkGlitter(int x, int y) {
        return (rooms[x][y]/GLITTER) % 2 == 1;
    }
    private boolean checkPit(int x, int y) {
        return (rooms[x][y]/PIT) % 2 == 1;
    }
    private boolean checkWumpus(int x, int y) {
        return (rooms[x][y]/WUMPUS) % 2 == 1;
    }

    public AgentPercept getPercept() {
        return nextPercept;
    }

    public int getRecord() {
        return record;
    }


    @Override
    protected void setup() {
        System.out.println("Hello! Environment agent " + getAID().getName() + " is ready!");
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("wumpus-cave");
        sd.setName("Cave-wandering");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new RequestBehavior());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Environment agent"+ getAID().getName()+ " is terminating.");
    }
    private class RequestBehavior extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if (spelAgent == null)
                    spelAgent = msg.getSender();
                if (spelAgent.equals(msg.getSender())) {
                    if (msg.getPerformative() == ACLMessage.REQUEST)
                        myAgent.addBehaviour(new PerceptReplyBehaviour(msg));
                    if (msg.getPerformative() == ACLMessage.CFP)
                        myAgent.addBehaviour(new WorldChangingBehaviour(msg));
                }
                else {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                    myAgent.send(reply);
                }
            }
            else
            {
                block();
            }
        }
    }

    private class PerceptReplyBehaviour extends OneShotBehaviour {

        ACLMessage msg;

        public PerceptReplyBehaviour(ACLMessage m)
        {
            super();
            msg = m;
        }

        public void action() {
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(GeneratePerceptSequence());
            myAgent.send(reply);
            System.out.println(getAID().getName() + ": " + reply.getContent());
        }

        private String GeneratePerceptSequence() {
            StringBuilder reply = new StringBuilder();
            reply.append("[");
            AgentPercept ap = getPercept();
            //AgentPercept ap = wwe.getPercept();
            if (ap.isStench())
                reply.append("Stench, ");
            else
                reply.append("None, ");
            if (ap.isBreeze())
                reply.append("Breeze, ");
            else
                reply.append("None, ");
            if (ap.isGlitter())
                reply.append("Glitter, ");
            else
                reply.append("None, ");
            if (ap.isBump())
                reply.append("Bump, ");
            else
                reply.append("None, ");
            if (ap.isScream())
                reply.append("Scream]");
            else
                reply.append("None]");
            return reply.toString();
        }
    }

    private class WorldChangingBehaviour extends OneShotBehaviour {

        ACLMessage msg;

        public WorldChangingBehaviour(ACLMessage m)
        {
            super();
            msg = m;
        }

        public void action() {
            String content = msg.getContent().toLowerCase();
            if (content.contains("forward"))
                changeWorld("Forward");
            else if (content.contains("shoot"))
                changeWorld("Shoot");
            else if (content.contains("climb"))
                changeWorld("Climb");
            else if (content.contains("grab"))
                changeWorld("Grab");
            else if (content.contains("right"))
                changeWorld("TurnRight");
            else if (content.contains("left"))
                changeWorld("TurnLeft");
        }
    }
}