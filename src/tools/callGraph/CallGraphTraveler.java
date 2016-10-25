package tools.callGraph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;

import databaseNodes.EdgeTypes;
import databaseNodes.NodeKeys;
import junit.framework.Assert;
import neo4j.readWriteDB.Neo4JDBInterface;
import neo4j.traversals.readWriteDB.Traversals;
import utils.Utils;

public class CallGraphTraveler {
	static List<CallGraph> cgList = new ArrayList<CallGraph>();
	static CallGraph cg = new CallGraph();
	public static void main(String[] args) throws CloneNotSupportedException{
		initilize();
		checkCommandLine(args);
		//获取function Node节点
		IndexHits<Node> funNodes = obtainFunctionNodes(args[0]);
		for(Node node : funNodes){
			Utils.print(node.getProperty("location").toString());
			generateFunCallGraph(node);
		}
		for(CallGraph cgtmp : cgList){
			Utils.print("cgList begin: {");
			for(int i=0; i<cgtmp.getPath().size(); i++){
				if(cgtmp.getPath().get(i).hasProperty("code")){
					Utils.print(cgtmp.getPath().get(i).getProperty("code").toString());
				}else if(cgtmp.getPath().get(i).hasProperty("name"))
					Utils.print(cgtmp.getPath().get(i).getProperty("name").toString());
			}
//			for(Node funOrInvocationNode : cgtmp.getPath()){
//				if(funOrInvocationNode.hasProperty("code")){
//					Utils.print(funOrInvocationNode.getProperty("code").toString());
//				}else if(funOrInvocationNode.hasProperty("name"))
//					Utils.print(funOrInvocationNode.getProperty("name").toString());
//			}
			Utils.print("} cgList end");
		}
		closeDatabase();
	}
	public static void generateFunCallGraph(Node node) throws CloneNotSupportedException{
		//printNodeAndRelatedNode(node);
		//将函数节点加入到call graph path中
		cg.addNode(node);
		Utils.printNodeProperty(node);
		//到达main函数终止
		if(isNodeFromMain(node)){
			CallGraph tmp_cg = (CallGraph)cg.deepClone();
			//findFunctionFromParameter(node);
			cgList.add(tmp_cg);
			for(int i=0; i<tmp_cg.getPath().size(); i++){
				if(tmp_cg.getPath().get(i).hasProperty("code")){
					Utils.print(tmp_cg.getPath().get(i).getProperty("code").toString());
				}else if(tmp_cg.getPath().get(i).hasProperty("name"))
					Utils.print(tmp_cg.getPath().get(i).getProperty("name").toString());
			}
			cg.popNode();
			cg.popNode();
			for(int i=0; i<cg.getPath().size(); i++){
				if(cg.getPath().get(i).hasProperty("code")){
					Utils.print(cg.getPath().get(i).getProperty("code").toString());
				}else if(cg.getPath().get(i).hasProperty("name"))
					Utils.print(cg.getPath().get(i).getProperty("name").toString());
			}
			return;
		}
		List<Node> parameterList = obtainParameterList(node);
		if(parameterList!=null && !parameterList.isEmpty()){
			Node param = parameterList.get(0);
			//根据第一个param搜索相应的调用节点的argument
			List<Node> firstArgList = obtainArgByICFG(param);
			//如果一个function找不到对应的invocation节点，终止，直接将这条路径舍弃
			if(firstArgList.isEmpty()){
//				CallGraph tmp_cg = new CallGraph();
//				tmp_cg = cg;
//				cgList.add(tmp_cg);
//				cg.popNode();
				return;
			}
			//从这里开始路径就会增加，如果一个函数没有调用节点那么cg中只有一个node节点。
			for(Node arg : firstArgList){
				Node invokeStatement = Traversals.getStatementForASTNode(arg);
				//将invoke节点加入到call graph path中
				cg.addNode(invokeStatement);
				//如果调用函数节点和wrap 函数参数有依赖关系，则返回的nextFunparam节点是wraped函数的参数节点
				Node nextFunParam = checkReachabilityBetweenInvokationAndFunParams(invokeStatement);
				//如果nextFunParam是null说明invocation语句和函数的输入没有关系，所以终止。这里也将cg增加到cgList中以备后续的判断，
				//也即如果最后一个节点是statement而不是function节点那么这个这个节点没有到达main函数入口。在后续的符号执行分析中，如果
				//没有与输入函数有数据依赖关系，可以被丢弃
				if(nextFunParam==null){
					CallGraph tmp_cg = (CallGraph)cg.deepClone();
					cgList.add(tmp_cg);
					for(int i=0; i<tmp_cg.getPath().size(); i++){
						if(tmp_cg.getPath().get(i).hasProperty("code")){
							Utils.print(tmp_cg.getPath().get(i).getProperty("code").toString());
						}else if(tmp_cg.getPath().get(i).hasProperty("name"))
							Utils.print(tmp_cg.getPath().get(i).getProperty("name").toString());
					}
					cg.popNode();
					for(int i=0; i<cg.getPath().size(); i++){
						if(cg.getPath().get(i).hasProperty("code")){
							Utils.print(cg.getPath().get(i).getProperty("code").toString());
						}else if(cg.getPath().get(i).hasProperty("name"))
							Utils.print(cg.getPath().get(i).getProperty("name").toString());
					}
					continue;
				}else{
					//根据parameter查找对应的Function节点
					Node tmp = findFunctionFromParam(nextFunParam);
					if(tmp!=null){
						generateFunCallGraph(tmp);
					}else{
						
					}
				}
			}
			if(cg.getPath().size()>2){
				cg.popNode();
				cg.popNode();
			}
		}
	}
	//判断节点Node节点是否为main函数节点，此node节点为Function节点
	public static boolean isNodeFromMain(Node node){
		boolean ismain=false;
		Object funName = node.getProperty("name");
		if(funName!=null && funName.toString().equals("main"))
			ismain=true;
		return ismain;
	}
	public static void printNodeAndRelatedNode(Node node){
		Utils.printNodeProperty(node);
		Utils.printAllRelatedNodes(node);
	}
	public static Node findFunctionFromWrappedNode(Node node){
		long functionId = Long.valueOf(node.getProperty(NodeKeys.FUNCTION_ID).toString());
		String query = String.format("%s:Function AND %s:%s", NodeKeys.TYPE, "id", functionId);
		IndexHits<Node> funNode = Neo4JDBInterface.queryIndex(query);
		if(funNode.size()!=1)
			return null;
		else
			return funNode.next();
	}
	public static Node findFunctionFromParam(Node node){
		Iterable<Relationship> paramRelIter = node.getRelationships(Direction.INCOMING);
		for(Relationship paramRel : paramRelIter){
			if(paramRel.getType().name().equals(EdgeTypes.IS_AST_PARENT)){
				node = paramRel.getStartNode();
				break;
			}
		}
		Iterable<Relationship> paramListRelIter = node.getRelationships(Direction.INCOMING);
		for(Relationship paramListRel : paramListRelIter){
			if(paramListRel.getType().name().equals(EdgeTypes.IS_AST_PARENT)){
				node = paramListRel.getStartNode();
				break;
			}
		}
		Iterable<Relationship> funDefRelIter = node.getRelationships(Direction.INCOMING);
		for(Relationship funDeftRel : funDefRelIter){
			if(funDeftRel.getType().name().equals(EdgeTypes.IS_FUNCTION_OF_AST)){
				node = funDeftRel.getStartNode();
				if(node.getProperty("type").toString().equals("Function")){
					return node;
				}
				break;
			}
		}
		return null;
	}
	//检查sink函数链中的函数调用节点是否和函数的参数具有依赖关系，返回第一个命中的Parameter节点
	public static Node checkReachabilityBetweenInvokationAndFunParams(Node invokeStatement){
		List<Node> taintNodeList = new ArrayList<Node>();
		taintNodeList.add(invokeStatement);
		return recursivelyFindTaintNode(taintNodeList);
		//每条语句的最上层的CFG节点都包含location参数
		//Utils.print(invokeStatement.getProperty("code").toString());
	}
	public static Node recursivelyFindTaintNode(List<Node> taintNodeList){
		if(taintNodeList.isEmpty())
			return null;
		List<Node> preNodeList = new ArrayList<Node>();
		for(Node interNode : taintNodeList){
			Iterable<Relationship> relIter = interNode.getRelationships(Direction.INCOMING);
			for(Relationship rel : relIter){
				if(rel.getType().name().equals(EdgeTypes.REACHES)){
					if(rel.getStartNode().getProperty(NodeKeys.TYPE)!=null){
						if(rel.getStartNode().getProperty(NodeKeys.TYPE).toString().equals("Parameter"))
							return rel.getStartNode();
						else{
							Utils.print(rel.getStartNode().getProperty("code").toString());
							preNodeList.add(rel.getStartNode());
						}
					}
				}
			}
		}
		return recursivelyFindTaintNode(preNodeList);
	}
	public static Node findFunctionFromNode(Node node){
		if(node.hasProperty(NodeKeys.FUNCTION_ID)){
			String query = "type:Parameter AND functionId:" + node.getProperty(NodeKeys.FUNCTION_ID);
			IndexHits<Node> hits = Neo4JDBInterface.queryIndex(query);
		}
		return null;
	}
	public static List<Node> obtainArgByICFG(Node param){
		List<Node> firstArgList = new ArrayList<Node>();
		Iterable<Relationship> rels = param.getRelationships();
		for(Relationship rel : rels){
			if(rel.getType().name().equals(EdgeTypes.IS_ARG)){
				firstArgList.add(rel.getStartNode());
			}
		}
		return firstArgList;
	}
	public static List<Node> obtainParameterList(Node funNode){
		List<Node> parameterList = new LinkedList<Node>();
		long nodeId = funNode.getId();
		//说明函数其他节点的functionId属性是Function节点的id
		String tmpQuery = String.format("type:ParameterList AND functionId:%s", String.valueOf(nodeId));
		IndexHits<Node> hits = Neo4JDBInterface.queryIndex(tmpQuery);
		if(hits==null)
			return null;
		if (hits.size() != 1)
			throw (new RuntimeException(
					"Warning: Parameterlist not found or more than one."));
		Node parameterListNode = hits.next();
		//Utils.print(parameterListNode.getProperty("code").toString());
		Iterable<Relationship> parameterRelIter = parameterListNode.getRelationships();
		for(Relationship parameterRel : parameterRelIter){
			//Utils.print(parameterRel.getType().name());
			if(parameterRel.getEndNode()==parameterListNode)
				continue;
			parameterList.add(parameterRel.getEndNode());
		}
		return getIdentifierFromParams(parameterList);
	}
	public static List<Node> getIdentifierFromParams(List<Node> paramList){
		List<Node> identifierNodeList = new ArrayList<Node>();
		for(Node param : paramList){
			Iterable<Relationship> relIter = param.getRelationships();
			for(Relationship rel : relIter){
				if(rel.getEndNode() == param)
					continue;
				Node identifierNode = rel.getEndNode();
				if(identifierNode.getProperty("type").equals("Identifier")){
					identifierNodeList.add(identifierNode);
				}
			}
		}
		return identifierNodeList;
	}
	public static IndexHits<Node> obtainFunctionNodes(String str){
		String query = String.format("type:%s AND name:%s","Function",str);
		IndexHits<Node> funNodes = Neo4JDBInterface.queryIndex(query);
		return funNodes;
	}
	public static void checkCommandLine(String[] args){
		
	}
	public static void initilize(){
		Neo4JDBInterface.setDatabaseDir(".joernIndex");
		Neo4JDBInterface.openDatabase();
	}
	public static void closeDatabase(){
		Neo4JDBInterface.closeDatabase();
	}
}
