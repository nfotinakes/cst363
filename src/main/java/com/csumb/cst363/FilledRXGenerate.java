package com.csumb.cst363;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Random;
import java.util.Date;

public class FilledRXGenerate {

    public static void main(String[] args) {

        Random gen = new Random();
        LocalDate currentdate = LocalDate.now();
        String url = "jdbc:mysql://localhost:3306/drugchain";
        String user = "root";
        String password = "1!FubarEgot2@";
        try (Connection con = DriverManager.getConnection(url, user, password);) {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

            for(int i = 0; i < 2500; i++) {


                String dateissue = "";
                String randomDate = generateRXDate();
                int randomRx = gen.nextInt(5000) + 1;





                PreparedStatement checkIssue = con.prepareStatement("select dateissued from prescription where rxnumber = ?");
                checkIssue.setInt(1, randomRx);
                ResultSet rs2 = checkIssue.executeQuery();
                if(rs2.next()) {
                    dateissue = rs2.getString(1);
                }


                Date dateIssueasdate = simpleDateFormat.parse(dateissue);
                Date datefilledasdate = simpleDateFormat.parse(randomDate);

                    if(simpleDateFormat.parse(randomDate).after(simpleDateFormat.parse(dateissue))) {
                        PreparedStatement ps = con.prepareStatement("update prescription set pharmacyid = ?, datefilled = ? where rxnumber = ?");
                        ps.setInt(1, gen.nextInt(5) + 1);
                        ps.setString(2, randomDate);
                        ps.setInt(3, randomRx);
                        ps.executeUpdate();
                    }


            }




        } catch (
                Exception e) {
            e.printStackTrace();
        }


    }

    public static String generateRXDate() {
        Random gen = new Random();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 2021);
        c.set(Calendar.DAY_OF_YEAR, 1);
        c.set(Calendar.DAY_OF_YEAR, gen.nextInt(365));
        Date dt = new Date(c.getTimeInMillis());
        String random_date = simpleDateFormat.format(dt);
        return random_date;
    }


}
