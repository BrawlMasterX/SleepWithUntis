package de.keule.webuntis.response;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class Schoolyears extends WebUntisDataWrapper<Schoolyear>{
	public Schoolyears(JSONObject json) throws JSONException {
		super(json, Schoolyear.class);
	}

	public List<Schoolyear> getSchoolyears() {
		return data;
	}
}
