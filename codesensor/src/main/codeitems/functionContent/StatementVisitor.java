package main.codeitems.functionContent;

import main.codeitems.expressions.CallItem;

public interface StatementVisitor
{
	public void visit(CallItem statementItem);
	public void visit(IdentifierDeclStatement statementItem);
	public void visit(ExprStatementItem statementItem);
	public void visit(StatementItem statementItem);

}