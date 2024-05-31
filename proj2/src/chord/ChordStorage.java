package chord;

import chord.FileInfo;
import messages.ReclaimMessage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChordStorage implements Serializable {


    //private ConcurrentHashMap<Integer, FileInfo> distributedHashTable;
    //private List<Chunk> chunksGettingBackup = Collections.synchronizedList(new ArrayList<Chunk>());
    private List<FileInfo> filesBackedUp = Collections.synchronizedList(new ArrayList<>());
    private List<FileInfo> ownedFiles = Collections.synchronizedList(new ArrayList<>());
    private ConcurrentHashMap<Integer, ArrayList<ChordInformation>> distributedHashTable = new ConcurrentHashMap<>();
    private long spaceAvailable;

    public ChordStorage() {
        spaceAvailable = 64_000_000_000L;
    }

    public long getSpaceAvailable() {
        return spaceAvailable;
    }


    public void addOwnedFile(FileInfo file) {
        ownedFiles.add(file);
    }

    public FileInfo getOwnedFileByKey(int fileKey) {
        for (var file: ownedFiles) {
            if (file.getFileKey() == fileKey) return file;
        }
        return null;
    }

    public ArrayList<ChordInformation> updateDHT(ArrayList<ChordInformation> previousNodes, ArrayList<ChordInformation> currentNodes) {
        ArrayList<ChordInformation> result = new ArrayList<>();
        for (var previous: previousNodes) {
            if (previousNodes.contains(previous)) {
                result.add(previous);
            }
        }

        for (var current: currentNodes) {
            if (currentNodes.contains(current)) {
                result.add(current);
            }
        }
        return result;
    }

    public void removeNodeFromDHT(int nodeGuid, int key) {
        if (distributedHashTable.get(key) == null) {
            System.out.println("Invalid key...");
            return;
        }
        ArrayList<ChordInformation> nodes = distributedHashTable.get(key);
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getGuid() == nodeGuid) {
                nodes.remove(i);
                break;
            }
        }
        distributedHashTable.replace(key, nodes);
    }

    public void addNodesList(ArrayList<ChordInformation> nodes, int key) {

        if (distributedHashTable.get(key) == null){
            distributedHashTable.put(key, nodes);
        }
        else {
            ArrayList<ChordInformation> previousNodes = distributedHashTable.get(key);
            ArrayList<ChordInformation> currentNodes = updateDHT(previousNodes, nodes);

            distributedHashTable.replace(key, currentNodes);
        }
    }

    public ConcurrentHashMap<Integer, ArrayList<ChordInformation>> getDistributedHashTable() {
        return distributedHashTable;
    }


    public FileInfo getFileByKey(int fileKey) {
        for (FileInfo file: filesBackedUp) {
            if (file.getFileKey() == fileKey) {
                return file;
            }
        }

        return null;
    }

    public FileInfo getFileByPath(String path) {
        for (FileInfo file: ownedFiles) {
            if (file.getFilePath().equals(path)) {
                return file;
            }
        }
        return null;
    }

    public void addFileToStorage(FileInfo fileInfo) {
        filesBackedUp.add(fileInfo);
    }

    public byte[] getFileBody(FileInfo file) {
        File srcFile = new File(file.getFilePath());
        if (!srcFile.exists()){
            System.out.println("File not found");
            return null;
        }
        Path path = Paths.get(file.getFilePath());
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteOwnedFile(int fileKey) {
        FileInfo fileInfo = getOwnedFileByKey(fileKey);
        ownedFiles.remove(fileInfo);
    }


    public void deleteFile(int fileKey) {
        //implement delete enhancement ?

        FileInfo fileInfo = getFileByKey(fileKey);
        if (fileInfo == null) {
            System.out.println("Peer does not exist in distributed hash table");
            return;
        }

        filesBackedUp.remove(fileInfo);
        File file = new File(fileInfo.getFilePath());
        file.delete();

    }

    public long spaceOccupied() {
        long space = 0;

        for (FileInfo fileInfo: filesBackedUp) {
            space += fileInfo.getSize();
        }
        return space;
    }

    public void reclaimSpace(int newSpaceAvailable) {
        long spaceOccupied = spaceOccupied();
        spaceAvailable = 0;

        filesBackedUp.sort(Comparator.comparing(FileInfo::getSize));
        while (spaceOccupied > newSpaceAvailable) {
            FileInfo toRemove =  filesBackedUp.get(filesBackedUp.size()-1);
            ChordNode.nodeReference.getPeerReference().replicateFile(toRemove);
            System.out.println("RECLAIM: Removed file " + toRemove.getFileKey());
            spaceOccupied -= toRemove.getSize();
            deleteFile(toRemove.getFileKey());
        }
        spaceAvailable = newSpaceAvailable - spaceOccupied;
    }


    public void saveBackupFile(FileInfo file) {
        String fileName = "Peers" + File.separator + "Peer" + ChordNode.nodeReference.getGuid() + File.separator + "Backup" + File.separator + "file_" + file.getFileKey() + "_" + file.getFileName();
        File destFile = new File(fileName);

        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();


        try {
            FileOutputStream fos = new FileOutputStream(destFile);
            System.out.println("Writing to file: " + file.getBody().length + " bytes.");
            fos.write(file.getBody());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        file.setFilePath(fileName);
        spaceAvailable -= file.getBody().length;
        file.clearBody();
        addFileToStorage(file);
    }

    public void saveRestoredFile(int fileKey, byte[] body) {
        FileInfo file = getOwnedFileByKey(fileKey);

        String fileName = "Peers" + File.separator + "Peer" + ChordNode.nodeReference.getGuid() + File.separator + "Restore" + File.separator + "restored_" + fileKey + "_" + file.getFileName();

        File restoredFile = new File(fileName);

        if (!restoredFile.getParentFile().exists())
            restoredFile.getParentFile().mkdirs();

        try {
            FileOutputStream fos = new FileOutputStream(restoredFile);
            fos.write(body);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void state() {
        StringBuilder stateBuilder = new StringBuilder();

        stateBuilder.append("Owned files:\n");
        stateBuilder.append("----------------------------------\n");
        for (var file: ownedFiles) {
            stateBuilder.append("\tfile path: " + file.getFilePath() + "\n");
            stateBuilder.append("\tfile size: " + file.getSize() + "\n");
            stateBuilder.append("\t file replication degree: " + distributedHashTable.get(file.getFileKey()).size() + "\n");
            stateBuilder.append("\tNodes with copies of this file: \n");
            for (var node: distributedHashTable.get(file.getFileKey())){
                stateBuilder.append(node + "\n");
            }
            stateBuilder.append("----------------------------------\n");
        }

        stateBuilder.append("Backed up files:\n");

        for (var file: filesBackedUp){
            stateBuilder.append("\tfile path: " + file.getFilePath() + "\n");
            stateBuilder.append("\tfile size: " + file.getSize() + "\n");
        }

        stateBuilder.append("\n----------------------------------\n");

        stateBuilder.append("Space available: " + spaceAvailable);

        System.out.println(stateBuilder.toString());
    }
}
