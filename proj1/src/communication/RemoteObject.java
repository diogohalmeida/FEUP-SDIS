package communication;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteObject extends Remote {
    //backup - backup sub protocol ran by the initiator peer through RMI
    void backup(String filePath, int repDegree) throws RemoteException;

    //restore - restore sub protocol ran by the initiator peer through RMI
    void restore(String filePath) throws RemoteException;

    //delete - delete sub protocol ran by the initiator peer through RMI
    void delete(String filePath) throws RemoteException;

    //reclaim - reclaim sub protocol ran by the initiator peer through RMI
    void reclaim(long diskSpace) throws RemoteException;

    //state - state sub protocol ran by the initiator peer through RMI
    void state() throws RemoteException;
}
