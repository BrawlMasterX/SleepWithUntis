package de.keule.webuntis.response;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class RemarkCategories extends WebUntisDataWrapper<RemarkCategorie> {

	public RemarkCategories(JSONObject json) throws JSONException {
		super(json, RemarkCategorie.class);
	}

	public List<RemarkCategorie> getRemarkCategories() {
		return data;
	}
}
