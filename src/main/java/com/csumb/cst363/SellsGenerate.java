package com.csumb.cst363;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Random;

public class SellsGenerate {
    public static void main(String[] args) {
        Random gen = new Random();

        String url = "jdbc:mysql://localhost:3306/drugchain";
        String user = "root";
        String password = "1!FubarEgot2@";
        try (Connection con = DriverManager.getConnection(url, user, password);) {


            for(int i = 1; i<=5; i++) {
                for(int j = 1; j<=99; j++) {
                    PreparedStatement ps = con.prepareStatement("insert into sells values(?, ?, ?)");
                    ps.setInt(1, i);
                    ps.setInt(2, j);
                    ps.setInt(3, gen.nextInt(15) +1);
                    ps.executeUpdate();
                    System.out.println("Input successful");
                }
            }

        } catch (
                Exception e) {
            e.printStackTrace();
        }
    }


}
