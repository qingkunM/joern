package ddg.DefUseCFG;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import misc.Pair;
import neo4j.readWriteDB.Neo4JDBInterface;
import neo4j.traversals.readWriteDB.Traversals;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;

import databaseNodes.EdgeTypes;

public class ReadWriteDbFactory extends DefUseCFGFactory
{

	DefUseCFG cfg;
	List<String> parameters;
	
	@Override
	public DefUseCFG create(Long funcId)
	{
		cfg = new DefUseCFG();
		//获取的是funcId对应函数的所有CFG节点并将id放在DefUseCFG对象的statesments属性中
		getStatementsOfFunction(funcId);
		//Use和Def分别对应的是两种边，将所有语句的id和code String分别放入symbolsUsed和symboldefined
		getUsesAndDefs();
		getParentBlocks();
		getChildBlocks();
		getExitNode(funcId);
		getParameters(funcId);
		//得出一个Map<objec, object>symbolsUsed的结果，表达的意思是exitnode使用了哪些symbol
		cfg.addUsesForExitNode();
		
		return cfg;
	}

	private void getParameters(Long funcId)
	{
		String query = "type:Parameter AND functionId:" + funcId;
		IndexHits<Node> hits = Neo4JDBInterface.queryIndex(query);
		for (Node node : hits)
		{
			Collection<Object> params = cfg.getSymbolsDefinedBy(node.getId());
			for (Object o : params)
			{
				cfg.addParameter((String)o);
			}
		}
		
	}

	private void getExitNode(Long funcId)
	{
		String query = "type:CFGExitNode AND functionId:" + funcId;
		IndexHits<Node> hits = Neo4JDBInterface.queryIndex(query);
		for (Node node : hits){
			cfg.setExitNode(node.getId());
		}
		
	}
	//讲functionId是funcId的函数的所有CFG语句都加入到cfg的Statements属性中
	private void getStatementsOfFunction(Long funcId)
	{
		//获取的可能是ASTNode和CFGNode上层的FunctionDataBaseNode
		String query = "isCFGNode:True AND functionId:" + funcId;
		IndexHits<Node> hits = Neo4JDBInterface.queryIndex(query);
		for (Node node : hits)
			cfg.addStatement(node.getId());	
	}

	private void getUsesAndDefs()
	{
		for (Object obj : cfg.getStatements())
		{
			Long statementId = (Long) obj;
			//获取的是statementId节点对应的孩子节点，该孩子节点和statementId节点用一条USE边连接
			List<Pair<Long, String>> used = Traversals
					.getSymbolsUsedByStatement(statementId);
			for (Pair<Long, String> symbolIdAndCode : used)
			{
				Long symbolId = symbolIdAndCode.getL();
				String symbolCode = symbolIdAndCode.getR();
				cfg.addSymbolUsed(statementId, symbolCode);
				cfg.setSetSymbolId(symbolCode, symbolId);
			}

			List<Pair<Long, String>> defined = Traversals
					.getSymbolsDefinedByStatement(statementId);
			for (Pair<Long, String> symbolIdAndCode : defined)
			{
				Long symbolId = symbolIdAndCode.getL();
				String symbolCode = symbolIdAndCode.getR();
				cfg.addSymbolDefined(statementId, symbolCode);
				cfg.setSetSymbolId(symbolCode, symbolId);
			}
		}
	}

	private void getParentBlocks()
	{
		for (Object obj : cfg.getStatements())
		{
			Long statementId = (Long) obj;

			Node statement = Neo4JDBInterface.getNodeById(statementId);
			Iterable<Relationship> rels = statement
					.getRelationships(Direction.INCOMING);
			for (Relationship rel : rels)
			{
				if (!rel.getType().name().toString().equals(EdgeTypes.FLOWS_TO))
					continue;
				long parentId = rel.getStartNode().getId();
				cfg.addParentBlock(statementId, parentId);
			}
		}
	}

	private void getChildBlocks()
	{
		for (Object obj : cfg.getStatements())
		{
			Long statementId = (Long) obj;

			Node statement = Neo4JDBInterface.getNodeById(statementId);
			Iterable<Relationship> rels = statement
					.getRelationships(Direction.OUTGOING);
			for (Relationship rel : rels)
			{
				if (!rel.getType().name().toString().equals(EdgeTypes.FLOWS_TO))
					continue;

				long childId = rel.getEndNode().getId();
				cfg.addChildBlock(statementId, childId);
			}
		}
	}

}
