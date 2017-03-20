import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Emil on 07/03-17.
 */
public class Elevator extends Thread{
    private int id;
    private float location = 0;
    private int direction = 0;
    LinkedList<Operation> queue = new LinkedList<>();
    public Lock locLock = new ReentrantLock();
    public Lock queueLock = new ReentrantLock();

    public Elevator(int id){
        this.id = id;
    }

    @Override
    public void run() {
        //fix internal struggles

        while (true){
            try{

                if (!queue.isEmpty()) {
                    //queue is ordered by closest operation from current location that has the same direction
                    Operation temp = queue.getFirst();
                    if(temp == null)    continue;
                    if ((int) temp.location == 32000) { sendMove(0); continue;}

                    //set direction
                    this.direction = temp.direction;
                    //move in right direction
                    sendMove(this.direction);

                    //start checking stuff at every new location
                    while (true){
                            queueLock.lock();
                            Operation current;
                            current = queue.getFirst();

                            if ((int) current.location == 32000) {

                                sendMove(0);
                                queueLock.unlock();
                                break;
                            }
                            queueLock.unlock();
                            float at;

                            locLock.lock();
                            at = this.location;
                            locLock.unlock();
                            float onFloor = Math.abs(at - Math.round(at));
                            if (onFloor < 0.04) {
                                //System.out.println(onFloor);
                                boolean toOpen = false;

                                toOpen = Handler.checkFloor(Math.round(at),this.direction);
                                if (Math.round(current.location) == Math.round(at)){
                                    toOpen = true;
                                }

                                if (toOpen) {
                                    sendMove(0);
                                    this.direction = 0;
                                    setDoor(1);
                                    System.out.println(id + ": Doors Open");
                                    Thread.sleep(8000);
                                    System.out.println(id + ": Doors Closed");
                                    setDoor(-1);
                                    if (Math.round(current.location) == Math.round(at)) {
                                        queueLock.lock();
                                        synchronized (Handler.class) {
                                            queue.remove(current);
                                        }
                                        queueLock.unlock();
                                    }
                                    for (Operation t: queue) {
                                        System.out.println(t.direction);
                                    }
                                    break;
                                }

                            }
                        }

                    }
                else{
                        Operation temp = null;
                        synchronized (Handler.class) {
                            temp = Handler.checkNearest(this.location);
                            if (temp == null)
                                continue;
                            queueLock.lock();
                            queue.add(temp);
                            queueLock.unlock();
                            System.out.println("Hello");
                        }

                }

            }catch(Exception E){
                System.out.println("Sup");
                sendMove(0);
                run();
            }

        }
    }

    synchronized public int compLocation(float location){

        if      (location > this.location)   return  1;
        else if (location < this.location)   return  -1;
        return 0;

    }


    //set if we're on floor check if anyone wants in the same direction.
    public void setLocation(float location){
        synchronized (Handler.class) {
            locLock.lock();
            if (Math.abs(this.location - location) > 0.03)
                this.location = location;
            locLock.unlock();
        }
        //We're on a floor
    }


    synchronized public void addStop(Operation temp){
        synchronized (Handler.class) {
            queue.clear();
            queue.add(temp);
        }
    }

    synchronized public void getSpot(Operation temp){
        synchronized (Handler.class) {
            queueLock.lock();
            if (queue.contains(temp)) {
                queueLock.unlock();
                return;
            }

            float tempDist = Math.abs(temp.location - this.location);
            if (queue.isEmpty()) {
                queue.add(temp);
                queueLock.unlock();
                return;
            }

            int i;
            if (temp.direction == this.direction) {
                for (i = 0; i < queue.size(); i++) {
                    Operation iteration = queue.get(i);
                    if (iteration.direction != this.direction) {
                        //Guy with the shortest distance to planned location should be before the other
                        float itDist = Math.abs(iteration.location - queue.getFirst().location);
                        float nTempDist = Math.abs(temp.location - queue.getFirst().location);
                        if (itDist > nTempDist) {
                            queue.add(i, temp);
                            queueLock.unlock();
                            return;
                        }
                    } else {
                        float itDist = Math.abs(iteration.location - this.location);
                        tempDist = Math.abs(temp.location - this.location);
                        if (tempDist < itDist) {
                            queue.add(i, temp);
                            queueLock.unlock();
                            return;
                        }
                    }
                }
                queue.addLast(temp);
            } else {
                for (i = 0; i < queue.size(); i++) {
                    Operation iteration = queue.get(i);
                    if (iteration.direction != this.direction) {
                        float itDist = Math.abs(iteration.location - this.location);
                        if (tempDist < itDist) {
                            queue.add(i, temp);
                            queueLock.unlock();
                            return;
                        }
                    }
                }
                queue.addLast(temp);
            }
            queueLock.unlock();
        }
    }



    synchronized private void sendMove(int direction){
        String m = ("m " + id + " " + direction);
        this.direction = direction;
        Handler.sendCommand(m);
    }



    //1 open - -1 close
    synchronized void setDoor(int c){
        String d = ("d " + this.id + " " + c);
        Handler.sendCommand(d);
    }
}
