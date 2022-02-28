package com.csumb.cst363;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
/**
 * Author: Group12
 * Description: This class uses JDBC to connect to database and then insert
 * 10 random doctors, 1000 random patients, and 5000 random prescriptions
 * The database must already be created and drug table must be populated.
 */

public class DataGenerate {

    public static void main(String[] args) {

        // List of 50 possible first names
        String[] firstNames = {"Billy", "Susan", "Conor", "Rebecca", "James", "Michael", "Mary", "Linda", "William",
                "Richard", "Susan", "Joseph", "Jessica", "Thomas", "Sarah", "Karen", "Daniel", "Lisa", "Matthew",
                "Ashley", "Steven", "Amanda", "George", "Timothy", "Sharon", "Brittany", "Jeffrey", "Laura", "Amy",
                "Ryan", "Jacob", "Stephen", "Nicole", "Brandon", "Samantha", "Gregory", "Debra", "Frank", "Rachel",
                "Christine", "Patrick", "Heather", "Tyler", "Maria", "Dennis", "Charlie", "Aaron", "Julie", "Peter", "Kelly"};

        // List of 30 possible last names
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Rodriguez", "Lopez",
                "Taylor", "Moore", "Wilson", "Jackson", "White", "Thompson", "Lee", "Sanchez", "Robinson", "Clark", "Walker",
                "King", "Wright", "Scott", "Flores", "Adams", "Nelson", "Hall", "Carter", "Turner", "Parker"};

        // List of 5 cities with corresponding state and zip
        String[] cities = {"Seaside,CA,93955", "San Jose,CA,95050", "Los Angeles,CA,90001", "Phoenix,AZ,85001", "Las Vegas,NV,88901"};

        // List of 10 street names
        String[] streetNames = {"Main St", "South St", "Tulip Ln", "Westside Ave", "Hollyhock Rd", "8th St", "12th St", "Park St",
                "Lake Blvd", "Maple Rd"};

        // List of 10 doctor specialties
        String[] specialties = {"Internal Medicine", "Family Medicine", "Neurologist", "Orthopedics", "Dermatology",
        "Cardiology", "Gastroenterology", "Psychiatry", "Oncology", "Ophthalmology"};

        // Random object to randomize results
        Random gen = new Random();
        // List of randomly generated unique SSN's
        List<String> ssnNums = generateSSN();

        String url = "jdbc:mysql://localhost:3306/drugchain";
        String user = "root";
        String password = "1!FubarEgot2@";
        try (Connection con = DriverManager.getConnection(url, user, password); ) {

            // Insert 10 randomized doctors into database
            PreparedStatement ps = con.prepareStatement("insert into doctor(name, practice_since, specialty, ssn) values(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            for(int i = 0; i < 10; i++) {
                ps.setString(1, generateName(firstNames, lastNames));
                int randomYear = 1980 + gen.nextInt(2020-1980+1);
                ps.setString(2, Integer.toString(randomYear));
                ps.setString(3, specialties[gen.nextInt(10)]);
                ps.setString(4, ssnNums.get(i));
                ps.executeUpdate();
            }
            System.out.println("Doctors Successfully Inputted");


            // Insert 1000 randomized patients into database
            ps = con.prepareStatement("insert into patient(name, birthdate, ssn, street, city, state, zipcode, primaryid) values(?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            for(int i = 0; i < 1000; i ++) {
                ps.setString(1, generateName(firstNames, lastNames));
                ps.setString(2, generatePatientBirth());
                ps.setString(3, ssnNums.get(i + 10));
                int streetNum = 10 + gen.nextInt(999-10+1);
                String streetAdd = streetNum + " " + streetNames[gen.nextInt(10)];
                ps.setString(4, streetAdd);
                String selectCity = cities[gen.nextInt(5)];
                String[] splitCity = selectCity.split(",");
                ps.setString(5, splitCity[0]);
                ps.setString(6, splitCity[1]);
                ps.setString(7, splitCity[2]);
                ps.setInt(8, 1 + gen.nextInt(10));
                ps.executeUpdate();
            }
            System.out.println("Patients Successfully Inputted");

            // Insert 5000 randomized prescriptions into database
            ps = con.prepareStatement("insert into prescription(dateissued, quantity, doctorid, patientid, drugid) values(?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            for(int i = 0; i < 5000; i++) {
                ps.setString(1, generateRXDate());
                ps.setInt(2, 1+ gen.nextInt(45));
                ps.setInt(3, 1+ gen.nextInt(10));
                ps.setInt(4, 1+ gen.nextInt(1000));
                ps.setInt(5, 1+ gen.nextInt(99));
                ps.executeUpdate();
            }
            System.out.println("Prescriptions Successfully Inputted");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Generate a random name from first and last name arrays
    public static String generateName(String[] first, String[] last) {
        Random gen = new Random();
        return first[gen.nextInt(first.length)] + " " + last[gen.nextInt(last.length)];
    }

    // Generate a unique list of 1500 possible unique SSN's extras included in case of generated duplicates
    public static List<String> generateSSN() {
        List<String> ssnList = new ArrayList<>();
        String ssn1AsString;
        String ssn2AsString;
        String ssn3AsString;
        String ssn;
        Random gen = new Random();
        for (int i = 0; i < 1500; i++) {
            int ssn1 = 100 + gen.nextInt(899-100+1);
            int ssn2 = 1 + gen.nextInt(99-1+1);
            int ssn3 = 1 + gen.nextInt(9999-1+1);

            ssn1AsString = Integer.toString(ssn1);

            if(ssn2 < 10) {
                ssn2AsString = "0" + ssn2;
            } else {
                ssn2AsString = Integer.toString(ssn2);
            }
            if (ssn3 < 10) {
                ssn3AsString = "000" + ssn3;
            } else if(ssn3 < 100) {
                ssn3AsString = "00" + ssn3;
            } else if (ssn3 < 1000) {
                ssn3AsString = "0" + ssn3;
            } else {
                ssn3AsString = Integer.toString(ssn3);
            }
            ssn = ssn1AsString + "-" + ssn2AsString + "-" + ssn3AsString;
            if(!ssnList.contains(ssn)){
                ssnList.add(ssn);
            }
        }
        return ssnList;
    }

    // Generate a random birthdate for patient between 1946 and 2011
    public static String generatePatientBirth(){
        Random gen = new Random();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd");
        Calendar c = Calendar.getInstance();
        int year = 1946 + gen.nextInt(2011-1946+1);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.DAY_OF_YEAR, 1);
        c.add(Calendar.DAY_OF_YEAR, gen.nextInt(365));
        Date dt = new Date(c.getTimeInMillis());
        String random_date = simpleDateFormat.format(dt);
        return random_date;
    }

    // Generate a random date for prescription date issued (in 2021)
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
