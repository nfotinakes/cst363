package com.csumb.cst363;

import java.sql.*;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


@Controller    
public class ControllerPrescription {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 * Doctor requests form to create new prescription.
	 */
	@GetMapping("/prescription/new")
	public String newPrescripton(Model model) {
		model.addAttribute("prescription", new Prescription());
		return "prescription_create";
	}
	
	/* 
	 * Patient requests form to search for prescription.
	 */
	@GetMapping("/prescription/fill")
	public String getfillForm(Model model) {
		model.addAttribute("prescription", new Prescription());
		return "prescription_fill";
	}
	
	/* 
	 * Process the new prescription form.
	 * 1.  Validate that Doctor SSN exists and matches Doctor Name.
	 * 2.  Validate that Patient SSN exists and matches Patient Name.
	 * 3.  Validate that Drug name exists.
	 * 4.  Insert new prescription.
	 * 5.  If error, return error message and the prescription form
	 * 6.  Otherwise, return the prescription with the rxid number that was generated by the database.
	 */
	@PostMapping("/prescription")
	public String newPrescription( Prescription p, Model model) {

		// Illegal characters to sanitize for XSS attacks
		char[] illegalChars = {'<', '>', '&', '"', '\'', '(', ')', '#', ';', '+'};


		try (Connection con = getConnection();) {

			//The following are variables used to hold data
			PreparedStatement ps;
			ResultSet rs;
			int DoctorId = 0, PId = 0, DId = 0;

			// Validate no blank fields
			if(p.getDoctor_ssn().equals("") || p.getDoctorName().equals("") || p.getPatient_ssn().equals("") || p.getPatientName().equals("") || p.getDrugName().equals("")) {
				model.addAttribute("message", "Cannot have blank fields");
				return "prescription_create";
			}

			// Check Doc SSN for possible XSS attack
			for(int i = 0; i < p.getDoctor_ssn().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getDoctor_ssn().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Doctor SSN");
						return "prescription_create";
					}
				}
			}

