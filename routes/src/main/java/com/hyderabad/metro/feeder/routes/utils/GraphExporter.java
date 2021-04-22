package com.hyderabad.metro.feeder.routes.utils;

import java.io.StringWriter;
import java.io.Writer;

import org.jgrapht.Graph;
import org.springframework.stereotype.Service;
import org.jgrapht.nio.dot.DOTExporter;

import com.hyderabad.metro.feeder.routes.beans.Edge;
import com.hyderabad.metro.feeder.routes.beans.Node;

@Service
public class GraphExporter {
	
	public void exportGraph(Graph<Node, Edge> graph) {
		
		DOTExporter<Node, Edge> exporter = new DOTExporter<>(v -> v.toString());
		Writer writer = new StringWriter();
		exporter.exportGraph(graph, writer);
		System.out.println(writer.toString());
		
	}

}


