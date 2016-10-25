package utils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class Utils {
	public static void print(String str){
		System.out.println(str);
	}
	public static void printNodeProperty(Node node){
		Iterable<String> propIter = node.getPropertyKeys();
		print(String.valueOf("id : "+node.getId()));
		for(String prop : propIter){
			Utils.print(prop+" : "+node.getProperty(prop).toString());
		}
	}
	public static void printAllRelatedNodes(Node node){
		Iterable<Relationship> relIter = node.getRelationships(Direction.OUTGOING);
		for(Relationship rel : relIter){
			if(rel.getEndNode()!=null){
				print(rel.getType().name().toString()+"{ END NODE begin : ");
				printNodeProperty(rel.getEndNode());
				print("END NODE end }");
			}else if(rel.getStartNode()!=null){
				print(rel.getType().name().toString()+"{ START NODE begin : ");
				printNodeProperty(rel.getEndNode());
				print("START NODE end }");
			}
		}
	}

}
