package com.exlibris.items;


public class PatronInfo {
	
    private String name;
    private String id;
    private String email;
    private String address;

	public PatronInfo(String name, String id, String email, String address) {
		this.name = name;
		this.id = id;
		this.email = email;
		this.address = address;
	}

	@Override
	public String toString() {
		return "Patron: "+getName()+", "+getId()+", "+getEmail()+", "+getAddress();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

}
