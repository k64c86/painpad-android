package com.packratapp.painpadandroid;

import java.io.File;
import java.io.FileOutputStream;


class PainScoreFile {

    private String dirPath;
    private String fileName;


    /**
     * Construct for PainScoreFile
     * @param dirPath the directory to store the file in
     * @param fileName the name of the file
     */
    PainScoreFile(String dirPath, String fileName) {
        this.dirPath = dirPath;
        this.fileName = fileName;
    }

    /**
     * Save a patient score input to the scores file.
     * @param patientID patient id
     * @param interfaceID interface id
     * @param score the pain score
     * @param dateTime the datetime the score was entered.
     */
    void writeScore(String patientID, String interfaceID, String score, String dateTime) {
        File painScoreFile = new File(this.dirPath, this.fileName);

        try {
            String comma = ",";
            String line = patientID + comma + interfaceID + comma + score + comma + dateTime + "\n";

            FileOutputStream outputStream = new FileOutputStream(painScoreFile, true);
            outputStream.write(line.getBytes());
            outputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
