package tools.argumentTainter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import neo4j.readWriteDB.Neo4JDBInterface;
import neo4j.traversals.readWriteDB.Traversals;

import org.neo4j.graphdb.Node;

import ddg.DefUseCFG.DefUseCFG;
import ddg.DefUseCFG.DefUseCFGFactory;
import ddg.DefUseCFG.ReadWriteDbFactory;

public class FunctionPatcher
{

	private DefUseCFGFactory defUseGraphFactory = new ReadWriteDbFactory();
	private Collection<Node> statementsToPatch = new LinkedList<Node>();
	private DefUseCFG defUseCFG = null;

	private String sourceToPatch;
	private int argumentToPatch;

	public void setSourceToPatch(String source)
	{
		sourceToPatch = source;
	}

	public void setArgumentToPatch(int argToPatch)
	{
		argumentToPatch = argToPatch;
	}

	public void reset()
	{
		statementsToPatch.clear();
		defUseCFG = null;
	}
	//funcId是调用危险函数的母函数id
	public void patch(Long funcId)
	{
		//将函数调用语句放入statementsToPatch，这里的语句指的是CFG节点（isCFG=true）
		determineCallsToPatch(funcId);
		//从数据库中获取DefUseCFG（有Def和Use关系的控制流图）
		retrieveDefUseCFGFromDatabase(funcId);
		patchDefUseCFG();
		patchDDG(funcId);
	}

	private void determineCallsToPatch(Long funcId)
	{
		//获取source对应的函数调用语句（type为CallExpression）
		List<Node> callNodes = Traversals.getCallsToForFunction(sourceToPatch,
				funcId);
		for (Node callNode : callNodes)
		{
			//Traversals.getStatementForASTNode(callNode)获取的节点不仅是ASTnode而且是CFGnode，获取的是包含函数调用的那一个语句
			statementsToPatch.add(Traversals.getStatementForASTNode(callNode));
		}
	}

	private void retrieveDefUseCFGFromDatabase(long funcId)
	{
		defUseCFG = defUseGraphFactory.create(funcId);
	}
	//不知道patchDefCFG这个函数是干嘛的？
	private void patchDefUseCFG()
	{
		DefUseCFGPatcher patcher = new DefUseCFGPatcher();
		//sourceToPatch是vul函数，argumentToPatch是函数的参数个数
		patcher.setSourceToPatch(sourceToPatch, argumentToPatch);
		patcher.patchDefUseCFG(defUseCFG, statementsToPatch);
		patcher.writeChangesToDatabase();
	}

	private void patchDDG(Long funcId)
	{
		DDGPatcher patcher = new DDGPatcher();
		patcher.patchDDG(defUseCFG, funcId);
		patcher.writeChangesToDatabase();
	}
}
