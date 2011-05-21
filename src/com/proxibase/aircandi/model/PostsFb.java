package com.proxibase.aircandi.model;

import java.util.List;

import com.google.gson.annotations.Expose;

public class PostsFb
{
	@Expose
	public List<PostFb> posts;
	@Expose
	public String type;
}
