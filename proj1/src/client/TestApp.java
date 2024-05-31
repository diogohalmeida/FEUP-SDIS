package client;

import communication.RemoteObject;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

//Using java 15.0.1
public class TestApp {
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 4) {
            System.out.println("Error! Correct Usage: java TestApp <remote_object_name> <sub_protocol> <opnd_1>* <opnd_2>*\n");
            System.out.println("BACKUP: java TestApp <remote_object_name> BACKUP <file_path> <replication_degree>");
            System.out.println("RESTORE: java TestApp <remote_object_name> RESTORE <file_path>");
            System.out.println("DELETE: java TestApp <remote_object_name> DELETE <file_path>");
            System.out.println("RECLAIM: java TestApp <remote_object_name> RECLAIM <disk_space>");
            System.out.println("STATE: java TestApp <remote_object_name> STATE");
            return;
        }

        String remoteObjName = args[0];
        String subProtocol = args[1];
        String filePath;
        int repDegree;
        long diskSpace;

        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            RemoteObject stub = (RemoteObject) registry.lookup(remoteObjName);

            switch(subProtocol){
                case "BACKUP":
                    filePath = args[2];
                    repDegree = Integer.parseInt(args[3]);
                    stub.backup(filePath, repDegree);
                    break;

                case "RESTORE":
                    filePath = args[2];
                    stub.restore(filePath);
                    break;

                case "DELETE":
                    filePath = args[2];
                    stub.delete(filePath);
                    break;

                case "RECLAIM":
                    diskSpace = Long.parseLong(args[2]);
                    stub.reclaim(diskSpace);
                    break;

                case "STATE":
                    stub.state();
                    break;

                default:
                    System.out.println("Error! Given Sub Protocol is invalid!");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}