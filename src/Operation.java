/**
 * Created by Emil on 07/03-17.
 */
public class Operation {
    int direction;
    float location;


    public Operation(int direction, float location){
        this.direction = direction;
        this.location = location;
    }

    //for this tasks sake, they are equal if the direction is zero also
    public boolean isEqual(Operation temp){
        return (temp.direction == direction || temp.direction == -1) && temp.location == location;
    }

}
