package com.hyderabad.metro.feeder.routes.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.springframework.stereotype.Service;

import com.hyderabad.metro.feeder.routes.beans.Edge;
import com.hyderabad.metro.feeder.routes.beans.Node;

@Service
public class RouteGenerationService {
	
	private final static Logger LOGGER = Logger.getLogger(RouteGenerationService.class.getName());
	
	public final static Double maxDistance = 25.0;
	
	public final static Double minDistance = 1.0;
	
	public Map<Node, Graph<Node, Edge>> routeGenerationAlgorithm(
			DirectedWeightedMultigraph<Node, Edge> graph) {
		
		Map<Node, Graph<Node, Edge>> generatedRoutes = new LinkedHashMap<>();
		
		Set<Node> allStops = graph.vertexSet();
		
		//Filtering out all Bus Stop Nodes
		Set<Node> originMetros =  allStops.stream()
			.filter((node) -> { 
				if(node.isMetro.equals(new Boolean(true))) {
					if(node.demand.compareTo(7) >= 0) {
						return true;
					} 
				} //Filtering metros whose demand is greater than or equal to 7
					return false;
			})
			.collect(Collectors.toSet());	
		
		for(Node node: originMetros) {
						
			DirectedWeightedMultigraph<Node, Edge> routeGraph = 
					new DirectedWeightedMultigraph<Node, Edge>(Edge.class);
			Double routeDistance = 0.0;
			routeGraph.addVertex(node);
			
			//Shallow copy of nodes and edges but graph instances are different.
			DirectedWeightedMultigraph<Node, Edge> clonedGraph = 
					(DirectedWeightedMultigraph<Node, Edge>) graph.clone();
			
			routeGraph = this.routeStitching(clonedGraph, node, originMetros, routeGraph, routeDistance);
			
			generatedRoutes.put(node, routeGraph);
			
		}
		LOGGER.info("Routes generated!");
		return generatedRoutes;
		
	}
	
	public Edge findTheNextStop(Set<Edge> outGoingEdges, Set<Node> originMetros, Double routeDistance) {
		
		Edge nextStop = null;
		ArrayList<Edge> edges = new ArrayList<>(outGoingEdges);
		Collections.sort(edges);
		
		List<Edge> potentialEdges = edges.stream()
				//Filtering out stops less than mininum distance and stops, adding whose distance will exceed route limit
				.filter((edge) -> 
				edge.getWeight() > RouteGenerationService.minDistance &&
				(edge.getWeight() + routeDistance) <= RouteGenerationService.maxDistance)
				//Filtering out edges whose target is a Bus Stop with 0 demand and metros which are a part of origin metros
				.filter((edge) -> {
					if(edge.getTarget().isMetro.equals(new Boolean(false))) {
						if(edge.getTarget().demand.equals(new Integer(0))) {
							return false;
						}
					} else if(originMetros.contains(edge.getTarget())) {
						return false;
					}
					return true;
				})
				.collect(Collectors.toList());
		
		if(potentialEdges.size() > 0) {
			nextStop = potentialEdges.get(0);
		}
		
		return nextStop;
	}
	
	public DirectedWeightedMultigraph<Node, Edge> routeStitching(
			DirectedWeightedMultigraph<Node, Edge> completeGraph, Node currentStop, Set<Node> originMetros,
			DirectedWeightedMultigraph<Node, Edge> routeGraph, Double routeDistance) {
		
		Set<Edge> outGoingEdges = completeGraph.outgoingEdgesOf(currentStop);
		Edge nextConnection = this.findTheNextStop(outGoingEdges, originMetros, routeDistance);
		
		if(nextConnection != null) {
			Node nextStop = nextConnection.getTarget();
			routeGraph.addVertex(nextStop);
			routeGraph.addEdge(currentStop, nextStop, nextConnection);
			routeGraph.setEdgeWeight(nextConnection, nextConnection.getWeight());
			routeDistance = routeDistance + nextConnection.getWeight();
			completeGraph.removeVertex(currentStop);
			//Recursion
			this.routeStitching(completeGraph, nextStop, originMetros, routeGraph, routeDistance);
		}
		
		return routeGraph;
	}

}


