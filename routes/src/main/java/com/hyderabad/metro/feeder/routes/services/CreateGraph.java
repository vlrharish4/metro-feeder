package com.hyderabad.metro.feeder.routes.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.hyderabad.metro.feeder.routes.beans.Edge;
import com.hyderabad.metro.feeder.routes.beans.Node;
import com.hyderabad.metro.feeder.routes.utils.ExcelUtility;
import com.hyderabad.metro.feeder.routes.utils.GraphExporter;

@Service
public class CreateGraph implements CommandLineRunner{
	
	@Autowired
	private ExcelUtility excelUtility;
	
	@Autowired RouteGenerationService routeGenerationService;
	
	@Autowired GraphExporter graphExporter;
	
	private final static Logger LOGGER = Logger.getLogger(CreateGraph.class.getName());
	
	//OD Matrix Values
	private String ODSheetName = "Matrix";
	private Integer startRow = 0;
	private Integer endRow = 140;
	private Integer startColumn = 0;
	private Integer endColumn = 140;
	
	//Demand Values
	private String demandSheetName = "Demand at each node";
	private Integer demandStartRow = 0;
	private Integer demandEndRow = 140;
	private Integer demandStartColumn = 0;
	private Integer demandEndColumn = 1;
	
	private List<List<Object>> ODMatrix;
	
	private List<List<Object>> demandData;
	
	
	public List<List<Object>> fetchDataFromMatrix() {
		
		List<List<Object>> matrixData = excelUtility.readData(this.ODSheetName, 
				this.startRow, this.endRow, this.startColumn, this.endColumn);
		
		
		LOGGER.info(" OD Matrix retrieved successfully");
		
		return matrixData;
	}
	
	public List<List<Object>> fetchDemandData() {
		
		List<List<Object>> demandData = excelUtility.readData(this.demandSheetName, 
				this.demandStartRow, this.demandEndRow, this.demandStartColumn, this.demandEndColumn);
		
		LOGGER.info("Demand Data retrieved successfully");
		
		return demandData;
	}
	
	public List<Node> createNodes() {
		
		List<Node> nodes = new ArrayList<Node>();
		
		//Using a set to avoid duplicates while collecting station names
		Set<String> stationNames = new HashSet<String>();
		
		//Get the first list and extract all names from it
		for(Object name: this.ODMatrix.get(0)) {
			stationNames.add(name.toString());
		}
		
		//Get the first element from the rest of the lists in matrix data
		for(int index = 1; index < this.ODMatrix.size(); index++) {
			stationNames.add(this.ODMatrix.get(index).get(0).toString());
		}
		
		//Create a new Node for each name in the set and push it to nodes list
		for(String name: stationNames) {
			Node stationNode = null;
			//Check if the string is empty and skip the step
			if(name.isEmpty()) {
				continue;
			} else {
				stationNode = this.createNode(name);
			}
			nodes.add(stationNode);
		}
		
		LOGGER.info("Station names extracted");
		return nodes;		
	}
	
	public DirectedWeightedMultigraph<Node, Edge> createGraph() {
		
		DirectedWeightedMultigraph<Node, Edge> graph = 
				new DirectedWeightedMultigraph<Node, Edge>(Edge.class);
		
		List<Object> columnHeaders = this.ODMatrix.get(0);
		for(int rowIndex = 1; rowIndex<this.ODMatrix.size(); rowIndex++) {
			
			String rowHeader = this.ODMatrix.get(rowIndex).get(0).toString();
			Node rowNode = this.createNode(rowHeader);
			graph.addVertex(rowNode);
			
			List<Object> row =  this.ODMatrix.get(rowIndex);
			
			for(int columnIndex = 1; columnIndex < row.size(); columnIndex++) {
				
				String columnHeader = columnHeaders.get(columnIndex).toString();
				Node columnNode = this.createNode(columnHeader);
				graph.addVertex(columnNode);
				
				Number weight = (Number) row.get(columnIndex);
				
				if(columnNode.equals(rowNode)) {
					continue;
				} else {
					//LOGGER.info(columnNode.toString() + " to " + rowNode.toString());
					Edge edge = new Edge(columnNode, rowNode, weight.doubleValue()); 
					graph.addEdge(columnNode, rowNode, edge);
					graph.setEdgeWeight(edge, weight.doubleValue());
				}				
			}
			
		}
		
		LOGGER.info("Graph created!");
		return graph;
	}
	
	public Node createNode(String name) {
		Node stationNode = null;
		
		Integer demandAtNode = null;
		
		List<Object> demandList = this.demandData.stream()
		.filter(row -> row.contains(name))
		.findAny()
		.orElse(null);
		
		if(demandList != null) {
			demandAtNode = (Integer) demandList.get(1);
		} else {
			demandAtNode = 0;
		}
		
		if(name.endsWith(" b")) {   //Check if the name ends with " b"
			stationNode = new Node(name.substring(0, name.length() - 2), new Boolean(false), demandAtNode);
		} else {
			stationNode = new Node(name.substring(0, name.length() - 2), new Boolean(true), demandAtNode);
		}
		return stationNode;
	}


	@Override
	public void run(String... args) throws Exception {
		
		this.ODMatrix = this.fetchDataFromMatrix();
		this.demandData = this.fetchDemandData();
		//Filtering out all Bus Stop vertices
		List<Node> metroNodes = this.createNodes()
				.stream()
				.filter((node) -> { 
					if(node.isMetro.equals(new Boolean(true))) {
						if(node.demand.compareTo(3) >= 0) {
							return true;
						} 
					} //Filtering metros whose demand is greater than or equal to 3
					return false;
				})
				.collect(Collectors.toList());		
		DirectedWeightedMultigraph<Node, Edge> graph = this.createGraph();
		this.routeGenerationService.routeGenerationAlgorithm(graph, metroNodes);
		//graphExporter.exportGraph(graph);
	}

}


