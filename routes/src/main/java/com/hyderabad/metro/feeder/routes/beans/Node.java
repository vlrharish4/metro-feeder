package com.hyderabad.metro.feeder.routes.beans;

import java.io.Serializable;

public class Node implements Serializable {
	
	public final String name;
	
	public final Boolean isMetro;
	
	public final Integer demand;
	
	public static final long serialVersionUID = 1L;
	
	public Node(String name, Boolean isMetro, Integer demand) {
		this.name = name;
		this.isMetro = isMetro;
		this.demand = demand;
	}
	
	public boolean equals(Object object) {
		return (object instanceof Node) && this.toString().equals(object.toString());
	}
	
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	public String toString() {
		if(this.isMetro) {
			return this.name + " Metro; Demand is " + this.demand.toString();
		} else {
			return this.name + " Bus Stop; Demand is " + this.demand.toString();
		}
	}

}


