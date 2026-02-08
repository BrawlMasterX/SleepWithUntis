package de.keule.webuntis.response;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class Departments extends WebUntisDataWrapper<Department> {

	public Departments(JSONObject json) throws JSONException {
		super(json, Department.class);
	}

	public Department getDpartment(int i) {
		return data.get(i);
	}

	public List<Department> getDepartments() {
		return data;
	}
}
