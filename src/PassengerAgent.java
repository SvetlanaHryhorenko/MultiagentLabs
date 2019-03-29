import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PassengerAgent extends Agent {
    private boolean isPassengerReadyForVisit;
    private int xPosition;
    private int yPosition;
    private int money = 500;

    @Override
    protected void setup(){
        isPassengerReadyForVisit = false;

        Random rand = new Random();
        xPosition = rand.nextInt(Melkor.xCity);
        yPosition = rand.nextInt(Melkor.yCity);

        DFServiceHelper.registerAgentInYellowPages(this, "Passenger", "Visit");

        addBehaviour(new processInvitations());

        visitAnotherPassengerBehaviour(1000);
        addBehaviour(new processInvitations());
    }

    private void visitAnotherPassengerBehaviour(long period){
        addBehaviour(new TickerBehaviour(this, period) {
            @Override
            protected void onTick() {

                ArrayList<AID> passengersAIDList = new ArrayList<>();
                DFAgentDescription[] passengers = DFServiceHelper.findAgents(myAgent, null, "Visit");
                if(passengers != null){
                    for(int i = 0; i < passengers.length; i++){

                        if(!myAgent.getAID().equals(passengers[i].getName())){
                            passengersAIDList.add(passengers[i].getName());
                        }
                    }
                    myAgent.addBehaviour(new RequestPerformer((PassengerAgent) myAgent, passengersAIDList));
                }
            }
        });
    }

    private class processInvitations extends CyclicBehaviour {
        private Integer step = 0;
        private MessageTemplate mt;

        @Override
        public void action() {

            money+=10;

            switch (step) {
                case 0:

                    mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);

                    ACLMessage invitationMessage = myAgent.receive(mt);
                    if(invitationMessage != null){
                        ACLMessage reply = invitationMessage.createReply();
                        if (!isPassengerReadyForVisit) {
                            reply.setPerformative(ACLMessage.AGREE);
                            isPassengerReadyForVisit = true;
                            step = 1;
                            myAgent.send(reply);
                        } else {
                            reply.setPerformative(ACLMessage.CANCEL);
                            step = 0;
                            myAgent.send(reply);
                        }
                    } else {
                        block();
                    }
                    break;
                case 1:
                    mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                            MessageTemplate.MatchPerformative(ACLMessage.CANCEL));


                    ACLMessage confirmationMessage = myAgent.receive(mt);
                    if(confirmationMessage != null){
                        if(confirmationMessage.getPerformative() == ACLMessage.CONFIRM){
                            step = 2;
                        } else {
                            isPassengerReadyForVisit = false;
                            step = 0;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:

                    mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

                    ACLMessage visitTimeInformationMessage = myAgent.receive(mt);
                    if(visitTimeInformationMessage != null){
                        try {
                            Long visitDurationTime = new Long(1000);
                            System.out.println("Average waiting time in system: " + Melkor.getAverageTaxiWaitingTime());
                            Melkor.sleep(visitDurationTime);
                        } catch (Exception e) {

                        }
                        isPassengerReadyForVisit = false;
                        step = 0;
                    } else {
                        block();
                    }
                    break;
            }
        }
    }


    private class RequestPerformer extends Behaviour {
        private ArrayList<AID> passengers;
        private PassengerAgent sender;
        private List<AID> passengersAcceptedInvitation = new ArrayList<>();

        private int repliesCount = 0;
        private MessageTemplate mt;
        private int step = 0;

        public RequestPerformer(PassengerAgent sender, ArrayList<AID> passengers){
            this.passengers = passengers;
            this.sender = sender;
        }

        public void action() {
            switch (step) {
                case 0:

                    ACLMessage invitationMessage = new ACLMessage(ACLMessage.PROPOSE);

                    for (AID passenger : passengers) {
                        invitationMessage.addReceiver(passenger);
                    }
                    invitationMessage.setConversationId("visit-offer");
                    invitationMessage.setReplyWith("propose" + System.currentTimeMillis());
                    sender.send(invitationMessage);


                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("visit-offer"),
                            MessageTemplate.MatchInReplyTo(invitationMessage.getReplyWith()));
                    step = 1;
                    repliesCount = 0;
                    break;
                case 1:

                    ACLMessage reply = sender.receive(mt);
                    if(reply != null){
                        repliesCount++;
                        if(reply.getPerformative() == ACLMessage.AGREE){
                            passengersAcceptedInvitation.add(reply.getSender());
                        }
                        if(passengers.size() == repliesCount){

                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    if(!passengersAcceptedInvitation.isEmpty()){
                        sender.isPassengerReadyForVisit = true;

                        ACLMessage acceptanceMessage = new ACLMessage(ACLMessage.CONFIRM);
                        acceptanceMessage.addReceiver(passengersAcceptedInvitation.get(0));
                        acceptanceMessage.setConversationId("visit-offer");
                        acceptanceMessage.setReplyWith("confirm" + System.currentTimeMillis());
                        sender.send(acceptanceMessage);

                        ACLMessage cancellationMessage =  new ACLMessage(ACLMessage.CANCEL);
                        for(int i = 1; i < passengersAcceptedInvitation.size(); i ++){

                            cancellationMessage.addReceiver(passengersAcceptedInvitation.get(i));
                        }
                        cancellationMessage.setConversationId("visit-offer");
                        cancellationMessage.setReplyWith("cancel" + System.currentTimeMillis());
                        sender.send(cancellationMessage);

                        step = 3;
                    } else {
                        step = 0;
                        sender.isPassengerReadyForVisit = false;
                    }
                    break;
                case 3:

                    long findDriverForVisit = findDriver();
                    if (findDriverForVisit > 0) {
                        Melkor.sleep(findDriverForVisit);
                        step = 4;
                    } else {

                        sender.isPassengerReadyForVisit = false;
                        step = 6;
                    }
                    break;
                case 4:

                    long visitDuration = 3000 + (long) (Math.random()*(3*6000 - 3000));
                    ACLMessage visitDurationInfoMessage = new ACLMessage(ACLMessage.INFORM);
                    visitDurationInfoMessage.addReceiver(passengersAcceptedInvitation.get(0));
                    visitDurationInfoMessage.setConversationId("visit-offer");
                    try {
                        visitDurationInfoMessage.setContentObject(visitDuration);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    visitDurationInfoMessage.setReplyWith("info" + System.currentTimeMillis());
                    sender.send(visitDurationInfoMessage);

                    step = 5;
                    break;
                case 5:

                    long findDriverToGoHome = findDriver();
                    if (findDriverToGoHome > 0) {
                        Melkor.sleep(findDriverToGoHome);
                        sender.isPassengerReadyForVisit = false;
                        step = 6;
                    }
                    break;
            }
        }

        private long findDriver(){
            long startDriverWaitingTime = System.currentTimeMillis();
            boolean isDriverFound = false;
            int findDriverStepNumber = 0;
            AID foundDriver;
            DFAgentDescription[] aeroDrivers = DFServiceHelper.findAgents(myAgent, null, "LithuanianCar");
            try {

            } catch(NullPointerException e){
                e.printStackTrace();
            }
            long distance = (long) (Math.random()*(1000 - 100));
            if(distance>money)
            {
                System.out.println(myAgent.getLocalName() + " has no money");
                return 0;
            }

            while(!isDriverFound) {
                long currentDriverWaitingTime = System.currentTimeMillis();
                double currentTaxiWaitingTime = currentDriverWaitingTime - startDriverWaitingTime;
                System.out.println(myAgent.getLocalName() + " waiting " + currentTaxiWaitingTime);
                if(currentTaxiWaitingTime/60>1)
                {
                    for (DFAgentDescription aeroDriver : aeroDrivers) {
                        ACLMessage cancellationMessage = new ACLMessage(ACLMessage.CANCEL);
                        cancellationMessage.setReplyWith("driverCancel" + System.currentTimeMillis());
                        cancellationMessage.setConversationId("callDriverId");
                        cancellationMessage.addReceiver(aeroDriver.getName());
                        myAgent.send(cancellationMessage);
                    }
                    System.out.println(myAgent.getLocalName() + " is tired ");
                    return 0;
                }
                switch (findDriverStepNumber) {
                    case 0:
                        aeroDrivers = DFServiceHelper.findAgents(myAgent, null, "LithuanianCar");
                        if(aeroDrivers != null){

                            ACLMessage invitationMessage = new ACLMessage(ACLMessage.PROPOSE);
                            invitationMessage.setConversationId("callDriverId");
                            invitationMessage.setReplyWith("aeroDriverPropose" + System.currentTimeMillis());
                            for (DFAgentDescription aeroDriver : aeroDrivers) {
                                invitationMessage.addReceiver(aeroDriver.getName());
                            }
                            myAgent.send(invitationMessage);

                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("callDriverId"),
                                    MessageTemplate.MatchInReplyTo(invitationMessage.getReplyWith()));
                            findDriverStepNumber = 1;
                            break;
                        }
                    case 1:

                        ACLMessage reply = sender.blockingReceive(mt, 3000);
                        foundDriver = reply.getSender();
                        if(reply != null) {
                            if (ACLMessage.AGREE == reply.getPerformative()) {

                                foundDriver = reply.getSender();
                            }
                            else if (ACLMessage.CANCEL == reply.getPerformative()) { }
                        }

                        if(foundDriver != null){

                            assert aeroDrivers != null;
                            for (DFAgentDescription aeroDriver : aeroDrivers) {
                                if (!aeroDriver.getName().equals(foundDriver)) {
                                    ACLMessage cancellationMessage = new ACLMessage(ACLMessage.CANCEL);
                                    cancellationMessage.setReplyWith("driverCancel" + System.currentTimeMillis());
                                    cancellationMessage.setConversationId("callDriverId");
                                    cancellationMessage.addReceiver(aeroDriver.getName());
                                    myAgent.send(cancellationMessage);
                                }
                            }


                            ACLMessage confirmationMessage = new ACLMessage(ACLMessage.CONFIRM);
                            confirmationMessage.setReplyWith("driverConfirm" + System.currentTimeMillis());
                            confirmationMessage.setConversationId("callDriverId");
                            confirmationMessage.addReceiver(foundDriver);

                            isDriverFound = true;
                            long endDriverWaitingTime = System.currentTimeMillis();

                            double taxiWaitingTime = endDriverWaitingTime - startDriverWaitingTime;

                            Melkor.addDriverWaitingTime((long) taxiWaitingTime);

                            try {
                                confirmationMessage.setContentObject((long) (Math.random()*(1000 - 100)));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            myAgent.send(confirmationMessage);

                            ACLMessage cancellationMessage = new ACLMessage(ACLMessage.CANCEL);
                            cancellationMessage.addReceiver(foundDriver);
                            System.out.println(myAgent.getLocalName() + " waited " + taxiWaitingTime + " " + foundDriver.getLocalName());
                            money-=distance;
                            myAgent.send(cancellationMessage);
                            return 0;
                        }

                }

            }

            return 0;
        }

        public boolean done() {
            return step == 6;
        }
    }


    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e){

        }
    }
}
