package com.csumb.cst363;

import java.sql.*;
import java.util.Scanner;

public class ManagerReport {

    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/drugchain";
        String user = "root";
        String password = "1!FubarEgot2@";
        try (Connection con = DriverManager.getConnection(url, user, password);) {

            Scanner scan = new Scanner(System.in);
            ResultSet rs;
            PreparedStatement ps;

            String input = null;
            int pharmID = 0;
            String startDate = null;
            String endDate = null;
            String pharmacyName = null;


            while (true) {


                System.out.println("Input Pharmacy ID (or enter 'exit' to end program): ");
                input = scan.nextLine();

                if (input.equalsIgnoreCase("exit")) {
                    scan.close();
                    con.close();
                    System.exit(0);
                }
                pharmID = Integer.parseInt(input);

                System.out.println("Input Start Date in format yyyy-mm-dd: ");
                startDate = scan.nextLine();

                System.out.println("Input End Date in format yyyy-mm-dd: ");
                endDate = scan.nextLine();

                PreparedStatement getPharmName = con.prepareStatement("select name from pharmacy where pharmacyid=?");
                getPharmName.setInt(1, pharmID);
                ResultSet pharmNameResult = getPharmName.executeQuery();
                if (pharmNameResult.next()) {
                    pharmacyName = pharmNameResult.getString(1);
                }

                ps = con.prepareStatement("select drugname, sum(rxtotal) from (select drug.tradename as drugname, quantity as rxtotal from prescription " +
                        "join drug on drug.drugid = prescription.drugid where pharmacyid =? and datefilled >= ? and datefilled <= ?) as pharmtotal group by drugname order by drugname");
                ps.setInt(1, pharmID);
                ps.setString(2, startDate);
                ps.setString(3, endDate);
                rs = ps.executeQuery();

                System.out.println("\n\nPrescriptions filled by pharmacy: " + pharmacyName + ", ID: " + pharmID + "' \nbetween dates '" + startDate + "' and '" + endDate + "'.");
                System.out.println("------------------------------------------------");
                System.out.format("%22s   | %18s\n", "Drug Name", "Total Quantity");
                System.out.println("------------------------------------------------");

                while (rs.next()) {
                    System.out.format("%22s   | %18d\n", rs.getString(1), rs.getInt(2));
                }

                System.out.println("------------------------------------------------\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
