package main;
import hashdb.HashFile;
import hashdb.HashHeader;
import hashdb.Vehicle;
import misc.ReturnCodes;
import misc.MutableInteger;

import java.io.*;


public class StudentFunctions {
    static MutableInteger k;

    /**
     * hashCreate
     * This funcAon creates a hash file containing only the HashHeader record.
     * • If the file already exists, return RC_FILE_EXISTS
     * • Create the binary file by opening it.
     * • Write the HashHeader record to the file at RBN 0.
     * • close the file.
     * • return RC_OK.
     */
    public static int hashCreate(String fileName, HashHeader hashHeader) throws IOException {
        File temp;
        temp = new File(fileName);
        boolean exists = temp.exists();
        if (exists){
            return ReturnCodes.RC_FILE_EXISTS;
        } else {
            RandomAccessFile hashFile = new RandomAccessFile(fileName, "rw");
            int rba = 0 * hashHeader.getRecSize();
            try {
                hashFile.seek(rba);
                hashFile.write(hashHeader.toByteArray());
                hashFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ReturnCodes.RC_OK;
        }
    }

    /**
     * hashOpen
     * This function opens an existing hash file which must contain a HashHeader record
     * , and sets the file member of hashFile
     * It returns the HashHeader record by setting the HashHeader member in hashFile
     * If it doesn't exist, return RC_FILE_NOT_FOUND.
     * Read the HashHeader record from file and return it through the parameter.
     * If the read fails, return RC_HEADER_NOT_FOUND.
     * return RC_OK
     */
    public static int hashOpen(String fileName, HashFile hashFile) {
        File temp;
        temp = new File(fileName);
        boolean exists = temp.exists();

        if (!exists) {
            return ReturnCodes.RC_FILE_NOT_FOUND;
        } else {
            try {
                RandomAccessFile file = new RandomAccessFile(fileName, "rw");
                file.seek(0);
                byte[] bytes = new byte[Vehicle.sizeOf() * 2];
                file.read(bytes, 0, Vehicle.sizeOf() * 2);
                hashFile.getHashHeader().fromByteArray(bytes);
                hashFile.setFile(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ReturnCodes.RC_OK;
        }
    }

    /**
     * vehicleInsert
     * This function inserts a vehicle into the specified file.
     * Determine the RBN using the Main class' hash function.
     * Use readRec to read the record at that RBN.
     * If that location doesn't exist
     * OR the record at that location has a blank vehicleId (i.e., empty string):
     * THEN Write this new vehicle record at that location using writeRec.
     * If that record exists and that vehicle's szVehicleId matches, return RC_REC_EXISTS.
     * (Do not update it.)
     * Otherwise, return RC_SYNONYM. a SYNONYM is the same thing as a HASH COLLISION
     * Note that in program #2, we will actually insert synonyms.
     */
    public static int vehicleInsert(HashFile hashFile, Vehicle vehicle) {
        MutableInteger rba = null;
        MutableInteger rbn = P2Main.hash(vehicle.getVehicleId(),hashFile.getHashHeader().getMaxHash());
        boolean recExist;
        boolean wroteRec;
        HashHeader hashHeader = hashFile.getHashHeader();
        Vehicle veh = new Vehicle();

        readRec(hashFile, rbn, veh);
        if ((veh == null) || (veh.getVehicleIdAsString().length() == 0)) {
            writeRec(hashFile, rbn.intValue(), vehicle);
        } else if (veh.getVehicleIdAsString().equals(vehicle.getVehicleIdAsString())) {
            return ReturnCodes.RC_REC_EXISTS;
        } else {
            recExist = false;
            for (int i = 1; i < hashFile.getHashHeader().getMaxProbe(); i++) {
                rba.set(rbn.intValue() * hashFile.getHashHeader().getRecSize() + i);
                readRec(hashFile, rba, veh);
                if (veh.getVehicleIdAsString().equals(vehicle.getVehicleIdAsString())) {
                    recExist = true;
                }
            }
            if (recExist) {
                return ReturnCodes.RC_REC_EXISTS;
            } else {
                wroteRec = false;

                for (int i = 1; i < hashFile.getHashHeader().getMaxProbe(); i++) {
                    Vehicle veh2 = new Vehicle();
                    rba.set(P2Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash()).intValue() + i);
                    readRec(hashFile, rba, veh2);
                    if (((veh2 == null) || (veh2.getVehicleIdAsString().length() == 0)) && (!wroteRec)) {
                        writeRec(hashFile, rba.intValue(), vehicle);
                        wroteRec = true;
                    }
                }
                if (wroteRec) {
                    return ReturnCodes.RC_OK;
                } else {
                    return ReturnCodes.RC_TOO_MANY_COLLISIONS;
                }
            }
        }
        return ReturnCodes.RC_OK;
    }

    /**
     * readRec(
     * This function reads a record at the specified RBN in the specified file.
     * Determine the RBA based on RBN and the HashHeader's recSize
     * Use seek to position the file in that location.
     * Read that record and return it through the vehicle parameter.
     * If the location is not found, return RC_LOC_NOT_FOUND.  Otherwise, return RC_OK.
     * Note: if the location is found, that does NOT imply that a vehicle
     * was written to that location.  Why?
     */
    public static int readRec(HashFile hashFile, MutableInteger rbn, Vehicle vehicle) {
        //MutableInteger newRbn = P2Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash());
        int rba = rbn.intValue() * hashFile.getHashHeader().getRecSize();
        try {
            hashFile.getFile().seek(rba);
            byte[] bytes = new byte[Vehicle.sizeOf() * 2];
            hashFile.getFile().read(bytes, 0, Vehicle.sizeOf() * 2);
            if (bytes[1] != 0){
                vehicle.fromByteArray(bytes);
            }
        } catch (IOException | java.nio.BufferUnderflowException e) {
            return ReturnCodes.RC_LOC_NOT_FOUND;
        }
        return ReturnCodes.RC_OK;
    }

    /**
     * writeRec
     * This function writes a record to the specified RBN in the specified file.
     * Determine the RBA based on RBN and the HashHeader's recSize
     * Use seek to position the file in that location.
     * Write that record to the file.
     * If the write fails, return RC_LOC_NOT_WRITTEN.
     * Otherwise, return RC_OK.
     */
    public static int writeRec(HashFile hashFile, int rbn, Vehicle vehicle) {
        int rba = rbn * hashFile.getHashHeader().getRecSize();
        try {
            hashFile.getFile().seek(rba);
            char[] chars = vehicle.toFileChars();
            for (int i = 0; i < chars.length; i++)
                hashFile.getFile().writeChar(chars[i]);
        } catch (IOException e) {
            e.printStackTrace();
            return ReturnCodes.RC_LOC_NOT_FOUND;
        }
        return ReturnCodes.RC_OK;
    }

    /**
     * vehicleRead
     * This function reads the specified vehicle by its vehicleId.
     * Since the vehicleId was provided,
     * determine the RBN using the Main class' hash function.
     * Use readRec to read the record at that RBN.
     * If the vehicle at that location matches the specified vehicleId,
     * return the vehicle via the parameter and return RC_OK.
     * Otherwise, return RC_REC_NOT_FOUND
     */
    public static int vehicleRead(HashFile hashFile, MutableInteger rbn, Vehicle vehicle) {
        rbn.set(P2Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash()).intValue());
        Vehicle veh = new Vehicle();
        readRec(hashFile, rbn, veh);
        if (veh.getVehicleIdAsString().equals(vehicle.getVehicleIdAsString())) {
            vehicle.fromByteArray(veh.toByteArray());
            return ReturnCodes.RC_OK;
        } else {
            for(int i = 1; i < hashFile.getHashHeader().getMaxProbe(); i++ ){
                Vehicle veh2 = new Vehicle();
                rbn.set(P2Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash()).intValue());
                readRec(hashFile, rbn, veh2);

                if (veh2.getVehicleIdAsString().equals(vehicle.getVehicleIdAsString())){
                    vehicle.fromByteArray(veh2.toByteArray());
                    return ReturnCodes.RC_OK;
                }
            }
            return ReturnCodes.RC_REC_NOT_FOUND;
        }
    }

