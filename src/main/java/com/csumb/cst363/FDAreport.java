package com.csumb.cst363;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.Scanner;

public class FDAreport {

    public static void main(String[] args) throws IOException {

        Properties properties = readProperties("src/main/resources/application.properties");

        try (Connection con = DriverManager.getConnection(  properties.getProperty("spring.datasource.url"),
                                                            properties.getProperty("spring.datasource.username"),
                                                            properties.getProperty("spring.datasource.password")); ) {

            Scanner scan = new Scanner(System.in);
            PreparedStatement ps;
            ResultSet rs;

            String drugName = "";
            LocalDate startDate = null;
            LocalDate endDate = null;


            while (true) {

                String errMsg = "";

                System.out.println("\nInput Drug name (enter 'exit' to end program):  ");
                drugName = scan.nextLine();

                if (drugName.equalsIgnoreCase("exit")) {
                    scan.close();
                    con.close();
                    System.exit(0);
                }

                System.out.println("\nInput Start Date in the following format (yyyy-mm-dd):  ");
                try {
                    startDate = LocalDate.parse(scan.nextLine());
                }
                catch (DateTimeParseException e) {
                    errMsg = "Start Date was not properly formatted!\n\n\n";
                    System.out.println(errMsg);
                }
                if (errMsg.equals("")) {
                    if (startDate.compareTo(LocalDate.now()) > 0) {
                        errMsg = "Start Date is set after the current date!\n\n\n";
                        System.out.println(errMsg);
                    }
                }

                if(errMsg.equals("")) {
                    System.out.println("\nInput End Date in the following format (yyyy-mm-dd):  ");
                    try {
                        endDate = LocalDate.parse(scan.nextLine());
                    }
                    catch (DateTimeParseException e) {
                        errMsg = "End Date was not properly formatted!\n\n\n";
                        System.out.println(errMsg);
                    }
                    if (errMsg.equals("")) {
                        if (startDate.compareTo(endDate) > 0) {
                            errMsg = "End Date is set before Start Date!\n\n\n";
                            System.out.println(errMsg);
                        }
                    }
                }

                if (errMsg.equals("")) {

                    ps = con.prepareStatement("SELECT doctor.name, SUM(quantity) AS totalQuantity, COUNT(quantity) AS totalPrescriptions FROM doctor, prescription, drug WHERE tradename LIKE ? AND dateissued >= ? AND dateissued <= ? AND drug.drugid = prescription.drugid AND prescription.doctorid = doctor.id GROUP BY doctor.name ORDER BY doctor.name;");
                    ps.setString(1, drugName + '%');
                    ps.setDate(2, Date.valueOf(startDate));
                    ps.setDate(3, Date.valueOf(endDate));
                    rs = ps.executeQuery();

                    System.out.println("\n\n\n\nPrescription data by doctor for Drugs with name matching '" + drugName + "' \nbetween dates '" + startDate +"' and '" + endDate +"'.");
                    System.out.println("-------------------------------------------------------------------------");
                    System.out.format("%18s   | %18s   | %20s\n", "Doctor Name", "Total Quantity", "Total Prescriptions");
                    System.out.println("-------------------------------------------------------------------------");

                    while (rs.next()) {
                        System.out.format("%18s   | %18d   | %20d\n", rs.getString(1), rs.getInt(2), rs.getInt(3));
                    }

                    System.out.println("-------------------------------------------------------------------------\n\n");

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static Properties readProperties (String fileName) throws IOException {

        FileInputStream fis = null;
        Properties properties = null;

        try {
            fis = new FileInputStream(fileName);
            properties = new Properties();
            properties.load(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fis.close();
        }

        return properties;
    }

}
