package com.hyderabad.metro.feeder.routes.beans;

import java.io.Serializable;

import org.jgrapht.graph.DefaultWeightedEdge;

public class Edge extends DefaultWeightedEdge implements Serializable, Comparable<Edge> {
	
	private final Node source;
	
	private final Node target;
	
	private final Double weight;
	
	public static final long serialVersionUID = 1L;
	
	public Edge(Node source, Node target, Double weight) {
		this.source = source;
		this.target = target;
		this.weight = weight;
	}
	
	public boolean equals(Object object) {
		return (object instanceof Edge) 
				&& (this.toString().equals(object.toString()));
	}
	
	public String toString() {
		return this.source.toString() + " to " + this.target.toString() 
		+ "; Distance: " + this.weight.toString() + " KMs";
	}
	
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public int compareTo(Edge o) {
		return this.weight.compareTo(o.weight);
	}

	public Node getSource() {
		return source;
	}

	public Node getTarget() {
		return target;
	}

	public double getWeight() {
		return weight.doubleValue();
	}
	
}