    /**
     * This function tries to find the given vehicle using its …getVehicleId(). If found, it updates the contents of
     * the vehicle in the hash file. If not found, it returns RC_REC_NOT_FOUND. Note that this function must
     * understand probing.
     * NOTE: You can make your life easier with this function if you use MutableInteger and call some of your
     * other functions to help out
     * @param hashFile
     * @param vehicle
     * @return
     */
    public static int vehicleUpdate(HashFile hashFile, Vehicle vehicle){
        Vehicle veh = new Vehicle();
        MutableInteger rbn = new MutableInteger(P2Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash()).intValue());
        int rba;
        int maxProbe;
        boolean recExist;
        boolean wroteRec;
        readRec(hashFile, rbn, veh);
        if (veh.getVehicleIdAsString().equals(vehicle.getVehicleIdAsString())) {
            //record exist at this position
            rba = rbn.intValue() * hashFile.getHashHeader().getRecSize();
            try{
                hashFile.getFile().seek(rba);
                char [] chars = vehicle.toFileChars();
                for(int i = 0; i < chars.length; i++) {
                    hashFile.getFile().writeChar(chars[i]);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return ReturnCodes.RC_LOC_NOT_FOUND;
            }
            return ReturnCodes.RC_OK;
        } else {
            maxProbe = hashFile.getHashHeader().getMaxProbe();
            recExist = false;
            for(int i = 1; i < maxProbe; i++){
                rbn.set( P2Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash()).intValue() + i);
                readRec(hashFile, rbn, veh);
                if (veh.getVehicleIdAsString().equals(vehicle.getVehicleIdAsString()) && !recExist) {
                    recExist = true;
                    rba = rbn.intValue() * hashFile.getHashHeader().getRecSize();
                    try {
                        hashFile.getFile().seek(rba);
                        char[] chars = vehicle.toFileChars();
                        for (int j = 0; j < chars.length; j++){
                            hashFile.getFile().writeChar(chars[j]);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return ReturnCodes.RC_LOC_NOT_FOUND;
                    }
                    return ReturnCodes.RC_OK;
                }
            }
            return ReturnCodes.RC_REC_NOT_FOUND;
        }
    }

    /**
     * If you did not do the extra credit, create a simple function that just returns RC_NOT_IMPLEMENTED.
     * This function finds the specified vehicle and deletes it by simply setting all bytes in that record to '\0'.
     * Once deleted, this may impact your vehicleRead, vehicleInsert, and vehicleUpdate since there can now
     * be empty records along a synonym list even though the needed vehicle could be after it
     * @param hashFile
     * @param vehicleId
     * @return
     */
    public static int vehicleDelete(HashFile hashFile, char [] vehicleId) {
        return ReturnCodes.RC_NOT_IMPLEMENTED;
    }
}