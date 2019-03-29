import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.Random;

import static jade.lang.acl.MessageTemplate.MatchPerformative;

public class DriverAgent extends Agent {
    boolean isDriverFree;
    private MessageTemplate mt;

    public int xPosition;
    public int yPosition;

    private Integer driverStepNumber = 0;

    @Override
    protected void setup(){
        DFServiceHelper.registerAgentInYellowPages(this, "Driver", "LithuanianCar");
        isDriverFree = true;

        Random rand = new Random();
        xPosition = rand.nextInt(Melkor.xCity);
        yPosition = rand.nextInt(Melkor.yCity);

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                switch (driverStepNumber){
                    case 0:
                        if(isDriverFree){

                            mt = MatchPerformative(ACLMessage.PROPOSE);
                            ACLMessage invitationMessage = myAgent.receive(mt);
                            if(invitationMessage != null){
                                isDriverFree = false;
                                ACLMessage reply = invitationMessage.createReply();
                                reply.setPerformative(ACLMessage.AGREE);
                                driverStepNumber = 1;
                                myAgent.send(reply);

                            } else {
                                block();
                            }
                        } else {

                            mt = MatchPerformative(ACLMessage.PROPOSE);
                            ACLMessage invitationMessage = myAgent.receive(mt);
                            if(invitationMessage != null){
                                ACLMessage reply = invitationMessage.createReply();
                                reply.setPerformative(ACLMessage.CANCEL);
                                myAgent.send(reply);
                                driverStepNumber = 0;
                            } else {
                                block();
                            }
                        }
                        break;
                    case 1:
                        processConfirmationFromPassenger();
                        break;
                }
            }

            private void processConfirmationFromPassenger(){
                mt = MessageTemplate.or(MatchPerformative(ACLMessage.CONFIRM), MatchPerformative(ACLMessage.CANCEL));
                ACLMessage confirmationMessage = myAgent.receive(mt);
                if(confirmationMessage != null){
                    int performative = confirmationMessage.getPerformative();
                    if(performative == ACLMessage.CANCEL){
                        isDriverFree = true;
                    } else {
                        try {
                            PassengerAgent.PassengerWay way = (PassengerAgent.PassengerWay) confirmationMessage.getContentObject();
                            long travelTime= (long) ((Math.sqrt((way.xPositionFinish-way.xPositionStart)*(way.xPositionFinish-way.xPositionStart)
                                    +(way.yPositionFinish-way.yPositionStart)*(way.yPositionFinish-way.yPositionStart)))
                                    + (Math.sqrt((xPosition-way.xPositionStart)*(xPosition-way.xPositionStart)
                                    +(yPosition-way.yPositionStart)*(yPosition-way.yPositionStart))));

                            System.out.println("Travel time = " + travelTime + " " + myAgent.getLocalName());
                            Melkor.sleep(travelTime);
                            xPosition = way.xPositionFinish;
                            yPosition = way.yPositionFinish;

                            isDriverFree = true;
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                    }
                    driverStepNumber = 0;
                } else {
                    block();
                }
            }
        });
    }


    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e){
            e.printStackTrace();
        }
    }
}
