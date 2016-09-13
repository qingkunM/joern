package parsing.C.PointerOps;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import antlr.C.FunctionParser;
import ast.ASTNode;
import ast.declarations.IdentifierDeclType;
import ast.expressions.AdditiveExpression;
import ast.expressions.ArrayIndexing;
import ast.expressions.AssignmentExpr;
import ast.expressions.CallExpression;
import ast.expressions.CastExpression;
import ast.expressions.Identifier;
import ast.expressions.IncDec;
import ast.expressions.IncDecOp;
import ast.expressions.InclusiveOrExpression;
import ast.expressions.MultiplicativeExpression;
import ast.expressions.PrimaryExpression;
import ast.expressions.ShiftExpression;
import ast.expressions.UnaryExpression;
import ast.expressions.UnaryOp;
import ast.functionDef.FunctionDef;
import ast.statements.CompoundStatement;
import ast.statements.ExpressionStatement;
import ast.statements.ForInit;
import ast.statements.ForStatement;
import ast.statements.IdentifierDeclStatement;
import ast.statements.IfStatement;
import ast.statements.SwitchStatement;
import ast.statements.WhileStatement;
import databaseNodes.FileDatabaseNode;

public class CPointerOpParsing{
	boolean loopOrNot=false, arrayOrNot=false;
	ASTNode funNode = new ASTNode();
	Map<String, Statisticalizer> statMap = new HashMap<String, Statisticalizer>();
	Map<String, Integer> flagMap = new HashMap<String, Integer>();
	//数组变量和索引变量的map表
	Map<String, String> var2Array = new HashMap<String, String>();
	Alias alias = new Alias();
	int loopNum=0, conditionNum=0;
	public String assArrayPattern="^.*\\[.*\\].*=.*", assPointerPattern="^\\*.*=.*";
	public String identifierFromArgument="";
	public void parsingAST(ASTNode node){
		if(node instanceof FunctionDef){
			if(node.getChildCount()>0){
				funNode = node;
				node = node.getChild(0);
				if(node instanceof CompoundStatement)
					parsingCompoundStatement4CulOp(node);
			}else
				return;
		}
	}
	public void parsingCompoundStatement4CulOp(ASTNode csnode){
		for(int i=0; i<csnode.getChildCount(); i++){
			if(csnode.getChild(i) instanceof IfStatement){
				conditionNum++;
				parsingCompoundStatement4CulOp(csnode.getChild(i));
				//parsingCompoundStatement4CulOp(csnode.getChild(i).getChild(csnode.getChild(i).getChildCount()-1));
				conditionNum--;
			}else if(csnode.getChild(i) instanceof ForStatement || csnode.getChild(i) instanceof WhileStatement){
				loopNum++;
				parsingCompoundStatement4CulOp(csnode.getChild(i));
				//parsingCompoundStatement4CulOp(csnode.getChild(i).getChild(csnode.getChild(i).getChildCount()-1));
				loopNum--;
			}else if(csnode.getChild(i) instanceof SwitchStatement){
				conditionNum++;
				parsingCompoundStatement4CulOp(csnode.getChild(i).getChild(1));
				conditionNum--;
			}else if(csnode.getChild(i) instanceof ExpressionStatement){
				processExpressionStatement(csnode.getChild(i));	
			}else if(csnode.getChild(i) instanceof UnaryExpression){//处理++p的情况
				processUnaryExpression(csnode.getChild(i));
			}else if(csnode.getChild(i) instanceof AssignmentExpr){//处理等式（包括嵌套在forinit里的等式）
				processAssignmentExpr(csnode.getChild(i));
			}else if(csnode.getChild(i) instanceof ForInit){
				processForInit(csnode.getChild(i));
			}else if(csnode.getChild(i) instanceof IncDecOp){
				updateStatisticsThroughOpIncDecOp(csnode.getChild(i));
			}else{
				parsingCompoundStatement4CulOp(csnode.getChild(i));
			}
		}
	}
	//从forInit中获取操作统计
	public void processForInit(ASTNode forInitNode){
		for(int i=0; i<forInitNode.getChildCount(); i++){
			if(forInitNode.getChild(i) instanceof AssignmentExpr){
				processAssignmentExpr(forInitNode.getChild(i));
				continue;
			}else if(forInitNode.getChild(i) instanceof UnaryExpression){
				processUnaryExpression(forInitNode.getChild(i));
				continue;
			}else if(forInitNode.getChild(i) instanceof IncDecOp){
				String id = forInitNode.getChild(i).getChild(0).getEscapedCodeStr().trim();
				id = alias.findAlias(id, statMap);
				if(statMap.get(id)!=null){
					updateStatisticsThroughOpIncDec(id);
					//System.out.println("IncDecOp : " + forInitNode.getChild(i).getEscapedCodeStr());
				}
			}
			processForInit(forInitNode.getChild(i));
		}
	}
	//处理++i形式的语句
	public void processUnaryExpression(ASTNode unaryNode){
			if(unaryNode.getChildCount()>1){
				String id = unaryNode.getChild(1).getEscapedCodeStr().trim();
				id = alias.findAlias(id, statMap);
				if(statMap.containsKey(id)){
					if(unaryNode.getChild(0) instanceof IncDec){
						updateStatisticsThroughOpIncDec(id);
						//System.out.println("unaryNode : "+unaryNode.getEscapedCodeStr());
					}
				}
			}
	}
	public void findSink(ASTNode node){
		if(node instanceof FunctionDef){
			findPointerAndArrayFromParaList(node.getChild(2));
			if(node.getChild(0) instanceof CompoundStatement){
				node = node.getChild(0);
				findPointerAndArrayFromCompoundStatement(node);
			}
		}
	}
	//从参数中获取sink点
	public void findPointerAndArrayFromParaList(ASTNode paralistNode){
		for(int i=0; i<paralistNode.getChildCount(); i++){
			ASTNode argument = paralistNode.getChild(i);
			ASTNode type = argument.getChild(0), identifier = argument.getChild(1);
			Statisticalizer stat = new Statisticalizer();
			if(type.getEscapedCodeStr().contains("*")){
				if(paralistNode.getChild(i).getChild(1) instanceof Identifier){
					stat.setType(Statisticalizer.POINTERTYPE);
					stat.setIdentifier(identifier.getEscapedCodeStr().trim());
					statMap.put(paralistNode.getChild(i).getChild(1).getEscapedCodeStr().trim(), stat);
				}
			}else if(matchNode(type, "^*.\\[.*\\].*")){
				stat.setType(Statisticalizer.ARRAYTYPE);
				stat.setIdentifier(identifier.getEscapedCodeStr().trim());
				statMap.put(paralistNode.getChild(i).getChild(1).getEscapedCodeStr().trim(), stat);
			}
		}
	}
	//获取pointer和array sink点
	public void findPointerAndArrayFromCompoundStatement(ASTNode cpds){
		for(int i=0; i<cpds.getChildCount(); i++){
//			if(cpds.getChild(i) instanceof CompoundStatement || cpds.getChild(i) instanceof ForInit){
//				parsingCompoundStatement(cpds.getChild(i));
//			}
//			if(cpds.getChild(i) instanceof IfStatement 
//			|| cpds.getChild(i) instanceof ForStatement
//			|| cpds.getChild(i) instanceof WhileStatement){
//				parsingCompoundStatement(cpds.getChild(i));
//				//parsingCompoundStatement(cpds.getChild(i).getChild(cpds.getChild(i).getChildCount()-1));
//			}else if(cpds.getChild(i) instanceof SwitchStatement ){
//				parsingCompoundStatement(cpds.getChild(i));
//			}
			if(cpds.getChild(i) instanceof ExpressionStatement){//这里expression和ExpressionStatement一起处理
				findPointerAndArray(cpds.getChild(i));
				checkAlias(cpds.getChild(i));
			}else if(cpds.getChild(i) instanceof ForInit){
				findPointerAndArrayFromForInit(cpds.getChild(i));
				checkAlias(cpds.getChild(i));
			}else if(cpds.getChild(i) instanceof IdentifierDeclStatement){
				findPointerAndArrayFromIdentifierDeclStatement(cpds.getChild(i));
			}else{
				findPointerAndArrayFromCompoundStatement(cpds.getChild(i));
			}
		}
	}
	public void findPointerAndArrayFromIdentifierDeclStatement(ASTNode declNode){
		Statisticalizer stat = new Statisticalizer();
		String identifier="";
		for(int i=0; i<declNode.getChildCount(); i++){
			if(matchNode(declNode.getChild(i), assPointerPattern)){//identifierDeclStatement中含有“=”号的
				stat.setType(Statisticalizer.POINTERTYPE);
				if(declNode.getChild(i).getChildCount()>1 && declNode.getChild(i).getChild(1) instanceof Identifier){
					identifier = declNode.getChild(i).getChild(1).getEscapedCodeStr().trim();
				}
				stat.setIdentifier(identifier);
				statMap.put(identifier, stat);
			}else if(matchNode(declNode.getChild(i), assArrayPattern)){
				stat.setType(Statisticalizer.ARRAYTYPE);
				if(declNode.getChild(i).getChildCount()>1 && declNode.getChild(i).getChild(1) instanceof Identifier){
					identifier = declNode.getChild(i).getChild(1).getEscapedCodeStr().trim();
				}
				stat.setIdentifier(identifier);
				statMap.put(identifier, stat);
			}else if(declNode.getChild(i).getEscapedCodeStr().trim().contains("[") && declNode.getChild(i).getEscapedCodeStr().trim().contains("]")){
				//剩余两种情况都是不包含“=”
				stat.setType(Statisticalizer.ARRAYTYPE);
				if(declNode.getChild(i).getChildCount()>1 && declNode.getChild(i).getChild(1) instanceof Identifier){
					identifier = declNode.getChild(i).getChild(1).getEscapedCodeStr().trim();
				}
				stat.setIdentifier(identifier);
				statMap.put(identifier, stat);
			}else if(declNode.getChild(i).getEscapedCodeStr().trim().contains("*")){
				stat.setType(Statisticalizer.POINTERTYPE);
				if(declNode.getChild(i).getChildCount()>1 && declNode.getChild(i).getChild(1) instanceof Identifier){
					identifier = declNode.getChild(i).getChild(1).getEscapedCodeStr().trim();
				}
				stat.setIdentifier(identifier);
				statMap.put(identifier, stat);
			}
		}
	}
	//检测alias
	public void checkAlias(ASTNode node){
		if(node instanceof ExpressionStatement){
			node = node.getChild(0);
			if(node instanceof AssignmentExpr){
				addAliasFromAssignmentExpr(node);
			}
		}else if(node instanceof ForInit){
			addAliasFromForInit(node);
		}
	}
	public void addAliasFromForInit(ASTNode forInitNode){
		for(int i=0; i<forInitNode.getChildCount(); i++){
			if(forInitNode.getChild(i) instanceof AssignmentExpr){
				addAliasFromAssignmentExpr(forInitNode.getChild(i));
				continue;
			}
			addAliasFromForInit(forInitNode.getChild(i));
		}
	}
	public void addAliasFromAssignmentExpr(ASTNode assNode){
		if(assNode.getChild(0) instanceof Identifier && assNode.getChild(1) instanceof Identifier && !(assNode.getChild(1) instanceof PrimaryExpression)){
			alias.addAlias(assNode.getChild(0).getEscapedCodeStr().trim(), assNode.getChild(1).getEscapedCodeStr().trim());
		}
	}
	//从forinit中获取pointer/array identifier
	public void findPointerAndArrayFromForInit(ASTNode forInitNode){
		for(int i=0; i<forInitNode.getChildCount(); i++){
			if(forInitNode.getChild(i) instanceof AssignmentExpr){
				if(matchNode(forInitNode.getChild(i), assPointerPattern)){
					findPointerIdFromAssignmentExpr(forInitNode.getChild(i));
				}
				if(matchNode(forInitNode.getChild(i), assArrayPattern)){
					findArrayIdFormAssignmentExpr(forInitNode.getChild(i));
				}
				continue;
			}
			findPointerAndArrayFromForInit(forInitNode.getChild(i));
		}
	}
	//正则表达式匹配array
	public boolean matchNode(ASTNode node, String pstr){
		String esStr = node.getEscapedCodeStr().trim();
		Pattern p = Pattern.compile(pstr);
		Matcher match = p.matcher(esStr);
		if(match.matches())
			return true;
		else
			return false;
	}
	//正则表达式匹配pointer
//	public boolean matchPointer(ASTNode node){
//		String esStr = node.getEscapedCodeStr().trim();
//		Pattern p = Pattern.compile("^\\*.*=.*");
//		Matcher match = p.matcher(esStr);
//		if(match.matches())
//			return true;
//		else 
//			return false;
//	}
	//处理EXpressStatement的情况
	public void findPointerAndArray(ASTNode node){
		if(matchNode(node,assPointerPattern)){//denote that the ExpressionStatement fall in the two situations *a++=1 or *a=1
			//System.out.print(esStr + "\n");
			for(int j = 0; j<node.getChildCount(); j++){
				if(node.getChild(j) instanceof AssignmentExpr){
					findPointerIdFromAssignmentExpr(node.getChild(j));
				}
			}
		}
		//match arrayIndexing
		if(matchNode(node, assArrayPattern)){
			//System.out.print(esStr + "\n");
			for(int j = 0; j<node.getChildCount(); j++){
				if(node.getChild(j) instanceof AssignmentExpr){
					findArrayIdFormAssignmentExpr(node.getChild(j));
				}
			}
		}
	}
	//从AssignmentExpr中获取array identifier
	public void findArrayIdFormAssignmentExpr(ASTNode assNode){
		if(assNode.getChildCount()>0 && assNode.getChild(0) instanceof ArrayIndexing){
			assNode = assNode.getChild(0);
			Statisticalizer stat = new Statisticalizer();
			stat.setType(Statisticalizer.ARRAYTYPE);
			if(assNode.getChild(0) instanceof Identifier){//c_buf[i]或者c_buf[i*6]的情况
				stat.setIdentifier(assNode.getChild(0).getEscapedCodeStr().trim());
				statMap.put(assNode.getChild(0).getEscapedCodeStr().trim(), stat);//p[b]
				if(assNode.getChild(1) instanceof Identifier){
					stat.setIdentifier(assNode.getChild(0).getEscapedCodeStr().trim());
					var2Array.put(assNode.getChild(1).getEscapedCodeStr().trim(), assNode.getChild(0).getEscapedCodeStr().trim());
				}
			}else{//(c_buf+i)[i]的情况，这种情况在findsink阶段暂时不处理
				
			}
		}
	}
	//从AssignmentExpr中获取pointer/identifier
	public void findPointerIdFromAssignmentExpr(ASTNode assNode){
		assNode = assNode.getChild(0);
		Statisticalizer stat = new Statisticalizer();
		stat.setType(Statisticalizer.POINTERTYPE);
		if(assNode instanceof Identifier){//*a=1
			stat.setIdentifier(assNode.getEscapedCodeStr().trim());
			statMap.put(assNode.getEscapedCodeStr().trim(), stat);
		}
		if(assNode.getChildCount()>1 && assNode.getChild(1).getChildCount()>1){
			if(assNode.getChild(1).getChild(0) instanceof Identifier){//*a++=1
				stat.setIdentifier(assNode.getChild(1).getChild(0).getEscapedCodeStr().trim());
				statMap.put(assNode.getChild(1).getChild(0).getEscapedCodeStr().trim(), stat);
			}
		}
	}
	//找到函数中的p[index]里面的index identifier，如果index不是一个变量，则返回null
	/**
	 * 
	 * @param node 函数节点
	 */
	public void findArrayIndexVariable(ASTNode node){
		if(node instanceof FunctionDef){
			if(node.getChildCount()>0){
				node = node.getChild(0);
			}else
				return;
		}
		for(int i=0; i<node.getChildCount(); i++){
			if(node.getChild(i) instanceof ArrayIndexing){
				if(node.getChild(i).getChild(1).getChildCount()==0){
					//这里假设p[index]，index作为一个identifier在函数的作用范围内唯一
					//arrayIndexVar.add(node.getChild(i).getChild(1).getEscapedCodeStr().trim());
					String key = node.getChild(i).getChild(1).getEscapedCodeStr().trim();
					statMap.put(key, new Statisticalizer());
				}
				continue;
			}
			parsingAST(node.getChild(i));
		}
	}
	//处理ExpressionStatement
	public void processExpressionStatement(ASTNode node){
		String id="";
		if(node.getChildCount()>0){
			node = node.getChild(0);
			if(node instanceof IncDecOp){
				id = node.getChild(0).getEscapedCodeStr().trim();
				id = alias.findAlias(id, statMap);
				if(statMap.get(id)!=null){
					updateStatisticsThroughOpIncDec(id);
					//System.out.println("IncDecOp : " + node.getEscapedCodeStr());
				}
			}else if(node instanceof AssignmentExpr){
				processAssignmentExpr(node);
			}else if(node instanceof CallExpression){//处理strncpy(c_buf, c_buf, sizeof(c_buf));情况
				processCallExpression(node);
			}
		}
	}
	//只要任意函数参数包含pointer/array identifier都会被统计在内
	public void processCallExpression(ASTNode callNode){
		ASTNode argumentList = callNode.getChild(1);
		if((callNode.getChild(0).getEscapedCodeStr().equals("strncpy")||
				callNode.getChild(0).getEscapedCodeStr().equals("strncat")||
				callNode.getChild(0).getEscapedCodeStr().equals("memncpy")
				)){
			if(argumentList.getChildCount()>0){//将length complexity作为对identifier的操作统计到statMap中
				ASTNode arg0 = argumentList.getChild(0);
				ASTNode arg2 = argumentList.getChild(2);
				if(arg0.getChild(0) instanceof Identifier && statMap.containsKey(arg0.getChild(0).getEscapedCodeStr().trim())){
					processAssignmentExpr_Right(arg2.getChild(0), arg0.getChild(0).getEscapedCodeStr().trim());
				}
			}
		}else{
			for(int i=0; i<argumentList.getChildCount(); i++){
				String expression = argumentList.getChild(i).getChild(0).getEscapedCodeStr();
				String expressionArray[] = expression.split(" ");
				for(int j=0; j<expressionArray.length; j++){
					if(statMap.containsKey(expressionArray[j])){
						//统计对identifier的操作
						processAssignmentExpr_Right(argumentList.getChild(i).getChild(0), expressionArray[j]);
						//函数参数当中每出现一次identifier Opcall就会累加一次
						updateStatisticsThroughOpCall(expressionArray[j]);
					}
				}
			}
		}
	}
	//只处理了identifier出现在函数参数一次的情况
	public void getIdFromArgument(ASTNode argument){
		if(argument instanceof Identifier){
			if(statMap.containsKey(argument.getEscapedCodeStr().trim()))
				identifierFromArgument = argument.getEscapedCodeStr().trim();
			return ;
		}else{
			for(int i=0; i<argument.getChildCount(); i++){
				getIdFromArgument(argument.getChild(i));
			}
		}
	}
	//处理等式
	public void processAssignmentExpr(ASTNode node){
		String id="";
		if(node.getChild(0) instanceof Identifier){
			id = node.getChild(0).getEscapedCodeStr().trim();
			id = alias.findAlias(id, statMap);
			if(node.getEscapedCodeStr().trim().contains("+=") || node.getEscapedCodeStr().trim().contains("-=")){
				updateStatisticsThroughOpIncDec(id);
				processAssignmentExpr_Right(node.getChild(1), id);
				//System.out.println("OpIncDec : " + node.getEscapedCodeStr());
				return;
			}
			if(node.getEscapedCodeStr().trim().contains("*=") || node.getEscapedCodeStr().contains("/=")){
				updateStatisticsThroughOpMul(id);
				processAssignmentExpr_Right(node.getChild(1), id);
				//System.out.println("OpMul : " + node.getEscapedCodeStr());
				return;
			}
			if(statMap.get(id)!=null){//等号左边是identifier或者时alias
				processAssignmentExpr_Right(node.getChild(1), id);
			}else if(var2Array.containsKey(id) && statMap.containsKey(var2Array.get(id))){
				processAssignmentExpr_Right(node.getChild(1), var2Array.get(id));
			}
		}else if(node.getChild(0) instanceof UnaryOp){//conduct the situation of *a++=1
			if(matchNode(node.getChild(0), "^\\*.*\\(.*\\).*$")){
				String array[] = node.getChild(0).getEscapedCodeStr().trim().split(" ");
				for(int i=0; i<array.length; i++){
					if(statMap.containsKey(array[i])){
						processAssignmentExpr_Right(node.getChild(0).getChild(1), array[i]);
					}
				}
			}else if(node.getChild(0).getChild(1).getChild(0) instanceof Identifier){
				id = node.getChild(0).getChild(1).getChild(0).getEscapedCodeStr().trim();
				id = alias.findAlias(id, statMap);
				updateStatisticsThroughOpIncDec(id);
				//System.out.println("UnaryOp" + node.getChild(0).getEscapedCodeStr());
			}
		}else if(node.getChild(0) instanceof ArrayIndexing){
			parsingArrayIndex(node.getChild(0));
		}
	}
	public void updateStatisticsThroughOpIncDecOp(ASTNode incdecNode){
		if(incdecNode.getChildCount()>1 && incdecNode.getChild(0) instanceof Identifier){
			String id = incdecNode.getChild(0).getEscapedCodeStr().trim();
			if(statMap.get(id)!=null){//等号左边是identifier或者时alias
				updateStatisticsThroughOpIncDec(id);
			}else if(var2Array.containsKey(id) && statMap.containsKey(var2Array.get(id))){
				updateStatisticsThroughOpIncDec(var2Array.get(id));
			}
		}
	}
	//遇到加减法以及类似操作更新statMap
	public void updateStatisticsThroughOpIncDec(String id){
		Statisticalizer stat = new Statisticalizer();
		if(statMap.get(id)!=null){
			stat = statMap.get(id);
			if(loopNum > 0){
				stat.setOpIncDecLoop(stat.getOpIncDecLoop()+1);
			}
			if(conditionNum>0){
				stat.setOpIncDecCondition(stat.getOpIncDecCondition()+1);
			}
			if(loopNum==0 && conditionNum==0)
				stat.setOpIncDec(stat.getOpIncDec()+1);
			statMap.put(id, stat);
		}
	}
	//遇到乘法以及类似操作更新statMap
	public void updateStatisticsThroughOpMul(String id){
		Statisticalizer stat = statMap.get(id);
		if(statMap.get(id)!=null){
			if(loopNum > 0){
				stat.setOpMulLoop(stat.getOpMulLoop()+1);
			}
			if(conditionNum>0){
				stat.setOpMulCondition(stat.getOpMulCondition()+1);
			}
			if(conditionNum==0 && loopNum==0){
				stat.setOpMul(stat.getOpMul()+1);
			}
			statMap.put(id,stat);
		}
	}
	//遇到函数调用更新statMap
	public void updateStatisticsThroughOpCall(String id){
		Statisticalizer stat = statMap.get(id);
		if(statMap.get(id)!=null){
			if(conditionNum>0){
				stat.setOpCallCondition(stat.getOpCallCondition()+1);
			}
			if(loopNum > 0){
				stat.setOpCallLoop(stat.getOpCallLoop()+1);
			}
			if(conditionNum==0 && loopNum==0){
				stat.setOpCall(stat.getOpCall()+1);
			}
			statMap.put(id,stat);
		}
	}
	//遇到cast操作更新statMap
	public void updateStatisticsThroughOpCast(String id){
		Statisticalizer stat = statMap.get(id);
		if(statMap.get(id)!=null){
			if(conditionNum>0){
				stat.setOpCastCondition(stat.getOpCastCondition()+1);
			}
			if(loopNum > 0){
				stat.setOpCastLoop(stat.getOpCastLoop()+1);
			}
			if(conditionNum==0 && loopNum==0){
				stat.setOpCast(stat.getOpCast()+1);
			}
			statMap.put(id,stat);
		}
	}
	public void parsingArrayIndex(ASTNode arrayNode){
		String array[] = arrayNode.getChild(0).getEscapedCodeStr().trim().split(" ");
		for(int i=0; i<array.length; i++){
			if(statMap.containsKey(array[i])){
				if(arrayNode.getChild(0).getChildCount()>1){
					processAssignmentExpr_Right(arrayNode.getChild(0), array[i]);
				}
				if(!(arrayNode.getChild(1) instanceof Identifier)){
					String id = array[i];
					id = alias.findAlias(id, statMap);
					if(statMap.get(id)!=null){
						parsingArrayOpRecursively(arrayNode.getChild(1), id);
						//System.out.println("arrayOp : " + arrayNode.getEscapedCodeStr());
					}
				}
			}
		}
	}
	public void parsingArrayOpRecursively(ASTNode arrayOpnode, String id){
		processAssignmentExpr_Right(arrayOpnode, id);
//		for(int i=0; i<arrayOpnode.getChildCount(); i++){
//			if(arrayOpnode.getChild(i).getChildCount()>0)
//				parsingArrayOpRecursively(arrayOpnode.getChild(i), id);
//			processAssignmentExpr_Right(arrayOpnode.getChild(i),id);
//		}
	}
	//process the right part of assignment statement
	public void processAssignmentExpr_Right(ASTNode node, String id){
		//处理node
		if(node instanceof CastExpression){
			updateStatisticsThroughOpCast(id);
			//System.out.println("CastExpression : " + node.getEscapedCodeStr());
		}else if(node instanceof CallExpression){
			updateStatisticsThroughOpCall(id);
			//System.out.println("CallExpression : " + node.getEscapedCodeStr());
		}else if(node instanceof MultiplicativeExpression 
				|| node instanceof ShiftExpression
				|| node instanceof InclusiveOrExpression){
			updateStatisticsThroughOpMul(id);
			//System.out.println("MultiOP : " + node.getEscapedCodeStr());
		}else if(node instanceof AdditiveExpression){
			updateStatisticsThroughOpIncDec(id);
			//System.out.println("opIncDec : "+node.getEscapedCodeStr());
		}
		//处理node的子节点
		for(int i=0; i<node.getChildCount(); i++){
			if(node.getChild(i).getChildCount()>1){
				processAssignmentExpr_Right(node.getChild(i), id);
			}
		}
	}
	public Statisticalizer findIdentifierFromMap(Map<String, Statisticalizer> statMap){
		return null;
	}
	//cumulate the conditions and loops
	public void cumulateFlags(Map<String, Statisticalizer> statMap ,ASTNode node){
		String culFlag = "";
		Integer intLoop = 0, intCondition=0;
		if(node instanceof IfStatement){
			culFlag = "condition";
			intCondition++;
			flagMap.put(culFlag, intCondition);
		}else if(node instanceof ForStatement || node instanceof WhileStatement){
			culFlag = "loop";
			intLoop++;
			flagMap.put(culFlag, intLoop);
		}else
			culFlag = "error";
	}
	//decrease the conditions and loops
	public void decreaseFlags(Map<String, Statisticalizer> statMap ,ASTNode node){
		if(node instanceof IfStatement){
			if(flagMap.containsKey("condition")){
				Integer in = flagMap.get("condition");
				flagMap.put("condition", in--);
			}
		}else if(node instanceof ForStatement || node instanceof WhileStatement){
			if(flagMap.containsKey("loop")){
				Integer in = flagMap.get("loop");
				flagMap.put("loop", in--);
			}
		}else{
		}
	}
	public void parsingDecl(ASTNode node){
		if(node instanceof IdentifierDeclType ){
			//System.out.println(((IdentifierDeclType)node).completeType);
		}
	}
	/**
	 * 确定candidate instructions:在循环内部内部的p++或者p[]
	 * @param node
	 */
	boolean loopFlag=false;
	public void findCandidateVar(ASTNode node){
		if(node instanceof FunctionDef){
			if(node.getChild(0) instanceof CompoundStatement){
				node = node.getChild(0);
			}
		}
		for(int i=0; i<node.getChildCount(); i++){
			if(node instanceof ForStatement || node instanceof WhileStatement){
				loopFlag=true;
			}
			if(loopFlag=true)
			findCandidateVar(node.getChild(i));
		}
		//
			
	}
	public void saveCulOp(ASTNode astRoot, FileDatabaseNode curFile){
		if(statMap.size()>0){
			String funName = astRoot.getEscapedCodeStr();
			StringBuilder sb = new StringBuilder();
			Statisticalizer tmp = new Statisticalizer();
			sb.append(curFile.getPathString()+" : "+funName+"\n");
			String filePath = "/home/mqk/git/joern/test/output/test";
			for(Map.Entry<String, Statisticalizer> entry : statMap.entrySet()){
				//计算一个函数的统计数据
				tmp.setOpIncDec(tmp.getOpIncDec()+entry.getValue().getOpIncDec());
				tmp.setOpIncDecCondition(tmp.getOpIncDecCondition()+entry.getValue().getOpIncDecCondition());
				tmp.setOpIncDecLoop(tmp.getOpIncDecLoop()+entry.getValue().getOpIncDecLoop());
				tmp.setOpMul(tmp.getOpMul()+entry.getValue().getOpMul());
				tmp.setOpMulCondition(tmp.getOpMulCondition()+entry.getValue().getOpMulCondition());
				tmp.setOpMulLoop(tmp.getOpMulLoop()+entry.getValue().getOpMulLoop());
				tmp.setOpCall(tmp.getOpCall()+entry.getValue().getOpCall());
				tmp.setOpCallCondition(tmp.getOpCallCondition()+entry.getValue().getOpCallCondition());
				tmp.setOpCallLoop(tmp.getOpCallLoop()+entry.getValue().getOpCallLoop());
				tmp.setOpCast(tmp.getOpCast()+entry.getValue().getOpCast());
				tmp.setOpCastCondition(tmp.getOpCastCondition()+entry.getValue().getOpCastCondition());
				tmp.setOpCastLoop(tmp.getOpCastLoop()+entry.getValue().getOpCastLoop());
				//打印每一个identifier对应的数据
//				String str = "{"+
//						Statisticalizer.IDENTIFIER+":"+entry.getValue().getIdentifier()+","+
//						Statisticalizer.TYPE+":"+entry.getValue().type+":"+","+""+
//						Statisticalizer.OPINCDEC+":"+entry.getValue().getOpIncDec()+","+
//						Statisticalizer.OPINCDECCONDITION+":"+ entry.getValue().getOpIncDecCondition()+","+" "+
//						Statisticalizer.OPINCDECLOOP+":"+ entry.getValue().getOpIncDecLoop()+","+" "+
//						Statisticalizer.OPMUL +":"+ entry.getValue().getOpMul()+","+" "+
//						Statisticalizer.OPMULCONDITION +":"+ entry.getValue().getOpMulCondition()+","+" "+
//						Statisticalizer.OPMULLOOP +":"+ entry.getValue().getOpMulLoop()+","+" "+
//						Statisticalizer.OPCALL +":"+ entry.getValue().getOpCall()+","+" "+
//						Statisticalizer.OPCALLCONDITION +":"+ entry.getValue().getOpCallCondition()+","+" "+
//						Statisticalizer.OPCALLLOOP +":"+ entry.getValue().getOpCallLoop()+","+" "+
//						Statisticalizer.OPCAST +":"+ entry.getValue().getOpCast()+","+" "+
//						Statisticalizer.OPCASTCONDITION+":"+ entry.getValue().getOpCastCondition()+" "+
//						Statisticalizer.OPCASTLOOP+":"+ entry.getValue().getOpCastLoop()+" "+
//						"}\n";
//				sb.append(str);
			}
			if(tmp.total()>0){
				String str = "{"+
						Statisticalizer.OPINCDEC+":"+tmp.getOpIncDec()+","+
						Statisticalizer.OPINCDECCONDITION+":"+ tmp.getOpIncDecCondition()+","+" "+
						Statisticalizer.OPINCDECLOOP+":"+ tmp.getOpIncDecLoop()+","+" "+
						Statisticalizer.OPMUL +":"+ tmp.getOpMul()+","+" "+
						Statisticalizer.OPMULCONDITION +":"+ tmp.getOpMulCondition()+","+" "+
						Statisticalizer.OPMULLOOP +":"+ tmp.getOpMulLoop()+","+" "+
						Statisticalizer.OPCALL +":"+ tmp.getOpCall()+","+" "+
						Statisticalizer.OPCALLCONDITION +":"+ tmp.getOpCallCondition()+","+" "+
						Statisticalizer.OPCALLLOOP +":"+ tmp.getOpCallLoop()+","+" "+
						Statisticalizer.OPCAST +":"+ tmp.getOpCast()+","+" "+
						Statisticalizer.OPCASTCONDITION+":"+ tmp.getOpCastCondition()+" "+
						Statisticalizer.OPCASTLOOP+":"+ tmp.getOpCastLoop()+" "+
						"}\n\n";
				sb.append(str);
				try {
					FileWriter fw = new FileWriter(filePath, true);
					BufferedWriter bw = new BufferedWriter(fw);
					bw.append(sb);
					bw.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	public void parsingUnary_expression(FunctionParser.Unary_expressionContext ctx, boolean loopOrNot){
//		if(loopOrNot){
//			if(ctx.postfix_expression()!=null){
//				int tmp = ctx.postfix_expression().getChildCount();
//				//System.out.println(ctx.postfix_expression().getText());
//				if(tmp > 2){
//					if(ctx.postfix_expression().getChild(1).getText().equals("[")){
//						for(int i=0; i<tmp; i++){
//							System.out.println(ctx.postfix_expression().getChild(i).getText());
//						}
//					}
//				}
//			}
//		}
	}
}
