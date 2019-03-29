import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Melkor extends Agent {
    public static AtomicLong iterationNumber = new AtomicLong(1);
    private static ArrayList<Long> driverWaitingTimeArray = new ArrayList<>();

    public static final int passengersCount = 100;
    private static int driversCount = 4;

    private static AgentContainer mainContainer;

    public static int xCity = 7900;
    public static int yCity = 8100;

    public static void setMainContainer(AgentContainer mainContainer) {
        Melkor.mainContainer = mainContainer;
    }

    public static synchronized void incrementDriversCount(){
        Agent aeroDriverAgent = new DriverAgent();
        AgentController aeroDriverAgentController = null;
        try {
            aeroDriverAgentController = mainContainer.acceptNewAgent("aeroDriverAgent" + driversCount, aeroDriverAgent);
            driversCount++;
            System.out.println("Add driver");
            aeroDriverAgentController.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getDriversCount() {
        return driversCount;
    }

    public static void sleep(long timeInMillis){
        try {
            Thread.sleep(timeInMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void addDriverWaitingTime(Long driverWaitingTime){
        driverWaitingTimeArray.add(driverWaitingTime);
    }

    public static double getAverageTaxiWaitingTime(){
        iterationNumber.incrementAndGet();
        long waitingTime = 0;
        if(!driverWaitingTimeArray.isEmpty()){
            for (Long time : driverWaitingTimeArray) {
                waitingTime += time;
            }
        }
        return driverWaitingTimeArray.size() != 0 ? waitingTime / driverWaitingTimeArray.size() : 0;
    }
}

