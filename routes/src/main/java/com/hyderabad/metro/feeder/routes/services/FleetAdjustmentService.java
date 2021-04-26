package com.hyderabad.metro.feeder.routes.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyderabad.metro.feeder.routes.beans.Edge;
import com.hyderabad.metro.feeder.routes.beans.Node;
import com.hyderabad.metro.feeder.routes.utils.ExcelUtility;

@Service
public class FleetAdjustmentService {
	
	@Autowired
	private ExcelUtility excelUtility;
	
	private final static Logger LOGGER = Logger.getLogger(FleetAdjustmentService.class.getName());
	
	private final static Integer speedOfTheBus = 30; //Kmph
	
	private final static Double dwellTime = new Double((20/60)/60); //20 Seconds converted to Hours
	
	private final static Integer unitWaitingCost = 108; //Rupees per Hour
	
	private final static Integer unitVehicleCost = 760; //Rupees per Hour
	
	private final static Integer multiplicationFactor = 8; //Based on the sample size
	
	private final static Integer busCapacity = 50; //Bus Capacity
	
	private final static Integer maxBusCapacity = 60; //Max Bus Capacity
	
	private final static Integer maxFleetSize = 74; //Buses per hour
	
	//Travel Demand Matrix
	private String sheetName = "Travel Demand Matrix";
	private Integer startRow = 0;
	private Integer endRow = 141;
	private Integer startColumn = 0;
	private Integer endColumn = 141;
	
	private List<List<Object>> travelDemandMatrix;
	
	public Map<Node, Integer> compute(Map<Node, Graph<Node, Edge>> routes) {
		
		Map<Node, Integer> fleetSizes = this.fleetSizeCalculation(routes);
		
		Integer totalFleetSize = fleetSizes.values().stream().mapToInt(Integer::intValue).sum();
		
		Map<Node, Double> adjustedHeadWay = this.headWayAdjustment(fleetSizes, routes);
		
		return this.fleetSizeAdjustment(routes, fleetSizes, adjustedHeadWay);
	}
	
	public Map<Node, Integer> fleetSizeCalculation(Map<Node, Graph<Node, Edge>> routes) {
		
		Map<Node, Double> headWay = this.headWayCalculation(routes);
		
		Set<Node> originNodes = routes.keySet();
		
		Map<Node, Integer> fleetSizeValues = originNodes.stream()
				.collect(Collectors.toMap(node -> node, node -> {
			
			Graph<Node, Edge> graph = routes.get(node);
			
			Double totalDistanceOfRoute = graph.edgeSet().stream()
					.mapToDouble(f -> f.getWeight()).sum();
			
			Integer numberOfStops = graph.vertexSet().size();
			
			Double headWayValue = headWay.get(node);
			
			Double numerator = 2 * (totalDistanceOfRoute + (FleetAdjustmentService.dwellTime
					* numberOfStops * FleetAdjustmentService.speedOfTheBus));
			
			Double denominator = headWayValue * FleetAdjustmentService.speedOfTheBus;
			
			return new Double(Math.ceil(numerator/denominator)).intValue();
		}));
		
		return fleetSizeValues;		
	}
	
	public Map<Node, Double> headWayAdjustment(Map<Node, Integer> fleetSizes, Map<Node, Graph<Node, Edge>> routes) {
		
		Set<Node> nodes = fleetSizes.keySet();
		
		Map<Node, Double> adjustedHeadWay = nodes.stream()
				.collect(Collectors.toMap(node -> node, node -> {
			
			//Reduce each current fleet size value by 1
			Integer reducedFleetSize = fleetSizes.get(node) - 1;
			
			Graph<Node, Edge> route = routes.get(node);
			
			Double totalDistanceOfRoute = route.edgeSet().stream()
					.mapToDouble(f -> f.getWeight()).sum();
			
			Integer numberOfStops = route.vertexSet().size();
			
			Double numerator = 2 * (totalDistanceOfRoute + (FleetAdjustmentService.dwellTime
					* numberOfStops * FleetAdjustmentService.speedOfTheBus));
			
			Integer denominator = reducedFleetSize * FleetAdjustmentService.speedOfTheBus;
			
			//Calculate Headway by substituting reduced fleet size value in Fleet size formula
			return numerator/denominator;
		}));
		
		return adjustedHeadWay;
	}
	
	public Map<Node, Integer> fleetSizeAdjustment(Map<Node, Graph<Node, Edge>> routes, 
			Map<Node, Integer> currentFleetSizes, Map<Node, Double> adjustedHeadWay) {
		
		Set<Node> originNodes = routes.keySet();
		
		Map<Node, Integer> adjustedFleetSizes;		
		
		adjustedFleetSizes = originNodes.stream()
		.collect(Collectors.toMap(node -> node, node -> {
	
			Integer highestDemandValue = this.getTravelDemandForRoute(routes.get(node)).stream()
			.collect(Collectors.summarizingInt(Integer::intValue)).getMax();
			
			//Use adjusted headway in Headway 2 formula to find the increased bus capacity
			Double capacity = highestDemandValue * adjustedHeadWay.get(node);
			
			//Check if the capacity calculated using adjusted headway is greater than max bus capacity
			if(capacity > FleetAdjustmentService.maxBusCapacity) {
				//Set current fleet size value
				return currentFleetSizes.get(node);
			} 	
			return currentFleetSizes.get(node) - 1;	
		}));
		
		return adjustedFleetSizes;		
	}
	
