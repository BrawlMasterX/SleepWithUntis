package de.keule.webuntis.response;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class RemarkGroups extends WebUntisDataWrapper<RemarkGroup>{

	public RemarkGroups(JSONObject json) throws JSONException {
		super(json, RemarkGroup.class);
	}

	public List<RemarkGroup> getRemarkGroups() {
		return data;
	}
}
