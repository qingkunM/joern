package tools.callGraph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;

import org.neo4j.graphdb.Node;
public class CallGraph implements Cloneable, Serializable{
//	public String type;
//	public int startLine;
//	public String fileName;
//	public CallGraphLink parent, child;
//	public List<String> taintArgs, paramList;
	LinkedList<Node> path = new LinkedList<Node>();
	public LinkedList<Node> getPath() {
		return path;
	}

	public void setPath(LinkedList<Node> path) {
		this.path = path;
	}
	public void addNode(Node node){
		path.add(node);
	}
	public void popNode(){
		path.removeLast();
	}
	@Override
	public Object clone() throws CloneNotSupportedException{
		return super.clone();
	}
	public Object deepClone(){
		ByteArrayOutputStream bo=new ByteArrayOutputStream();
		ObjectOutputStream oo;
		try {
			oo = new ObjectOutputStream(bo);
			oo.writeObject(this);//从流里读出来
			ByteArrayInputStream bi=new ByteArrayInputStream(bo.toByteArray());
			ObjectInputStream oi=new ObjectInputStream(bi);
			return(oi.readObject());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
