

package main;

import hashdb.HashFile;
import hashdb.HashHeader;
import hashdb.Vehicle;
import misc.ReturnCodes;

import java.io.*;

public class StudentFunctions {
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
        //temp file to see if file exists using the given fileName
        File test = new File(fileName);
        boolean exists = test.exists();
        //tests if file exists is true
        if (exists) {
            return ReturnCodes.RC_FILE_EXISTS;
        } else {
            RandomAccessFile binaryHashFile = new RandomAccessFile(fileName, "rw");;
            int rba = 0;
            try {
                //positions the file pointer to the beginning of the file
                binaryHashFile.seek(rba);
                binaryHashFile.writeInt(hashHeader.getMaxHash());
                binaryHashFile.writeInt(hashHeader.getRecSize());
                binaryHashFile.writeInt(hashHeader.getMaxProbe());
                binaryHashFile.close();
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
        File file = new File(fileName);
        boolean exists = file.exists();
        //tests if file exists is true
        if (!exists) {
            return ReturnCodes.RC_FILE_NOT_FOUND;
        } else {
            try {
                //create a random access file using original temp file and setting the hashFile
                RandomAccessFile tempFile = new RandomAccessFile(fileName, "rw");
                //sets position of the pointer to the beginning of the file
                tempFile.seek(0);
                hashFile.setFile(tempFile);
                HashHeader header = hashFile.getHashHeader();
                //sets header values
                header.setMaxHash(tempFile.readInt());
                header.setRecSize(tempFile.readInt());
                header.setMaxProbe(tempFile.readInt());
                //sets new hashHeader in hashFile
                hashFile.setHashHeader(header);
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
        int RBN = Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash());
        //create new vehicle to be inserted
        Vehicle tempVehicle = new Vehicle();
        readRec(hashFile,RBN, tempVehicle);
        //checks if vehicle is null or if position of vehicle is empty
        if(tempVehicle == null || tempVehicle.getVehicleIdAsString().isEmpty() == true){
            writeRec(hashFile, RBN, vehicle);
        }
        //tests if vehicle is not null and if vehicle Id already exists
        else if ( tempVehicle != null && tempVehicle.getVehicleId() == vehicle.getVehicleId()){
            return ReturnCodes.RC_REC_EXISTS;
        }
        return ReturnCodes.RC_SYNONYM;
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

    public static int readRec(HashFile hashFile, int rbn, Vehicle vehicle) {
        int rba = rbn * hashFile.getHashHeader().getRecSize();
        try {
            hashFile.getFile().seek(rba);
            byte[] bytes = new byte[Vehicle.sizeOf() * 2];
            hashFile.getFile().read(bytes, 0, Vehicle.sizeOf() * 2);
            if (bytes[1] != 0)
                vehicle.fromByteArray(bytes);
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
            for (int i = 0; i < chars.length; i++);
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

    public static int vehicleRead(HashFile hashFile, int rbn, Vehicle vehicle) {
        //gets RBN value from main Hash function
        int RBN = Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash());
        Vehicle tempVehicle = vehicle;
        //reads record and stores found vehicle record into tempVehicle
        readRec(hashFile, RBN, tempVehicle);
        //checks if vehicle ID matches the vehicle ID in that position
        if(tempVehicle.getVehicleId() == vehicle.getVehicleId()){
            //assigns vehicle to temp vehicle if record was found and vehicle IDs matched
            vehicle = tempVehicle;
            return ReturnCodes.RC_OK;
        } else {
            return ReturnCodes.RC_REC_NOT_FOUND;
        }
    }
}

