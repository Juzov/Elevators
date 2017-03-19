import sun.awt.image.ImageWatched;

import java.net.Socket;
import java.io.*;
import java.util.IllegalFormatException;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Handler {

    private static PrintWriter wr;
    private static Elevator[] elevators;
    public static bMonitor bqueue;


    public class bMonitor{
        private LinkedList<Operation> bOperations= new LinkedList<>();

        synchronized public void addBOperation(Operation add){
            if(add == null) return;
            if(bOperations.isEmpty())
            {System.out.println("Wassa");bOperations.addLast(add); return;}
            for (Operation temp : bOperations)
                if(temp.isEqual(add)) {System.out.println("Nassa"); return; }
            bOperations.addLast(add);

        }
        //check if elevator is in the same floor & that its the right direction
        synchronized public Operation checkFloor(int floor) {
            if(bOperations.isEmpty())
                return null;

            for (Operation temp : bOperations)
                if(Math.round(temp.location) == floor) {
                    bOperations.remove(temp);
                    return temp;
                }
            return null;
        }

        //Gets called upon free elevator
        synchronized public Operation checkNearest(float location ){
            float minDiff = 100;
            Operation nearest = null;

            if(bOperations.isEmpty())
                return null;

            for (Operation temp : bOperations){
                float diff = Math.abs(location - temp.location);
                if(diff < minDiff) {
                    nearest = temp;
                    minDiff = location;
                }
            }
            bOperations.remove(nearest);
            return nearest;
        }
    }


    synchronized public static void sendCommand(String command)
    {
        wr.println(command);
        wr.flush();
    }

    public static void commandHandler(String command) throws InterruptedException {
        String[] sCommand = command.split(" ");
        if (sCommand.length < 5 && sCommand.length > 1) {
            String operation = sCommand[0];
            switch (operation) {
                case "b":
                    bCommand(sCommand);
                    break;
                case "p":
                    pCommand(sCommand);
                    break;
                case "f":
                    fCommand(sCommand);
                    break;
                default:
                    System.out.println("illegal input");
                    break;
            }
        }
    }


    //put floor requests on a queue.
    //elevator checks when on the floor if someone is waiting to go on the same direction here
    //if so put him into the elevator operation queue.

    //b location direction
    static void bCommand(String[] sCommand) throws InterruptedException {
        for (String s:sCommand) {
            System.out.println(s);
        }

        float floor = Float.parseFloat(sCommand[1]);
        int direction = Integer.parseInt(sCommand[2]);
        System.out.println(" ");
        System.out.println(floor + " " + direction);


        Operation temp = new Operation(direction,floor);

 /*       synchronized (Handler.class){
            bqueue.addBOperation(temp);
        }*/

    }

    //set location
    static void fCommand(String[] sCommand){
        int elev = Integer.parseInt(sCommand[1]);
        float location = Float.parseFloat(sCommand[2]);
        elevators[elev].setLocation(location);
    }

    static void pCommand(String[] sCommand){
        int elev = Integer.parseInt(sCommand[1]);
        int instruction = Integer.parseInt(sCommand[2]);

        if(instruction == 32000)
            elevators[elev].addStop(new Operation(0,(float) instruction));
        else{
            //Are we going upward or downward from current pos?
            int direction = elevators[elev].compLocation((float)instruction);
            Operation temp = new Operation(direction,instruction);

            //Where should this command be positioned in the queue?
            elevators[elev].getSpot(temp);
        }
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("usage: java Handler <host> <port> <elevator count> <floor count>");
            System.exit(1);
        }

        String host = args[0];
        int port = -1, nElev = -1, nFloors = -1;

        try {
            port = Integer.parseInt(args[1]);
            nElev = Integer.parseInt(args[2]);
            nFloors = Integer.parseInt(args[3]);

            if (port < 1 || nElev < 1 || nFloors < 1) {
                throw new Exception();
            }
        } catch (Exception exception) {
            System.out.println("error: invalid <port>, <elevator count> or <floor count>");
            System.exit(1);
        }

        Socket socket = new Socket(host, port);

        socket.setSoLinger(false, 0);

        elevators = new Elevator[nElev + 1];

        //not used
        elevators[0] = new Elevator(0);
        //elevators to be started as threads
        for(int i = 1; i < nElev + 1; i++) {
            elevators[i] = new Elevator(i);
            elevators[i].start();
        }

        wr = new PrintWriter(socket.getOutputStream());

        BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String commandWithArguments;

        while ((commandWithArguments = rd.readLine()) != null) {
            if(!commandWithArguments.equals(""))
                commandHandler(commandWithArguments);
        }

        socket.close();
    }
}