package com.hyderabad.metro.feeder.routes.utils;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.jgrapht.Graph;
import org.jgrapht.nio.graphml.GraphMLExporter;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.springframework.stereotype.Service;

import com.hyderabad.metro.feeder.routes.beans.Edge;
import com.hyderabad.metro.feeder.routes.beans.Node;

@Service
public class GraphExporter {
	
	private final static Logger LOGGER = Logger.getLogger(GraphExporter.class.getName());
	
	public void exportGraph(Graph<Node, Edge> graph, String graphName) {
		
		GraphMLExporter<Node, Edge> exporter = new GraphMLExporter<>();
		
		//Provides Station name + Metro / Bus Stop as ID for each node
		exporter.setVertexIdProvider( (node) -> {
			if(node.isMetro) {
				return node.name + " Metro";
			}
			return node.name + " Bus Stop";
		});
		
		//Provides the demand at the node as an attribute.
		exporter.setVertexAttributeProvider((node) -> {
			Map<String, Attribute> result = new HashMap<>();
			Attribute demand = new DefaultAttribute<>(node.demand.toString(), AttributeType.STRING);
			result.put("Demand", demand);
			return result;
		});
		
		exporter.setExportEdgeWeights(true);
		exporter.setExportVertexLabels(true);		
		exporter.setVertexLabelAttributeName("Demand");
		
		try {			
			FileOutputStream fos = new FileOutputStream(graphName + ".graphml");			
			exporter.exportGraph(graph, fos);
			fos.close();
			LOGGER.info("Graph exported!");
		} catch (Exception e) {
			// TODO: handle exception
			LOGGER.warning("Failed to export graph!");
			LOGGER.warning(e.getMessage());
		}
		
	}
	
	public void exportRoutes(Map<Node, Graph<Node, Edge>> routes) {
		
		routes.keySet().stream()
		.forEach(node -> {
			this.exportGraph(routes.get(node), node.name);
		});
		
	}

}