	public Map<Node, Double> headWayCalculation(Map<Node, Graph<Node, Edge>> routes) {
		Map<Node, Double> headWay1 = this.headWayCalculation1(routes);
		
		Map<Node, Double> headWay2 = this.headWayCalculation2(routes);
		
		Set<Node> originNodes = headWay1.keySet();
		
		Map<Node, Double> headWay = originNodes.stream()
				.collect(Collectors.toMap(node -> node, node -> {
					return Math.min(headWay1.get(node), headWay2.get(node));
				}));
		return headWay;		
	}
	
	public Map<Node, Double> headWayCalculation1(Map<Node, Graph<Node, Edge>> routes) {
		
		this.readTravelDemandMatrix();
		
		Map<Node, Double> passengersPerRoute = this.passengersPerRoute(routes);
		
		Set<Node> originNodes = routes.keySet();
		
		Map<Node, Double> result = originNodes.stream()
		.collect(Collectors.toMap(node -> node, node -> {
			
			Graph<Node, Edge> graph = routes.get(node);
			
			Double lengthOfRoute = graph.edgeSet().stream()
					.mapToDouble(f -> f.getWeight()).sum();
			
			Double numerator = ((FleetAdjustmentService.dwellTime * 
					graph.vertexSet().size() * FleetAdjustmentService.speedOfTheBus) + lengthOfRoute);
			numerator = 2 * FleetAdjustmentService.unitVehicleCost * numerator;
			
			Double denominator = FleetAdjustmentService.unitWaitingCost 
					* FleetAdjustmentService.speedOfTheBus
					* passengersPerRoute.get(node);
			
			return Math.sqrt(numerator/denominator);
		}));
		
		return result;
	}
	
	public Double findTravelTime(Graph<Node, Edge> route) {
		
		Set<Edge> edges = route.edgeSet();
		
		//Sum of the distances in all edges in a route
		Double totalDistance = edges.stream()
		.mapToDouble(f -> f.getWeight()).sum();
		
		//Time = Distance/Speed
		Double timePerRoute = totalDistance/FleetAdjustmentService.speedOfTheBus;
		
		//e^(-time)
		return Math.exp(timePerRoute * -1);
	}
	
	public Map<Node, Double> passengersPerRoute (Map<Node, Graph<Node, Edge>> routes) {
		
		Set<Node> originNodes = routes.keySet();
		
		Map<Node, Double> travelTimePerRoute = originNodes.stream()
				.collect(Collectors.toMap(node -> node, node -> {
					return this.findTravelTime(routes.get(node));
				}));
		
		Double totalTravelTime = travelTimePerRoute.values().stream()
		.mapToDouble(f -> f.doubleValue()).sum();
		
		Map<Node, Double> passengersPerRoute = originNodes.stream()
				.collect(Collectors.toMap(node -> node, node -> {
					Integer travelDemand = this.getTravelDemandForRoute(routes.get(node))
							.stream().mapToInt(demand -> demand.intValue()).sum();
					Double travelTime = travelTimePerRoute.get(node);			
					return ((travelTime/totalTravelTime)*travelDemand);
				}));
		
		return passengersPerRoute;
	}
	
	
	
	public Map<Node, Double> headWayCalculation2(Map<Node, Graph<Node, Edge>> routes) {
		Set<Node> originNodes = routes.keySet();
		
		Map<Node, Double> headWay = originNodes.stream()
				.collect(Collectors.toMap(node -> node, node -> {
			
			Integer highestDemandValue = this.getTravelDemandForRoute(routes.get(node)).stream()
					.collect(Collectors.summarizingInt(Integer::intValue)).getMax();
			
			Double headWayValue = new Double(FleetAdjustmentService.busCapacity/highestDemandValue);
			
			return headWayValue;
			
		}));
		
		return headWay;
	}
	
	public void readTravelDemandMatrix() {
		
		this.travelDemandMatrix = excelUtility.readData(this.sheetName, this.startRow, 
				this.endRow, this.startColumn, this.endColumn);
		
		LOGGER.info("Travel Demand Matrix retrieved successfully");
	}
	
	public List<Integer> getTravelDemandForRoute(Graph<Node, Edge> route) {
		
		Set<Edge> edges = route.edgeSet();
		
		List<Integer> travelDemandForRoute = new ArrayList<>();
		
		 edges.stream().forEach(edge -> {
			List<Object> destinations = this.travelDemandMatrix.get(0);
			
			for(int destinationIndex = 1; destinationIndex < destinations.size(); destinationIndex++) {
				
				String destination = destinations.get(destinationIndex).toString();
				destination = destination.substring(0, destination.length() - 2);
				
				if(destination.equals(edge.getTarget().name)) {
					
					for(int index=1; index < this.travelDemandMatrix.size(); index++) {
						
						String source = this.travelDemandMatrix.get(index).get(0).toString();
						source = source.substring(0, source.length() - 2);
						
						if(source.equals(edge.getSource().name)) {
							
							if(this.travelDemandMatrix.get(index).get(destinationIndex) instanceof Integer) {
								travelDemandForRoute.add(((Integer) this.travelDemandMatrix
										.get(index).get(destinationIndex) + 1) * FleetAdjustmentService.multiplicationFactor);
							}
						}
					}
				}
			}			
			travelDemandForRoute.add(FleetAdjustmentService.multiplicationFactor);
		});
		
		return travelDemandForRoute;		
	}	

}