			// Check Doc Name for XSS attack
			for(int i = 0; i < p.getDoctorName().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getDoctorName().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Doctor Name");
						return "prescription_create";
					}
				}
			}

			// Check Patient SSN for XSS attack
			for(int i = 0; i < p.getPatient_ssn().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getPatient_ssn().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Patient SSN");
						return "prescription_create";
					}
				}
			}

			// Check patient name for XSS attack
			for(int i = 0; i < p.getPatientName().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getPatientName().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Patient Name");
						return "prescription_create";
					}
				}
			}

			// Check drug name for XSS attack
			for(int i = 0; i < p.getDrugName().length(); i++) {
				for(int j = 0; j < illegalChars.length; j++) {
					if(p.getDrugName().charAt(i) == illegalChars[j]) {
						model.addAttribute("message", "Illegal character used in Drug Name");
						return "prescription_create";
					}
				}
			}


			//#1 Validate that Doctor SSN exists and matches Doctor Name. Get doctor ID if successful
			ps = con.prepareStatement("SELECT id FROM doctor where ssn=? AND name=?");
			ps.setString(1, p.getDoctor_ssn());
			ps.setString(2, p.getDoctorName());
			rs = ps.executeQuery();
			if(!rs.next()) {
				model.addAttribute("message", "No doctor matching that SSN and name.");
				return "prescription_create";
			}
			DoctorId = rs.getInt(1);

			//#2 Validate that Patient SSN exists and matches Patient Name. Get patient ID if successful
			ps = con.prepareStatement("SELECT patientId FROM patient where ssn=? and name=?");
			ps.setString(1, p.getPatient_ssn());
			ps.setString(2, p.getPatientName());
			rs = ps.executeQuery();
			if(!rs.next()) {
				model.addAttribute("message", "No patient matching that SSN and name.");
				return "prescription_create";
			}
			PId = rs.getInt(1);

			//#3 Validate that Drug name exists. Get drug ID if successful
			ps = con.prepareStatement("SELECT drugid FROM drug where tradename=?");
			ps.setString(1, p.getDrugName());
			rs = ps.executeQuery();
			if(!rs.next()) {
				model.addAttribute("message", "Drug not found.");
				return "prescription_create";
			}
			DId = rs.getInt(1);

			// Validate quantity is not negative or zero
			if(p.getQuantity() <= 0) {
				model.addAttribute("message", "Quantity cannot be negative or zero");
				return "prescription_create";
			}

			// Validate quantity is not larger than 200
			if(p.getQuantity() > 200) {
				model.addAttribute("message", "Quantity cannot be larger than 200");
				return "prescription_create";
			}

			//#4 Insert new prescription.
			ps = con.prepareStatement("insert into prescription(quantity, DoctorID, dateissued, patientid, drugid) values(?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, p.getQuantity());
			ps.setInt(2, DoctorId);
			ps.setString(3, LocalDate.now().toString());
			ps.setInt(4, PId);
			ps.setInt(5, DId);
			ps.executeUpdate();
			rs = ps.getGeneratedKeys();
			if (rs.next()) p.setRxid((String)rs.getString(1));

		}
		catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}
		
		model.addAttribute("message", "Prescription created.");
		model.addAttribute("prescription", p);
		return "prescription_show";
	}
	

	
	/*
	 * Process the prescription fill request from a patient.
	 * 1.  Validate that Prescription p contains rxid, pharmacy name and pharmacy address
	 *     and uniquely identify a prescription and a pharmacy.
	 * 2.  update prescription with pharmacyid, name and address.
	 * 3.  update prescription with today's date.
	 * 4.  Display updated prescription 
	 * 5.  or if there is an error show the form with an error message.
	 */
	@PostMapping("/prescription/fill")
	public String processFillForm(Prescription p,  Model model) {

		
		try (Connection con = getConnection();) {
		 //* 1.  Validate that Prescription p contains rxid, pharmacy name and pharmacy address
		 //*     and uniquely identify a prescription and a pharmacy.

			PreparedStatement ps;
			ResultSet rs;
			String errMsg = "";
			int doctorId = 0;
			int patientId = 0;
			int drugId = 0;
			String[] address = p.getPharmacyAddress().split(",");
			String street = address[0].trim();
			String city = "";
			String state = "";
			String zipcode = "";

			if (p.getRxid().equals("")) {
				errMsg += "Rx field cannot be blank. ";
			}
			else {
				ps = con.prepareStatement("SELECT * FROM prescription where rxnumber=?");
				ps.setString(1, p.getRxid());
				rs = ps.executeQuery();
				if (!rs.next()) {
					errMsg += "Rx '" + p.getRxid() + "' cannot be found. ";
				} else {
					doctorId = rs.getInt(4);
					patientId = rs.getInt(5);
					drugId = rs.getInt(6);
					p.setQuantity(rs.getInt(3));
				}
			}

			if (p.getPharmacyName().equals("") || p.getPharmacyAddress().equals("")) {
				if (p.getPharmacyName().equals("")) {
					errMsg += "Pharmacy Name field cannot be blank. ";
				}
				if (p.getPharmacyAddress().equals("")) {
					errMsg += "Pharmacy Address field cannot be blank. ";
				}
			} else {
				ps = con.prepareStatement("SELECT * FROM pharmacy where name=? AND street=?");
				ps.setString(1, p.getPharmacyName());
				ps.setString(2, street);
				rs = ps.executeQuery();
				if (!rs.next()) {
					errMsg += "Pharmacy cannot be found. ";
				} else {
					p.setPharmacyID(rs.getString(1));
					city = rs.getString(4);
					state = rs.getString(5);
					zipcode = rs.getString(6);
					p.setPharmacyPhone(rs.getString(7));
					p.setPharmacyAddress(street + ", " + city + " " + state + ", " + zipcode);
				}
			}

			if (doctorId != 0) {
				ps = con.prepareStatement("SELECT name, ssn FROM doctor where id=?");
				ps.setInt(1, doctorId);
				rs = ps.executeQuery();
				if (rs.next()) {
					p.setDoctorName(rs.getString(1));
					p.setDoctor_ssn(rs.getString(2));
				}
			}

			if (patientId != 0) {
				ps = con.prepareStatement("SELECT name, ssn FROM patient where patientid=? AND name=?");
				ps.setInt(1, patientId);
				ps.setString(2, p.getPatientName());
				rs = ps.executeQuery();
				if (!rs.next()) {
					errMsg += "Name does not match with prescription. ";
				}else {
					p.setPatient_ssn(rs.getString(2));
				}
			}

			if (drugId != 0) {
				ps = con.prepareStatement("SELECT tradename, genericformula FROM drug where drugid=?");
				ps.setInt(1, drugId);
				rs = ps.executeQuery();
				if (rs.next()) {
					p.setDrugName(rs.getString(1) + " (" + rs.getString(2) +")");
				}
			}

			if (errMsg.equals("")) {
				ps = con.prepareStatement("SELECT price FROM sells where pharmacyid=? AND drugid=?");
				ps.setString(1, p.getPharmacyID());
				ps.setInt(2, drugId);
				rs = ps.executeQuery();
				if (!rs.next()) {
					errMsg += "This Pharmacy cannot fufill Prescription. ";
				} else {
					p.setCost(String.valueOf(rs.getDouble(1)*p.getQuantity()));
				}
			}

			if (!errMsg.equals("")) {
				model.addAttribute("message", errMsg);
				return "prescription_fill";
			}

			//* 3.  update prescription with today's date.
			LocalDate date = LocalDate.now();
			p.setDateFilled(date.toString());

			//* 2.  update prescription with pharmacyid, name and address.
			ps = con.prepareStatement("UPDATE prescription SET pharmacyid=?, datefilled=? WHERE rxnumber = ?;");
			ps.setInt(1, Integer.parseInt(p.getPharmacyID()));
			ps.setDate(2, Date.valueOf(date));
			ps.setInt(3, Integer.parseInt(p.getRxid()));
			int rc = ps.executeUpdate();
		
		 //* 4.  Display updated prescription
			if (rc == 1) {
				model.addAttribute("message", "Prescription has been filled.");
				model.addAttribute("prescription", p);
				return "prescription_show";
			}
			//* 5.  or if there is an error show the form with an error message.
			else {
				model.addAttribute("message", "Prescription did not properly execute.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
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
