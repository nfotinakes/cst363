package com.csumb.cst363;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Patient Controller for Team 12
 * Code for registering, displaying and editing patients
 * Validates and sanitizes all input
 * Note: Some validation could have associated methods, but to preserve template, all done within
 * specified sections
 */

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatient {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 * Request patient_register form.
	 */
	@GetMapping("/patient/new")
	public String newPatient(Model model) {
		// return blank form for new patient registration
		model.addAttribute("patient", new Patient());
		return "patient_register";
	}
	
	/*
	 * Request form to search for patient.
	 */
	@GetMapping("/patient/edit")
	public String getPatientForm(Model model) {
		// prompt for patient id and name
		return "patient_get";
	}
	
	/*
	 * Process a form to create new patient.
	 */
	@PostMapping("/patient/new")
	public String newPatient(Patient p, Model model) {

		// array of illegal characters to check against XSS attacks
		char[] illegalChars = {'<', '>', '&', '"', '\'', '(', ')', '#', ';', '+'};

		try (Connection con = getConnection();) {



			// Check SSN for valid entry
			if(p.getSsn().length() != 11 || p.getSsn().charAt(3) != '-' || p.getSsn().charAt(6) != '-') {
				model.addAttribute("message", "SSN number must be in format 'xxx-xx-xxxx' (hyphens included)");
				return "patient_register";
			} else if (p.getSsn().charAt(0) == '0' || p.getSsn().charAt(0) == '9') {
				model.addAttribute("message", "SSN cannot begin with 0 or 9");
				return "patient_register";
			} else if (p.getSsn().charAt(4) == '0' && p.getSsn().charAt(5) == '0') {
				model.addAttribute("message", "SSN middle numbers cannot be '00'");
				return "patient_register";
			} else if (p.getSsn().charAt(7) == '0' && p.getSsn().charAt(8) == '0' && p.getSsn().charAt(9) == '0' && p.getSsn().charAt(10) == '0') {
				model.addAttribute("message", "SSN last four numbers cannot be '0000'");
				return "patient_register";
			}

			// Check SSN for XSS attack
			for(int i = 0; i < p.getSsn().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getSsn().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in SSN");
						return "patient_register";
					}
				}
			}


			// Validate no blank name or address fields
			if(p.getName().equals("") || p.getStreet().equals("") || p.getCity().equals("") || p.getState().equals("") || p.getZipcode().equals("")){
				model.addAttribute("message", "Name or address fields cannot be blank");
				return "patient_register";
			}

			// Validate state is entered as two characters
			if(p.getState().length() != 2) {
				model.addAttribute("message", "State must be entered as two characters");
				return "patient_register";
			}

			// Validate zipcode is entered as 5 digit or with 4 digit extension
			if(p.getZipcode().length() != 5 && p.getZipcode().length() != 10) {
				model.addAttribute("message", "Invalid Zipcode entry: must be 5 or 10 characters long");
				return "patient_register";
			}

			// Check name for xss attack illegal characters
			for(int i = 0; i < p.getName().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getName().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Name field");
						return "patient_register";
					}
				}
			}

			// Check street for xss attack illegal characters
			for(int i = 0; i < p.getStreet().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getStreet().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Street field");
						return "patient_register";
					}
				}
			}

			// Check city for xss attack illegal characters
			for(int i = 0; i < p.getCity().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getCity().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in City field");
						return "patient_register";
					}
				}
			}

			// Check state for xss attack illegal characters
			for(int i = 0; i < p.getState().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getState().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in State field");
						return "patient_register";
					}
				}
			}

			// Check zipcode for xss attack illegal characters
			for(int i = 0; i < p.getZipcode().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getZipcode().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Zipcode field");
						return "patient_register";
					}
				}
			}


			// Validate Birthdate entry
			if(p.getBirthdate().equals("")) {
				model.addAttribute("message", "Birth Date cannot be blank");
				return "patient_register";
			} else if(Integer.parseInt(p.getBirthdate().substring(0, 4)) > 2022 ) {
				model.addAttribute("message", "Birth year cannot be after 2022");
				return "patient_register";
			} else if ( Integer.parseInt(p.getBirthdate().substring(0, 4)) < 1900) {
				model.addAttribute("message", "Birth year cannot be before 1900");
				return "patient_register";
			}

			// Check for already existing SSN
			PreparedStatement ps = con.prepareStatement("select patientid from patient where ssn=?");
			ps.setString(1, p.getSsn());
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				model.addAttribute("message", "That SSN already exists in database");
				return "patient_register";
			}

			// Get the doctor's id by doctor name and set the value in patient object
			ps = con.prepareStatement("select id from doctor where name=?");
			ps.setString(1, p.getPrimaryName());
			rs = ps.executeQuery();

			// Check that doctor exists in database, if not return message
			if(rs.next()) {
				p.setPrimaryID(rs.getInt(1));
			} else {
				model.addAttribute("message", "No matching doctor, please try again");
				return "patient_register";
			}

			// Complete the insert sql statement with provided values from patient object
			ps = con.prepareStatement("insert into patient(name, birthdate, ssn,  street, city, state, zipcode, primaryid ) values(?, ?, ?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, p.getName());
			ps.setString(2, p.getBirthdate());
			ps.setString(3, p.getSsn());
			ps.setString(4, p.getStreet());
			ps.setString(5, p.getCity());
			ps.setString(6, p.getState());
			ps.setString(7, p.getZipcode());
			ps.setInt(8, p.getPrimaryID());

			// execute query and set the patient object id number from the autogenerated key
			ps.executeUpdate();
			rs = ps.getGeneratedKeys();
			if (rs.next()) p.setPatientId((String)rs.getString(1));


		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_register";
		}

		// Display success message and show registered patient
		model.addAttribute("message", "Registration successful.");
		model.addAttribute("patient", p);
		return "patient_show";

	}
	
	/*
	 * Search for patient by patient id and name.
	 */
	@PostMapping("/patient/show")
	public String getPatientForm(@RequestParam("patientId") String patientId, @RequestParam("name") String name,
			Model model) {

		Patient p = new Patient();
		p.setPatientId(patientId);
		p.setName(name);

		// array of illegal characters to check against XSS attacks
		char[] illegalChars = {'<', '>', '&', '"', '\'', '(', ')', '#', ';', '+'};

		try (Connection con = getConnection();) {
			// for DEBUG
			System.out.println("start getPatient "+p);

			if(p.getPatientId().equals("") || p.getName().equals("")) {
				model.addAttribute("message", "ID or name field cannot be blank");
				return "patient_get";
			}

			// Check Patient ID field for xss attack illegal characters
			for(int i = 0; i < p.getPatientId().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getPatientId().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Patient ID field");
						return "patient_get";
					}
				}
			}

			// Check Patient Name field for xss attack illegal characters
			for(int i = 0; i < p.getName().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getName().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Patient Name field");
						return "patient_get";
					}
				}
			}

			// Try to pull a patient from database from id number and name provided
			PreparedStatement ps = con.prepareStatement("select patientId, name, birthdate, street, city, state, zipcode, primaryid from patient where patientid=? and name=?");
			ps.setInt(1, Integer.parseInt(p.getPatientId()));
			ps.setString(2, p.getName());

			// if result is returned, set patient object with information
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				p.setPatientId(rs.getString(1));
				p.setName(rs.getString(2));
				p.setBirthdate(rs.getString(3));
				p.setStreet(rs.getString(4));
				p.setCity(rs.getString(5));
				p.setState(rs.getString(6));
				p.setZipcode(rs.getString(7));

				// primary doctor id returned, but need name
				int primaryId = rs.getInt(8);
				String primaryName = null;

				// Pull doctor's matching name from database by id and set
				PreparedStatement getprimaryName = con.prepareStatement("select name from doctor where id=?");
				getprimaryName.setInt(1,primaryId);
				ResultSet nameSet = getprimaryName.executeQuery();
				if(nameSet.next()) {
					primaryName = nameSet.getString(1);
				}
				p.setPrimaryName(primaryName);


				model.addAttribute("patient", p);
				// for DEBUG
				System.out.println("end getPatient "+p);

			} else {
				model.addAttribute("message", "Patient not found.");
				return "patient_get";
			}

		} catch (SQLException e) {
			System.out.println("SQL error in getPatient "+e.getMessage());
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_get";
		}

		// If patient ID and name match in database, display patient
		model.addAttribute("patient", p);
		return "patient_show";
	}

	/*
	 * Search for patient by patient id.
	 */
	@GetMapping("/patient/edit/{patientId}")
	public String updatePatient(@PathVariable String patientId, Model model) {

		// Create new patient object and set with id passed
		Patient p = new Patient();
		p.setPatientId(patientId);
		try (Connection con = getConnection();) {

			// Search database for patient matching ID
			PreparedStatement ps = con.prepareStatement("select name, birthdate, street, city, state, zipcode, primaryid from patient where patientid=?");
			ps.setInt(1, Integer.parseInt(patientId));

			// If result returned, set patient object with provided information from database
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				p.setName(rs.getString(1));
				p.setBirthdate(rs.getString(2));
				p.setStreet(rs.getString(3));
				p.setCity(rs.getString(4));
				p.setState(rs.getString(5));
				p.setZipcode(rs.getString(6));

				int primaryId = rs.getInt(7);
				String primaryName = null;

				// Get doctor name by primary ID
				PreparedStatement getprimaryName = con.prepareStatement("select name from doctor where id=?");
				getprimaryName.setInt(1,primaryId);
				ResultSet nameSet = getprimaryName.executeQuery();
				if(nameSet.next()) {
					primaryName = nameSet.getString(1);
				}
				p.setPrimaryName(primaryName);


				model.addAttribute("patient", p);
				return "patient_edit";
			} else {
				model.addAttribute("message", "Doctor not found.");
				model.addAttribute("patient", p);
				return "patient_get";
			}

		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_get";

		}
	}
	
	
	/*
	 * Process changes to patient address and primary doctor
	 */
	@PostMapping("/patient/edit")
	public String updatePatient(Patient p, Model model) {

		// array of illegal characters to check against XSS attacks
		char[] illegalChars = {'<', '>', '&', '"', '\'', '(', ')', '#', ';', '+'};

		try (Connection con = getConnection();) {

			int primaryId = 0;

			// Validate the doctor field still contains valid doctor
			PreparedStatement ps = con.prepareStatement("select id from doctor where name=?");
			ps.setString(1, p.getPrimaryName());
			ResultSet rs = ps.executeQuery();

			if(rs.next()) {
				primaryId = rs.getInt(1);
			} else {
				model.addAttribute("message", "Error. Doctor not found");
				model.addAttribute("patient", p);
				return "patient_edit";
			}

			// Validate no address fields or doctor name
			if(p.getStreet().equals("") || p.getCity().equals("") || p.getState().equals("") || p.getZipcode().equals("") || p.getPrimaryName().equals("")){
				model.addAttribute("message", "Name or address fields cannot be blank");
				return "patient_edit";
			}

			// Validate state is entered as two characters
			if(p.getState().length() != 2) {
				model.addAttribute("message", "State must be entered as two characters");
				return "patient_edit";
			}

			// Check name for xss attack illegal characters
			for(int i = 0; i < p.getName().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getName().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Name field");
						return "patient_edit";
					}
				}
			}

			// Check street for xss attack illegal characters
			for(int i = 0; i < p.getStreet().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getStreet().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Street field");
						return "patient_edit";
					}
				}
			}

			// Check city for xss attack illegal characters
			for(int i = 0; i < p.getCity().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getCity().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in City field");
						return "patient_edit";
					}
				}
			}

			// Check state for xss attack illegal characters
			for(int i = 0; i < p.getState().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getState().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in State field");
						return "patient_edit";
					}
				}
			}

			// Check zipcode for xss attack illegal characters
			for(int i = 0; i < p.getZipcode().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getZipcode().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Zipcode field");
						return "patient_edit";
					}
				}
			}
			
			// Validate zipcode is entered as 5 digit or with 4 digit extension
						if(p.getZipcode().length() != 5 && p.getZipcode().length() != 10) {
							model.addAttribute("message", "Invalid Zipcode entry: must be 5 or 10 characters long");
							return "patient_edit";
						}

			// Update the patient information in database and then display
			ps = con.prepareStatement("update patient set street=?, city=?, state=?, zipcode=?, primaryid=? where patientid=?");
			ps.setString(1,  p.getStreet());
			ps.setString(2, p.getCity());
			ps.setString(3,  p.getState());
			ps.setString(4, p.getZipcode());
			ps.setInt(5, primaryId);
			ps.setInt(6, Integer.parseInt(p.getPatientId()));


			int rc = ps.executeUpdate();
			if (rc==1) {
				model.addAttribute("message", "Update successful");
				model.addAttribute("patient", p);
				return "patient_show";
			}else {
				model.addAttribute("message", "Error. Update was not successful");
				model.addAttribute("patient", p);
				return "patient_edit";
			}

		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_edit";
		}

	}

	/*
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 */

	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}
